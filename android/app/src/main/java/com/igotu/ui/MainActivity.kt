package com.igotu.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.igotu.R
import com.igotu.databinding.ActivityMainBinding
import com.igotu.sos.SosState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var sosHoldStart = 0L
    private val SOS_HOLD_MS = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        runEntranceAnimations()
        setupSosButton()
        setupQuickActions()
        observeState()
    }

    private fun runEntranceAnimations() {
        // SOS button: scale + fade in
        val sosAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale)
        binding.btnSos.startAnimation(sosAnim)

        // Action cards: staggered slide-up
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        binding.layoutSafetyBanner.startAnimation(slideUp)
    }

    private fun startSosPulse() {
        binding.ivSosPulse.alpha = 0.6f
        val scaleX = ObjectAnimator.ofFloat(binding.ivSosPulse, "scaleX", 1f, 1.25f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.ivSosPulse, "scaleY", 1f, 1.25f, 1f)
        val alpha  = ObjectAnimator.ofFloat(binding.ivSosPulse, "alpha", 0.6f, 0f, 0.6f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 1200
            interpolator = OvershootInterpolator(0.5f)
            start()
        }
    }

    private fun setupSosButton() {
        binding.btnSos.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    sosHoldStart = System.currentTimeMillis()
                    startSosPulse()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (System.currentTimeMillis() - sosHoldStart >= SOS_HOLD_MS) {
                        viewModel.activateSos()
                    }
                    binding.ivSosPulse.animate().alpha(0f).setDuration(300).start()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupQuickActions() {
        binding.btnEmergencyContacts.setOnClickListener {
            startActivity(Intent(this, EmergencyContactsActivity::class.java))
        }
        binding.btnVoiceSettings.setOnClickListener {
            startActivity(Intent(this, VoiceSettingsActivity::class.java))
        }
        binding.btnConnections.setOnClickListener {
            startActivity(Intent(this, ConnectionsActivity::class.java))
        }
        binding.btnPrivacy.setOnClickListener {
            startActivity(Intent(this, PrivacyLockdownActivity::class.java))
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.sosState.collect { event ->
                when (event?.state) {
                    SosState.ACTIVE -> startActivity(
                        Intent(this@MainActivity, EmergencyStatusActivity::class.java)
                            .putExtra("event_id", event.id)
                    )
                    else -> {}
                }
            }
        }
        lifecycleScope.launch {
            viewModel.voiceActive.collect { active ->
                binding.tvVoiceStatus.text = if (active) "● ACTIVE" else "● INACTIVE"
                binding.tvVoiceStatus.setTextColor(
                    getColor(if (active) android.R.color.holo_green_dark else android.R.color.darker_gray)
                )
            }
        }
    }
}
