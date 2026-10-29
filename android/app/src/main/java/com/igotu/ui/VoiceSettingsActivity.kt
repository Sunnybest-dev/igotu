package com.igotu.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.igotu.R
import com.igotu.databinding.ActivityVoiceSettingsBinding
import com.igotu.voice.VoiceTriggerConfig
import com.igotu.voice.VoiceTriggerSettings
import com.igotu.voice.VoiceTriggerSettingsStore
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VoiceSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceSettingsBinding
    private val viewModel: VoiceSettingsViewModel by viewModels()

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.enableVoiceTrigger()
        else binding.switchVoiceEnabled.isChecked = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnEnrollVoice.setOnClickListener {
            startActivity(Intent(this, VoiceEnrollmentActivity::class.java))
        }

        binding.switchVoiceEnabled.setOnCheckedChangeListener { _, checked ->
            if (!checked) { viewModel.disableVoiceTrigger(); return@setOnCheckedChangeListener }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
                viewModel.enableVoiceTrigger()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        binding.rgSensitivity.setOnCheckedChangeListener { _, checkedId ->
            val sensitivity = when (checkedId) {
                R.id.rbLow  -> VoiceTriggerConfig.Sensitivity.LOW
                R.id.rbHigh -> VoiceTriggerConfig.Sensitivity.HIGH
                else        -> VoiceTriggerConfig.Sensitivity.MEDIUM
            }
            viewModel.updateSettings(viewModel.settings.value.copy(sensitivity = sensitivity))
        }

        binding.rgActivationMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = if (checkedId == R.id.rbImmediate)
                VoiceTriggerSettings.ActivationMode.IMMEDIATE
            else
                VoiceTriggerSettings.ActivationMode.COUNTDOWN
            viewModel.updateSettings(viewModel.settings.value.copy(activationMode = mode))
        }

        lifecycleScope.launch {
            viewModel.settings.collect { renderSettings(it) }
        }
    }

    private fun renderSettings(settings: VoiceTriggerSettings) {
        binding.switchVoiceEnabled.setOnCheckedChangeListener(null)
        binding.switchVoiceEnabled.isChecked = settings.enabled
        binding.switchVoiceEnabled.setOnCheckedChangeListener { _, checked ->
            if (!checked) { viewModel.disableVoiceTrigger(); return@setOnCheckedChangeListener }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
                viewModel.enableVoiceTrigger()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        val sensitivityId = when (settings.sensitivity) {
            VoiceTriggerConfig.Sensitivity.LOW  -> R.id.rbLow
            VoiceTriggerConfig.Sensitivity.HIGH -> R.id.rbHigh
            else                                -> R.id.rbMedium
        }
        if (binding.rgSensitivity.checkedRadioButtonId != sensitivityId)
            binding.rgSensitivity.check(sensitivityId)

        val modeId = if (settings.activationMode == VoiceTriggerSettings.ActivationMode.IMMEDIATE)
            R.id.rbImmediate else R.id.rbCountdown
        if (binding.rgActivationMode.checkedRadioButtonId != modeId)
            binding.rgActivationMode.check(modeId)
    }
}

@HiltViewModel
class VoiceSettingsViewModel @Inject constructor(
    private val settingsStore: VoiceTriggerSettingsStore
) : ViewModel() {

    val settings: StateFlow<VoiceTriggerSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VoiceTriggerSettings())

    fun enableVoiceTrigger() {
        viewModelScope.launch {
            settingsStore.update(settings.value.copy(enabled = true))
        }
    }

    fun disableVoiceTrigger() {
        viewModelScope.launch {
            settingsStore.update(settings.value.copy(enabled = false))
        }
    }

    fun updateSettings(updated: VoiceTriggerSettings) {
        viewModelScope.launch { settingsStore.update(updated) }
    }
}
