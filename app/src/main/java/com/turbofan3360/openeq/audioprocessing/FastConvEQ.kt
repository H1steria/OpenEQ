package com.turbofan3360.openeq.audioprocessing

import kotlin.math.*

class FastConvEQ {
    companion object {
        const val FS = 44100
        const val N_FFT = 1024
        const val N_H = 513
        const val N_X = N_FFT - N_H + 1 // 512

        val BAND_FREQS = floatArrayOf(0f, 31.5f, 63f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f, (FS / 2).toFloat())
        val BAND_LABELS = listOf("31.5", "63", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

        // Generación de la LUT (Look-Up Table)
        // Se ejecuta una sola vez al cargar la clase.
        val sinwn = FloatArray(1536) { n ->
            sin(PI * n / N_FFT).toFloat()
        }
    }

    private val revTable = IntArray(N_FFT)
    private val H_eq_re = FloatArray(N_FFT)
    private val H_eq_im = FloatArray(N_FFT)
    private val olaOut = FloatArray(N_FFT)
    private val X_cplx = FloatArray(N_FFT * 2)

    val currentSpectrum = FloatArray(N_FFT / 2)

    init {
        // Pre-calcular bit-reversal table
        val bits = log2(N_FFT.toDouble()).toInt()
        for (i in 0 until N_FFT) {
            var r = 0
            for (b in 0 until bits) {
                r = r or (((i shr b) and 1) shl (bits - 1 - b))
            }
            revTable[i] = r
        }
        updateCoefficients(FloatArray(BAND_LABELS.size) { 0f })
    }

    fun updateCoefficients(gainsDb: FloatArray) {
        val f_bins = FloatArray(N_FFT / 2 + 1) { it * (FS.toFloat() / N_FFT) }
        val mag_lin = FloatArray(f_bins.size)

        // Extender ganancias e interpolación lineal
        val gainsExt = FloatArray(BAND_FREQS.size)
        gainsExt[0] = gainsDb[0]
        for (i in gainsDb.indices) gainsExt[i + 1] = gainsDb[i]
        gainsExt[gainsExt.size - 1] = gainsDb.last()

        // Ganancia base
        val BASE_GAIN_DB = 5f // Agrega volumen por defecto

        for (i in f_bins.indices) {
            val f = f_bins[i]
            var idx = 0
            while (idx < BAND_FREQS.size - 2 && f > BAND_FREQS[idx + 1]) idx++

            val f1 = BAND_FREQS[idx]
            val f2 = BAND_FREQS[idx + 1]
            val g1 = gainsExt[idx]
            val g2 = gainsExt[idx + 1]

            // Interpolar en dB y añadir ganancia base
            var gDb = g1 + (g2 - g1) * ((f - f1) / (f2 - f1))
            gDb += BASE_GAIN_DB

            // Ampliamos el rango del coerce para permitir la suma de la ganancia extra
            gDb = gDb.coerceIn(-30f, 40f)
            mag_lin[i] = 10.0.pow(gDb / 20.0).toFloat()
        }

        val alpha = (N_H - 1) / 2.0f
        val hFullRe = FloatArray(N_FFT)
        val hFullIm = FloatArray(N_FFT)

        // Fase lineal y simetría
        for (i in 0..N_FFT / 2) {
            val fase = -2.0 * PI * f_bins[i] / FS * alpha
            hFullRe[i] = (mag_lin[i] * cos(fase)).toFloat()
            hFullIm[i] = (mag_lin[i] * sin(fase)).toFloat()
            if (i > 0 && i < N_FFT / 2) {
                hFullRe[N_FFT - i] = hFullRe[i]
                hFullIm[N_FFT - i] = -hFullIm[i] // Conjugado
            }
        }

        val hTime = FloatArray(N_FFT * 2)
        for (i in 0 until N_FFT) {
            val cbr = revTable[i]
            hTime[cbr * 2] = hFullRe[i]
            hTime[cbr * 2 + 1] = hFullIm[i]
        }
        fbasicfft(hTime, N_FFT, -1) // IFFT

        // Enventanado (Hamming) y Padding
        val hPadded = FloatArray(N_FFT * 2)
        for (i in 0 until N_H) {
            val window = 0.54f - 0.46f * cos(2.0 * PI * i / (N_H - 1)).toFloat()
            hPadded[i * 2] = hTime[i * 2] * window
            hPadded[i * 2 + 1] = 0f
        }
        // Llenar el resto de ceros de forma explícita
        for (i in N_H until N_FFT) {
            hPadded[i * 2] = 0f
            hPadded[i * 2 + 1] = 0f
        }

        val hPaddedRev = FloatArray(N_FFT * 2)
        for (i in 0 until N_FFT) {
            val cbr = revTable[i]
            hPaddedRev[cbr * 2] = hPadded[i * 2]
            hPaddedRev[cbr * 2 + 1] = hPadded[i * 2 + 1]
        }

        fbasicfft(hPaddedRev, N_FFT, 1) // FFT

        // Guardar el filtro final
        for (i in 0 until N_FFT) {
            H_eq_re[i] = hPaddedRev[i * 2]
            H_eq_im[i] = hPaddedRev[i * 2 + 1]
        }
    }

    // Puerto de process_data (Overlap-Add)
    fun processBlock(x: ShortArray, yOut: ShortArray) {
        for (i in 0 until N_X) {
            val cbr = revTable[i]
            X_cplx[cbr * 2] = x[i].toFloat()
            X_cplx[cbr * 2 + 1] = 0f
        }
        for (i in N_X until N_FFT) {
            val cbr = revTable[i]
            X_cplx[cbr * 2] = 0f
            X_cplx[cbr * 2 + 1] = 0f
        }

        fbasicfft(X_cplx, N_FFT, 1)

        for (i in 0 until N_FFT) {
            val Xr = X_cplx[2 * i]
            val Xi = X_cplx[2 * i + 1]
            val Hr = H_eq_re[i]
            val Hi = H_eq_im[i]
            X_cplx[2 * i] = Xr * Hr - Xi * Hi
            X_cplx[2 * i + 1] = Xr * Hi + Xi * Hr

            if (i < N_FFT / 2) {
                currentSpectrum[i] = sqrt(X_cplx[2 * i] * X_cplx[2 * i] + X_cplx[2 * i + 1] * X_cplx[2 * i + 1])
            }

        }

        // Bit reversal inplace para IFFT
        for (i in 0 until N_FFT) {
            val j = revTable[i]
            if (i < j) {
                var temp = X_cplx[2 * i]; X_cplx[2 * i] = X_cplx[2 * j]; X_cplx[2 * j] = temp
                temp = X_cplx[2 * i + 1]; X_cplx[2 * i + 1] = X_cplx[2 * j + 1]; X_cplx[2 * j + 1] = temp
            }
        }

        fbasicfft(X_cplx, N_FFT, -1)

        for (i in 0 until N_FFT) olaOut[i] += X_cplx[2 * i]

        for (i in 0 until N_X) {
            yOut[i] = olaOut[i].coerceIn(-32768f, 32767f).toInt().toShort()
        }

        for (i in 0 until N_FFT - N_X) olaOut[i] = olaOut[i + N_X]
        for (i in N_FFT - N_X until N_FFT) olaOut[i] = 0f
    }

    private fun fbasicfft(data: FloatArray, Nfft: Int, inv: Int) {
        // 1. Calcula FFTs de 2 puntos
        for (i in 0 until Nfft * 2 step 4) {
            val t1 = data[i]
            val t2 = data[i + 1]
            val r2 = data[i + 2]
            val i2 = data[i + 3]

            data[i] = t1 + r2
            data[i + 1] = (t2 + i2) * inv

            data[i + 2] = t1 - r2
            data[i + 3] = (t2 - i2) * inv
        }

        // 2. Calcula las combinaciones de la FFT usando la Look-Up Table
        var stepsintable = 512
        var N1 = 2

        while (N1 < Nfft) {
            for (k1 in 0 until Nfft step 2 * N1) {
                var indexsintable = 0
                var indexcostable = 512

                var ptr = k1 * 2
                var ptrN1 = (k1 + N1) * 2

                for (k in 0 until N1) {
                    val cosk = sinwn[indexcostable]
                    val senk = sinwn[indexsintable]

                    indexcostable += stepsintable
                    indexsintable += stepsintable

                    val hrN1 = data[ptrN1]
                    val hiN1 = data[ptrN1 + 1]
                    val hr = data[ptr]
                    val hi = data[ptr + 1]

                    // Cálculo mariposa (Butterfly)
                    val bwR = hrN1 * cosk + hiN1 * senk
                    val bwI = -hrN1 * senk + hiN1 * cosk

                    data[ptrN1] = hr - bwR
                    data[ptrN1 + 1] = hi - bwI
                    data[ptr] = hr + bwR
                    data[ptr + 1] = hi + bwI

                    ptr += 2
                    ptrN1 += 2
                }
            }
            stepsintable = stepsintable shr 1 // Bitwise shift right (equivalente a /= 2)
            N1 *= 2
        }

        // 3. Ajuste final para IFFT
        if (inv == -1) {
            val cte = 1.0f / Nfft
            // Multiplicar todo el arreglo (Reales e Imaginarios) por 1/N
            for (i in 0 until Nfft * 2) {
                data[i] *= cte
            }
            // Cambiar signo solo a los Imaginarios (índices impares)
            for (i in 1 until Nfft * 2 step 2) {
                data[i] = -data[i]
            }
        }
    }
}