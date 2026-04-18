package com.turbofan3360.openeq.audioprocessing

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import android.media.AudioAttributes
import android.content.Context
import android.media.AudioDeviceInfo

@SuppressLint("MissingPermission")
class AudioEngine(private val context: Context) {
    @Volatile private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    val dsp = FastConvEQ()

    val spectrumFlow = MutableStateFlow(FloatArray(FastConvEQ.N_FFT / 2))
    val processingTimeFlow = MutableStateFlow(0.0)

    fun start() {
        if (isRecording) return

        val minBufBytes = AudioRecord.getMinBufferSize(
            FastConvEQ.FS,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // getMinBufferSize devuelve ERROR (-1) o ERROR_BAD_VALUE (-2) si falla
        if (minBufBytes <= 0) return

        // El buffer debe ser al menos del tamaño del bloque de procesamiento (N_X muestras * 2 bytes)
        val bufferSize = maxOf(minBufBytes * 2, FastConvEQ.N_X * 2)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            FastConvEQ.FS,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        // NUEVO: Forzar el uso del micrófono integrado del dispositivo
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)

        val builtInMic = inputDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC
        }

        if (builtInMic != null) {
            audioRecord?.setPreferredDevice(builtInMic)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(FastConvEQ.FS)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        // Verificar que los objetos se inicializaron correctamente
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED ||
            audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
            audioRecord?.release(); audioRecord = null
            audioTrack?.release(); audioTrack = null
            return
        }

        audioRecord?.startRecording()
        audioTrack?.play()
        isRecording = true

        Thread {
            val inBuffer = ShortArray(FastConvEQ.N_X)
            val outBuffer = ShortArray(FastConvEQ.N_X)

            var timeAccumulator = 0.0
            var blockCounter = 0
            val BLOCKS_PER_UPDATE = 20 // Actualiza la interfaz aprox. 4 veces por segundo

            while (isRecording) {
                audioRecord?.read(inBuffer, 0, FastConvEQ.N_X)

                val t0 = System.nanoTime()
                // Aplicar el Ecualizador por Convolución
                dsp.processBlock(inBuffer, outBuffer)
                val t1 = System.nanoTime()

                audioTrack?.write(outBuffer, 0, FastConvEQ.N_X)

                spectrumFlow.value = dsp.currentSpectrum.clone()

                // Sumar el tiempo de procesamiento en milisegundos
                timeAccumulator += (t1 - t0) / 1_000_000.0
                blockCounter++

                // Actualizar el valor promedio solo cada BLOCKS_PER_UPDATE iteraciones
                if (blockCounter >= BLOCKS_PER_UPDATE) {
                    processingTimeFlow.value = timeAccumulator / BLOCKS_PER_UPDATE
                    timeAccumulator = 0.0
                    blockCounter = 0
                }
            }
        }.start()
    }

    fun updateEq(levels: List<Float>) {
        dsp.updateCoefficients(levels.toFloatArray())
    }

    fun stop() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioTrack?.stop()
        audioTrack?.release()
    }
}