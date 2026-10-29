package com.igotu.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.igotu.R
import com.igotu.databinding.ActivityLandingBinding

class LandingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLandingBinding
    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private var currentPage = 0

    private val features = listOf(
        FeatureItem(R.drawable.ic_feature_voice,     R.string.feature_1_title, R.string.feature_1_desc),
        FeatureItem(R.drawable.ic_feature_location,  R.string.feature_2_title, R.string.feature_2_desc),
        FeatureItem(R.drawable.ic_feature_recording, R.string.feature_3_title, R.string.feature_3_desc),
    )

    private val autoScrollRunnable = Runnable {
        currentPage = (currentPage + 1) % features.size
        binding.vpFeatures.setCurrentItem(currentPage, true)
        scheduleAutoScroll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFeaturePager()
        animateHero()

        binding.btnGetStarted.setOnClickListener { openAuth(startOnSignUp = true) }
        binding.tvSignIn.setOnClickListener { openAuth(startOnSignUp = false) }
    }

    private fun setupFeaturePager() {
        binding.vpFeatures.adapter = FeaturePagerAdapter(features)
        binding.vpFeatures.offscreenPageLimit = 1

        TabLayoutMediator(binding.dotsIndicator, binding.vpFeatures) { _, _ -> }.attach()

        binding.vpFeatures.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { currentPage = position }
        })

        scheduleAutoScroll()
    }

    private fun scheduleAutoScroll() {
        autoScrollHandler.postDelayed(autoScrollRunnable, 3000)
    }

    private fun animateHero() {
        val fadeSlideUp = AnimationUtils.loadAnimation(this, R.anim.splash_text_in)
        binding.tvLandingHeadline.visibility = View.VISIBLE
        binding.tvLandingSub.visibility = View.VISIBLE
        binding.btnGetStarted.visibility = View.VISIBLE
        binding.tvSignIn.visibility = View.VISIBLE
        binding.tvLandingHeadline.startAnimation(fadeSlideUp)
        binding.tvLandingSub.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.splash_tagline_in)
        )
    }

    private fun openAuth(startOnSignUp: Boolean) {
        startActivity(
            Intent(this, AuthActivity::class.java)
                .putExtra(AuthActivity.EXTRA_START_TAB, if (startOnSignUp) 1 else 0)
        )
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    override fun onResume() {
        super.onResume()
        scheduleAutoScroll()
    }

    override fun onPause() {
        super.onPause()
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }
}
