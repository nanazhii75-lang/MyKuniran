package com.kuniran.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuniran.core.common.AppError
import com.kuniran.core.common.PhoneFormat
import com.kuniran.core.common.Resource
import com.kuniran.domain.usecase.GetCurrentUserUseCase
import com.kuniran.domain.usecase.SignOutUseCase
import com.kuniran.domain.usecase.UpdateProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompleteProfileUiState(
    val loaded: Boolean = false,
    val initialName: String = "",
    val initialPhone: String = "",
    val initialHouse: String = "",
    val isSaving: Boolean = false,
    val phoneInvalid: Boolean = false,
    val error: AppError? = null
)

class CompleteProfileViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompleteProfileUiState())
    val uiState: StateFlow<CompleteProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().first()
            _uiState.update {
                it.copy(
                    loaded = true,
                    // Nama bawaan sistem tidak ditawarkan sebagai isian awal.
                    initialName = user?.fullName?.takeIf { n -> n != "Warga Baru" }.orEmpty(),
                    initialPhone = user?.phoneNumber.orEmpty(),
                    initialHouse = user?.houseInfo.orEmpty()
                )
            }
        }
    }

    fun onInputChanged() {
        _uiState.update { it.copy(phoneInvalid = false, error = null) }
    }

    fun save(rawName: String, rawPhone: String, rawHouse: String, onDone: () -> Unit) {
        val name = rawName.trim()
        val phone = PhoneFormat.normalize(rawPhone)
        if (name.isEmpty()) {
            _uiState.update { it.copy(error = AppError.FieldInvalid) }
            return
        }
        if (!PhoneFormat.isValid(phone)) {
            _uiState.update { it.copy(phoneInvalid = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, phoneInvalid = false) }
            when (val res = updateProfileUseCase(name, phone, rawHouse.trim().ifBlank { null })) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    onDone()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            signOutUseCase()
            onDone()
        }
    }
}
