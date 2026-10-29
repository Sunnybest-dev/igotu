package com.igotu.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.igotu.databinding.ActivityPrivacyLockdownBinding
import com.igotu.privacy.PrivacyLockdownManager
import com.igotu.privacy.PrivacyLockdownSettings
import com.igotu.privacy.PrivacyLockdownSettingsStore
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PrivacyLockdownActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivacyLockdownBinding
    private val viewModel: PrivacyLockdownViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyLockdownBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnDeviceSettings.setOnClickListener { viewModel.openDeviceSettings() }

        binding.switchLockdown.setOnCheckedChangeListener { _, checked ->
            if (checked) viewModel.activate() else viewModel.deactivate()
        }

        binding.btnSave.setOnClickListener {
            viewModel.saveSettings(
                viewModel.settings.value.copy(
                    hideSensitiveNotifications = binding.cbHideNotifications.isChecked,
                    protectIgotuContent = binding.cbProtectContent.isChecked,
                    activateWithSos = binding.cbActivateWithSos.isChecked
                )
            )
            finish()
        }

        lifecycleScope.launch {
            viewModel.settings.collect { renderSettings(it) }
        }
    }

    private fun renderSettings(settings: PrivacyLockdownSettings) {
        binding.switchLockdown.setOnCheckedChangeListener(null)
        binding.switchLockdown.isChecked = settings.isCurrentlyActive
        binding.switchLockdown.setOnCheckedChangeListener { _, checked ->
            if (checked) viewModel.activate() else viewModel.deactivate()
        }
        binding.tvLockdownStatus.text = if (settings.isCurrentlyActive) "● ACTIVE" else "● INACTIVE"
        binding.tvLockdownStatus.setTextColor(
            getColor(if (settings.isCurrentlyActive) android.R.color.holo_green_dark else android.R.color.darker_gray)
        )
        binding.cbHideNotifications.isChecked = settings.hideSensitiveNotifications
        binding.cbProtectContent.isChecked = settings.protectIgotuContent
        binding.cbActivateWithSos.isChecked = settings.activateWithSos
    }
}

@HiltViewModel
class PrivacyLockdownViewModel @Inject constructor(
    private val manager: PrivacyLockdownManager,
    private val settingsStore: PrivacyLockdownSettingsStore
) : ViewModel() {

    private val _settings = MutableStateFlow(settingsStore.load())
    val settings: StateFlow<PrivacyLockdownSettings> = _settings

    fun activate() {
        viewModelScope.launch {
            val result = manager.activate()
            _settings.value = settingsStore.load()
            // Expose result.actions to UI so limitations are shown honestly
        }
    }

    fun deactivate() {
        viewModelScope.launch {
            manager.deactivate()
            _settings.value = settingsStore.load()
        }
    }

    fun saveSettings(settings: PrivacyLockdownSettings) {
        settingsStore.save(settings)
        _settings.value = settings
    }

    fun openDeviceSettings() = manager.openDeviceAppLockSettings()
}
