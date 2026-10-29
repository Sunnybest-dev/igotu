package com.igotu.ui

import android.os.Bundle
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.igotu.R
import com.igotu.databinding.ActivityEmergencyStatusBinding
import com.igotu.sos.RecordingState
import com.igotu.sos.SosEngine
import com.igotu.sos.SosEvent
import com.igotu.sos.SosState
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EmergencyStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyStatusBinding
    private val viewModel: EmergencyStatusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityEmergencyStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Slide card up on enter
        binding.cardStatus.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
        )

        val eventId = intent.getStringExtra("event_id") ?: run { finish(); return }

        binding.btnCancel.setOnClickListener { viewModel.cancel(eventId) }

        lifecycleScope.launch {
            viewModel.sosState.collect { event ->
                if (event?.id == eventId) renderStatus(event)
                if (event?.state == SosState.RESOLVED || event?.state == SosState.CANCELLED) finish()
            }
        }
    }

    private fun renderStatus(event: SosEvent) {
        binding.tvEventId.text = "Event #${event.id.take(8)}"
        binding.tvShareUrl.text = event.shareUrl ?: "Generating…"

        val recordingText = when (event.recordingState) {
            RecordingState.ACTIVE    -> "RECORDING"
            RecordingState.UPLOADING -> "UPLOADING"
            RecordingState.COMPLETE  -> "SAVED"
            RecordingState.FAILED    -> "FAILED"
            RecordingState.STARTING  -> "STARTING"
            else                     -> "IDLE"
        }
        binding.tvRecordingStatus.text = recordingText
        binding.tvRecordingStatus.setTextColor(
            getColor(
                when (event.recordingState) {
                    RecordingState.ACTIVE -> android.R.color.holo_red_light
                    RecordingState.FAILED -> android.R.color.holo_orange_light
                    else                  -> android.R.color.holo_blue_light
                }
            )
        )

        val notified = event.contactsNotified.size
        binding.tvContactsStatus.text = if (notified > 0) "NOTIFIED ($notified)" else "PENDING"
        binding.tvContactsStatus.setTextColor(
            getColor(if (notified > 0) android.R.color.holo_green_dark else android.R.color.holo_orange_light)
        )
    }
}

@HiltViewModel
class EmergencyStatusViewModel @Inject constructor(
    private val sosEngine: SosEngine
) : ViewModel() {
    val sosState: StateFlow<SosEvent?> = sosEngine.state

    fun cancel(eventId: String) {
        viewModelScope.launch { sosEngine.cancel(eventId) }
    }

    fun resolve(eventId: String) {
        viewModelScope.launch { sosEngine.resolve(eventId) }
    }
}
