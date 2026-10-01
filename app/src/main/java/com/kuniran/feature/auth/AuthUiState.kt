package com.kuniran.feature.auth

import com.kuniran.core.common.AppError
import com.kuniran.core.model.UserProfile

data class AuthUiState(
    val currentUser: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val joinStatus: String? = null,
    val previewLabel: String? = null,
    val usernameAvailable: String? = null
)
