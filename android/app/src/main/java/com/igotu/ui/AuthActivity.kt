package com.igotu.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.igotu.R
import com.igotu.databinding.ActivityAuthBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.vpAuth.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment =
                if (position == 0) SignInFragment() else SignUpFragment()
        }

        TabLayoutMediator(binding.tabAuth, binding.vpAuth) { tab, pos ->
            tab.text = if (pos == 0) getString(R.string.sign_in) else getString(R.string.sign_up)
        }.attach()

        binding.vpAuth.setCurrentItem(intent.getIntExtra(EXTRA_START_TAB, 0), false)
        binding.btnAuthBack.setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_START_TAB = "extra_start_tab"
    }
}
