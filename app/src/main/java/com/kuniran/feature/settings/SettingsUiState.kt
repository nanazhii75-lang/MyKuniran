package com.kuniran.feature.settings

import com.kuniran.core.common.AppError
import com.kuniran.core.model.RtGroup
import com.kuniran.core.model.UserProfile

data class SettingsUiState(
    val currentUser: UserProfile? = null,
    val rtGroup: RtGroup? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null
)
