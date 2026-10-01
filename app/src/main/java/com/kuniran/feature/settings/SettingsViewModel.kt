package com.kuniran.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.RtRepository
import com.kuniran.domain.usecase.GetCurrentUserUseCase
import com.kuniran.domain.usecase.LeaveRtUseCase
import com.kuniran.domain.usecase.SetAutoApproveUseCase
import com.kuniran.domain.usecase.SetInviteUsernameUseCase
import com.kuniran.domain.usecase.SignOutUseCase
import com.kuniran.domain.usecase.UpdateProfileUseCase
import com.kuniran.domain.usecase.UpdateRtInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val leaveRtUseCase: LeaveRtUseCase,
    private val setAutoApproveUseCase: SetAutoApproveUseCase,
    private val setInviteUsernameUseCase: SetInviteUsernameUseCase,
    private val updateRtInfoUseCase: UpdateRtInfoUseCase,
    private val rtRepository: RtRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _uiState.update { it.copy(currentUser = user) }
                if (user?.rtId != null) {
                    observeRtGroup(user.rtId)
                }
            }
        }
    }

    private fun observeRtGroup(rtId: String) {
        viewModelScope.launch {
            rtRepository.getRtGroupFlow(rtId).collect { group ->
                _uiState.update { it.copy(rtGroup = group) }
            }
        }
    }

    fun updateProfile(fullName: String, phone: String?, house: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = updateProfileUseCase(fullName, phone, house)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun setAutoApprove(enabled: Boolean) {
        viewModelScope.launch {
            when (val res = setAutoApproveUseCase(enabled)) {
                is Resource.Error -> _uiState.update { it.copy(error = res.error) }
                else -> Unit
            }
        }
    }

    fun setInviteUsername(username: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = setInviteUsernameUseCase(username)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun updateRtInfo(
        name: String,
        rtNumber: String,
        rwNumber: String,
        desa: String,
        dukuh: String,
        lingkungan: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = updateRtInfoUseCase(name, rtNumber, rwNumber, desa, dukuh, lingkungan)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun leaveRt(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = leaveRtUseCase()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun signOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            signOutUseCase()
            onSuccess()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
