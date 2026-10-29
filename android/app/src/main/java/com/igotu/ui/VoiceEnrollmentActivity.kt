package com.igotu.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.igotu.databinding.ActivityVoiceEnrollmentBinding
import com.igotu.voice.EnrollmentState
import com.igotu.voice.VoiceEnrollmentManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VoiceEnrollmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceEnrollmentBinding
    private val viewModel: VoiceEnrollmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceEnrollmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRecord.setOnClickListener {
            binding.btnRecord.isEnabled = false
            viewModel.recordSample()
        }
        binding.btnReset.setOnClickListener { viewModel.reset() }

        lifecycleScope.launch {
            viewModel.enrollmentState.collect { updateUi(it) }
        }
    }

    private fun updateUi(state: EnrollmentState) {
        binding.tvProgress.text = "Sample ${state.samplesRecorded} of ${state.samplesRequired}"
        binding.progressEnrollment.max = state.samplesRequired
        binding.progressEnrollment.progress = state.samplesRecorded
        binding.btnRecord.isEnabled = !state.isComplete

        when {
            state.error != null -> {
                binding.tvStatus.text = "Error: ${state.error}"
                binding.tvStatus.setTextColor(getColor(android.R.color.holo_red_light))
                binding.tvStatus.visibility = android.view.View.VISIBLE
                binding.btnRecord.isEnabled = true
            }
            state.isComplete -> {
                binding.tvInstruction.text = "Voice profile created ✓"
                binding.tvStatus.text = "Enrollment complete. You can now use voice trigger."
                binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                binding.tvStatus.visibility = android.view.View.VISIBLE
            }
            else -> {
                binding.tvInstruction.text = "Say \"IGOT-U\" clearly"
                binding.tvStatus.visibility = android.view.View.INVISIBLE
                binding.btnRecord.isEnabled = true
            }
        }
    }
}

@HiltViewModel
class VoiceEnrollmentViewModel @Inject constructor(
    private val enrollmentManager: VoiceEnrollmentManager
) : ViewModel() {

    val enrollmentState: StateFlow<EnrollmentState> = enrollmentManager.state

    fun recordSample() {
        viewModelScope.launch { enrollmentManager.recordSample() }
    }

    fun reset() = enrollmentManager.reset()
}
