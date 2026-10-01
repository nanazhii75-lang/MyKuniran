package com.kuniran.feature.members

import com.kuniran.core.common.AppError
import com.kuniran.core.model.JoinRequest
import com.kuniran.core.model.RtMember
import com.kuniran.core.model.UserProfile

data class MembersUiState(
    val members: List<RtMember> = emptyList(),
    val pendingRequests: List<JoinRequest> = emptyList(),
    val currentUser: UserProfile? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null
)
