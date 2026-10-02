package com.kuniran.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuniran.core.common.AppError
import com.kuniran.core.common.Resource
import com.kuniran.domain.usecase.CancelJoinRequestUseCase
import com.kuniran.domain.usecase.CheckUsernameAvailableUseCase
import com.kuniran.domain.usecase.CreateRtUseCase
import com.kuniran.domain.usecase.GetCurrentUserUseCase
import com.kuniran.domain.usecase.PreviewRtUseCase
import com.kuniran.domain.usecase.RequestJoinRtUseCase
import com.kuniran.domain.usecase.SignInWithGoogleUseCase
import com.kuniran.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val createRtUseCase: CreateRtUseCase,
    private val previewRtUseCase: PreviewRtUseCase,
    private val requestJoinRtUseCase: RequestJoinRtUseCase,
    private val cancelJoinRequestUseCase: CancelJoinRequestUseCase,
    private val checkUsernameAvailableUseCase: CheckUsernameAvailableUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun onGoogleSignInFailed(throwable: Throwable) {
        val detail = throwable.message?.takeIf { it.isNotBlank() } ?: throwable.javaClass.simpleName
        _uiState.update { it.copy(isLoading = false, error = AppError.LoginFailed(detail)) }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = signInWithGoogleUseCase(idToken)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, currentUser = res.data) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun previewRt(inviteUsername: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, previewLabel = null) }
            when (val res = previewRtUseCase(inviteUsername)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, previewLabel = res.data) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun requestJoinRt(inviteUsername: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = requestJoinRtUseCase(inviteUsername)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, joinStatus = res.data) }
                    onSuccess(res.data)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun createRt(
        name: String,
        rtNumber: String,
        rwNumber: String,
        desa: String,
        dukuh: String,
        lingkungan: String,
        inviteUsername: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = createRtUseCase(
                name = name,
                rtNumber = rtNumber,
                rwNumber = rwNumber,
                desa = desa,
                dukuh = dukuh,
                lingkungan = lingkungan,
                inviteUsername = inviteUsername
            )) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(res.data)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun cancelJoinRequest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = cancelJoinRequestUseCase()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, joinStatus = null) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun checkUsername(username: String) {
        viewModelScope.launch {
            when (val res = checkUsernameAvailableUseCase(username)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(usernameAvailable = res.data) }
                }
                else -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
