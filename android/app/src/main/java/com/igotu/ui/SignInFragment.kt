package com.igotu.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.igotu.databinding.FragmentSignInBinding
import kotlinx.coroutines.launch

class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnSignIn.setOnClickListener { attemptSignIn() }
        binding.btnGoogleSignIn.setOnClickListener {
            Toast.makeText(requireContext(), "Google Sign-In coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(requireContext(), "Password reset coming soon", Toast.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.state.collect { state ->
                when (state) {
                    is AuthState.Loading -> setLoading(true)
                    is AuthState.Success -> {
                        setLoading(false)
                        navigateToMain()
                    }
                    is AuthState.Error -> {
                        setLoading(false)
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        authViewModel.resetState()
                    }
                    else -> setLoading(false)
                }
            }
        }
    }

    private fun attemptSignIn() {
        val email    = binding.tilSignInEmail.editText?.text?.toString().orEmpty().trim()
        val password = binding.tilSignInPassword.editText?.text?.toString().orEmpty()
        if (email.isEmpty())    { binding.tilSignInEmail.error = "Required"; return }
        if (password.isEmpty()) { binding.tilSignInPassword.error = "Required"; return }
        binding.tilSignInEmail.error = null
        binding.tilSignInPassword.error = null
        authViewModel.login(email, password)
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSignIn.isEnabled = !loading
        binding.btnSignIn.text = if (loading) "Signing in…" else getString(com.igotu.R.string.sign_in)
    }

    private fun navigateToMain() {
        startActivity(Intent(requireContext(), MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
