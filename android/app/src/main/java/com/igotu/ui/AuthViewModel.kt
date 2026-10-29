package com.igotu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.igotu.data.ApiService
import com.igotu.data.LoginRequest
import com.igotu.data.RegisterRequest
import com.igotu.data.TokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun login(email: String, password: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = api.login(LoginRequest(email.trim(), password))
                tokenStore.token = response.token
                tokenStore.userId = response.user.id
                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    fun register(fullName: String, email: String, password: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = api.register(RegisterRequest(email.trim(), password, fullName.trim()))
                tokenStore.token = response.token
                tokenStore.userId = response.user.id
                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun resetState() { _state.value = AuthState.Idle }
}
