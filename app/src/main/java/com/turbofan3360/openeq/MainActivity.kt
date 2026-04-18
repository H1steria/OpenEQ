package com.turbofan3360.openeq

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import com.turbofan3360.openeq.appdata.RoomDatabaseHandler
import com.turbofan3360.openeq.appdata.SharedPreferencesSettings
import com.turbofan3360.openeq.audioprocessing.ForegroundServiceHandler
import com.turbofan3360.openeq.audioprocessing.eqFrequenciesToLabels
import com.turbofan3360.openeq.audioprocessing.getEqBands
import com.turbofan3360.openeq.audioprocessing.getEqRange
import com.turbofan3360.openeq.audioprocessing.globalEqAllowed
import com.turbofan3360.openeq.audioprocessing.AudioEngine
import com.turbofan3360.openeq.audioprocessing.FastConvEQ
import com.turbofan3360.openeq.ui.screens.MainScreen
import com.turbofan3360.openeq.ui.theme.OpenEQTheme
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    val context = getApplication<Application>()

    val audioEngine = AudioEngine(context)

    // State - whether EQ service is enabled or not
    var eqEnabled by mutableStateOf(false)

    // Whether the device supports global audio EQ
//    val globalAudioAllowed = false

    // Whether to try and attach the EQ to the global audio mix
    var tryGlobalAudio by mutableStateOf(false)

    // EQ frequency bands in milliHz
    val eqFrequencyBandsStr = FastConvEQ.BAND_LABELS
    val eqRange = listOf(-15f, 15f) // +/- 15 dB
    var eqLevels = List(eqFrequencyBandsStr.size) { 0f }.toMutableStateList()

}

class MainActivity : ComponentActivity() {
    val myViewModel: MainActivityViewModel by viewModels()

    val appSettings by lazy { SharedPreferencesSettings(this, "app_settings") }
    //private val foregroundServiceHandler by lazy { ForegroundServiceHandler(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Function that handles initialization of app state (database, shared preferences, finding foreground service)
        appDataInit()

        // Handles starting the app UI
        setContent {
            OpenEQTheme {
                MainScreen(
                    eqEnabled = myViewModel.eqEnabled,
                    eqToggle = ::toggleEq,

                    tryGlobal = myViewModel.tryGlobalAudio,
                    toggleGlobal = ::toggleGlobalAudio,

                    eqLevels = myViewModel.eqLevels,
                    // Saving EQ levels + passing them to the foreground service managing EQ objects
                    updateEqLevel = { index: Int, value: Float ->
                        myViewModel.eqLevels[index] = value
                        myViewModel.audioEngine.updateEq(myViewModel.eqLevels.toList())
                    },

                    frequencyBands = myViewModel.eqFrequencyBandsStr,
                    eqRange = myViewModel.eqRange,

                    onPresetUpdate = { presetId ->
                        RoomDatabaseHandler.updatePreset(presetId, myViewModel.eqLevels, lifecycleScope)
                    },
                    onPresetSave = { presetId ->
                        RoomDatabaseHandler.addPreset(presetId, myViewModel.eqLevels, lifecycleScope)
                    },
                    onPresetSelect = { presetId ->
                        RoomDatabaseHandler.getPreset(
                            presetId,
                            lifecycleScope
                        ) { presetVals ->
                            // Safely update the EQ levels to the retrieved values matching length, without changing the list size
                            for (i in myViewModel.eqLevels.indices) {
                                myViewModel.eqLevels[i] = if (i < presetVals.size) presetVals[i] else 0f
                            }
                            myViewModel.audioEngine.updateEq(myViewModel.eqLevels.toList())
                        }
                    },
                    onPresetDelete = { presetId ->
                        RoomDatabaseHandler.deletePreset(
                            presetId,
                            lifecycleScope
                        ) {
                            // Clearing the EQ levels back to default
                            for (i in 0..<myViewModel.eqLevels.size) {
                                myViewModel.eqLevels[i] = 0f
                            }
                            myViewModel.audioEngine.updateEq(myViewModel.eqLevels.toList())
                        }
                    },
                    audioSpectrumFlow = myViewModel.audioEngine.spectrumFlow,
                    processingTimeFlow = myViewModel.audioEngine.processingTimeFlow
                )
            }
        }
    }

    override fun onDestroy() {
        RoomDatabaseHandler.updatePresetBlocking(
            getString(R.string.db_key_recent_eq_levels),
            myViewModel.eqLevels
        )
        RoomDatabaseHandler.dbInitialized = false

        myViewModel.audioEngine.stop()

        super.onDestroy()
    }

    private fun appDataInit() {
        appDatabaseInit()

        myViewModel.tryGlobalAudio = appSettings.getAppSettingBoolean(
            getString(R.string.shared_preferences_global_mix_key),
            false
        )

        myViewModel.audioEngine.updateEq(myViewModel.eqLevels.toList())
    }

    private fun appDatabaseInit() {
        // Starts the app database to access stored preset info
        RoomDatabaseHandler.buildDatabase("preset-database", this)

        val currentBandCount = myViewModel.eqFrequencyBandsStr.size

        // Checking if a "latest_eq_levels" preset already exists
        if (RoomDatabaseHandler.idStrings.contains(getString(R.string.db_key_recent_eq_levels))) {
            RoomDatabaseHandler.getPreset(
                getString(R.string.db_key_recent_eq_levels),
                lifecycleScope
            ) { values ->
                if (values.size == currentBandCount) {
                    for (i in 0 until currentBandCount) {
                        myViewModel.eqLevels[i] = values[i]
                    }
                } else {
                    // Si el tamaño es diferente (ej. venía de 5 bandas y ahora son 10)
                    // Reiniciamos los niveles in-place sin modificar la longitud real
                    for (i in 0 until currentBandCount) {
                        myViewModel.eqLevels[i] = 0f
                    }

                    // Actualizamos la base de datos con el nuevo formato
                    RoomDatabaseHandler.updatePreset(
                        getString(R.string.db_key_recent_eq_levels),
                        myViewModel.eqLevels,
                        lifecycleScope
                    )
                }
                // Sincronizar con el motor de audio
                myViewModel.audioEngine.updateEq(myViewModel.eqLevels.toList())
            }
        } else {
            // Inicialización por primera vez
            for (i in 0 until currentBandCount) {
                myViewModel.eqLevels[i] = 0f
            }

            RoomDatabaseHandler.addPreset(
                getString(R.string.db_key_recent_eq_levels),
                myViewModel.eqLevels,
                lifecycleScope
            )
        }
    }

    private fun toggleEq() {
        if (!myViewModel.eqEnabled) {
            // Verificar permiso de micrófono en tiempo de ejecución (Requisito de Android)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
                return
            }

            // Iniciar la captura y el procesamiento
            myViewModel.audioEngine.updateEq(myViewModel.eqLevels.toList())
            myViewModel.audioEngine.start()
            myViewModel.eqEnabled = true
        } else {
            // Detener
            myViewModel.audioEngine.stop()
            myViewModel.eqEnabled = false
        }
    }

    private fun toggleGlobalAudio() {
        // la idea de "Global Audio Mix" ya no aplica.
        // Mostramos un mensaje indicando que no está disponible en este modo.
        Toast.makeText(
            this,
            "El modo Global Mix no aplica usando captura de Micrófono directo.",
            Toast.LENGTH_LONG
        ).show()

        myViewModel.tryGlobalAudio = false
    }
}
