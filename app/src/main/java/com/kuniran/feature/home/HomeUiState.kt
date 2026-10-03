package com.kuniran.feature.home

import com.kuniran.core.common.AppError
import com.kuniran.core.model.Post
import com.kuniran.core.model.RtGroup
import com.kuniran.core.model.UserProfile

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val authorNames: Map<String, String> = emptyMap(),
    val currentUser: UserProfile? = null,
    val rtGroup: RtGroup? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: AppError? = null
)
