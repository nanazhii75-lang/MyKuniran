package com.kuniran.feature.attendance

import com.kuniran.core.common.AppError
import com.kuniran.core.model.WargaActivityLog

data class AttendanceUiState(
    val isLoading: Boolean = false,
    val history: List<WargaActivityLog> = emptyList(),
    val lastScannedEvent: String? = null,
    val successMessage: String? = null,
    val error: AppError? = null,
    val showManualDialog: Boolean = false,
    val isAdmin: Boolean = false,
    val rtId: String? = null,
    val showGeneratorDialog: Boolean = false
)
