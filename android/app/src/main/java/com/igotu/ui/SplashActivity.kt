package com.igotu.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.igotu.R
import com.igotu.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide system status/nav bars for a true full-screen splash
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE
        )
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        runAnimations()
    }

    private fun runAnimations() {
        val shieldAnim   = AnimationUtils.loadAnimation(this, R.anim.splash_shield_in)
        val textAnim     = AnimationUtils.loadAnimation(this, R.anim.splash_text_in)
        val taglineAnim  = AnimationUtils.loadAnimation(this, R.anim.splash_tagline_in)

        binding.ivSplashShield.visibility  = View.VISIBLE
        binding.tvSplashName.visibility    = View.VISIBLE
        binding.tvSplashTagline.visibility = View.VISIBLE

        binding.ivSplashShield.startAnimation(shieldAnim)
        binding.tvSplashName.startAnimation(textAnim)
        binding.tvSplashTagline.startAnimation(taglineAnim)

        // Total animation ends ~1700ms; navigate after 2400ms
        binding.root.postDelayed({
            startActivity(Intent(this, LandingActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2400)
    }
}
