package com.kuniran.feature.forum

import com.kuniran.core.common.AppError
import com.kuniran.core.model.Post

enum class ForumFilter {
    ALL,
    QUESTION,
    SUGGESTION
}

data class ForumThreadUiState(
    val isLoading: Boolean = false,
    val threads: List<Post> = emptyList(),
    val filteredThreads: List<Post> = emptyList(),
    val selectedFilter: ForumFilter = ForumFilter.ALL,
    val showCreateDialog: Boolean = false,
    val successMessage: String? = null,
    val error: AppError? = null
)
