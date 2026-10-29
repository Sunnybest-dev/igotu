package com.igotu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igotu.sos.SosEngine
import com.igotu.sos.SosEvent
import com.igotu.sos.TriggerMethod
import com.igotu.voice.VoiceTriggerSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sosEngine: SosEngine,
    private val voiceSettingsStore: VoiceTriggerSettingsStore
) : ViewModel() {

    val sosState: StateFlow<SosEvent?> = sosEngine.state

    val voiceActive: StateFlow<Boolean> = voiceSettingsStore.settings
        .map { it.enabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun activateSos() {
        sosEngine.activate(TriggerMethod.BUTTON)
    }
}
