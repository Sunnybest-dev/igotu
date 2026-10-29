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
import com.igotu.databinding.FragmentSignUpBinding
import kotlinx.coroutines.launch

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnSignUp.setOnClickListener { attemptSignUp() }
        binding.btnGoogleSignUp.setOnClickListener {
            Toast.makeText(requireContext(), "Google Sign-Up coming soon", Toast.LENGTH_SHORT).show()
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

    private fun attemptSignUp() {
        val name     = binding.tilSignUpName.editText?.text?.toString().orEmpty().trim()
        val email    = binding.tilSignUpEmail.editText?.text?.toString().orEmpty().trim()
        val password = binding.tilSignUpPassword.editText?.text?.toString().orEmpty()
        val confirm  = binding.tilSignUpConfirm.editText?.text?.toString().orEmpty()

        var valid = true
        if (name.isEmpty())      { binding.tilSignUpName.error = "Required";                valid = false }
        if (email.isEmpty())     { binding.tilSignUpEmail.error = "Required";               valid = false }
        if (password.length < 8) { binding.tilSignUpPassword.error = "Min 8 characters";   valid = false }
        if (confirm != password) { binding.tilSignUpConfirm.error = "Passwords don't match"; valid = false }
        if (!valid) return

        binding.tilSignUpName.error = null
        binding.tilSignUpEmail.error = null
        binding.tilSignUpPassword.error = null
        binding.tilSignUpConfirm.error = null
        authViewModel.register(name, email, password)
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSignUp.isEnabled = !loading
        binding.btnSignUp.text = if (loading) "Creating account…" else getString(com.igotu.R.string.sign_up)
    }

    private fun navigateToMain() {
        startActivity(Intent(requireContext(), MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
