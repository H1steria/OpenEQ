package com.turbofan3360.openeq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel

import com.turbofan3360.openeq.ui.screens.MainScreen
import com.turbofan3360.openeq.ui.theme.OpenEQTheme
import com.turbofan3360.openeq.audioprocessing.getEqBands
import com.turbofan3360.openeq.audioprocessing.eqFrequenciesToLabels

class MainActivityViewModel: ViewModel() {
    // State - whether EQ service is enabled or not
    var eqEnabled by mutableStateOf(false)
    // EQ frequency bands in milliHz
    val eqFrequencyBands = getEqBands()
    // String labels for EQ frequency bands
    val eqFrequencyBandsStr = eqFrequenciesToLabels(eqFrequencyBands)
    // State of the sliders (and so EQ levels)
    var eqLevels = mutableStateListOf(*MutableList(eqFrequencyBands.size) {0f}.toTypedArray())
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainActivityViewModel = viewModel()
            OpenEQTheme {
                MainScreen(
                    viewModel.eqEnabled,
                    eqToggle = {viewModel.eqEnabled = !viewModel.eqEnabled},
                    viewModel.eqLevels,
                    updateEqLevel = {index:Int, value:Float -> viewModel.eqLevels[index] = value},
                    frequencyBands = viewModel.eqFrequencyBandsStr,
                )
            }
        }
    }
}
