package com.kuniran.feature.members

import com.kuniran.core.common.AppError
import com.kuniran.core.model.Warga

data class ResidentDirectoryUiState(
    val isLoading: Boolean = false,
    val residents: List<Warga> = emptyList(),
    val filteredResidents: List<Warga> = emptyList(),
    val searchQuery: String = "",
    val availableBlocks: List<String> = emptyList(),
    val selectedBlock: String? = null,
    val error: AppError? = null
)
