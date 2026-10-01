package com.kuniran.feature.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuniran.core.common.Resource
import com.kuniran.core.model.Post
import com.kuniran.core.model.PostType
import com.kuniran.domain.usecase.CreatePostUseCase
import com.kuniran.domain.usecase.GetCurrentUserUseCase
import com.kuniran.domain.usecase.GetRtFeedUseCase
import com.kuniran.domain.usecase.SyncFeedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForumViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getRtFeedUseCase: GetRtFeedUseCase,
    private val createPostUseCase: CreatePostUseCase,
    private val syncFeedUseCase: SyncFeedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForumThreadUiState(isLoading = true))
    val uiState: StateFlow<ForumThreadUiState> = _uiState.asStateFlow()

    private var currentRtId: String? = null
    private var currentUserId: String = ""

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                if (user != null) {
                    currentRtId = user.rtId
                    currentUserId = user.id
                    user.rtId?.let { rtId ->
                        observeThreads(rtId)
                    }
                }
            }
        }
    }

    private fun observeThreads(rtId: String) {
        viewModelScope.launch {
            getRtFeedUseCase(rtId).collect { posts ->
                val discussionPosts = posts.filter {
                    it.type == PostType.DISKUSI ||
                    it.title.startsWith("[Tanya]", ignoreCase = true) ||
                    it.title.startsWith("[Saran]", ignoreCase = true) ||
                    it.title.startsWith("[Diskusi]", ignoreCase = true)
                }

                _uiState.update { state ->
                    val filtered = filterThreads(discussionPosts, state.selectedFilter)
                    state.copy(
                        isLoading = false,
                        threads = discussionPosts,
                        filteredThreads = filtered
                    )
                }
            }
        }
    }

    fun setFilter(filter: ForumFilter) {
        _uiState.update { state ->
            val filtered = filterThreads(state.threads, filter)
            state.copy(selectedFilter = filter, filteredThreads = filtered)
        }
    }

    fun createThread(title: String, content: String, category: String) {
        val rtId = currentRtId ?: return
        val prefix = when (category) {
            "QUESTION" -> "[Tanya] "
            "SUGGESTION" -> "[Saran] "
            else -> "[Diskusi] "
        }
        val fullTitle = if (title.startsWith("[")) title else "$prefix$title"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showCreateDialog = false) }
            val result = createPostUseCase(
                rtId = rtId,
                authorId = currentUserId,
                title = fullTitle,
                content = content,
                type = PostType.DISKUSI
            )

            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = fullTitle) }
                    refresh()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun refresh() {
        val rtId = currentRtId ?: return
        viewModelScope.launch {
            syncFeedUseCase(rtId)
        }
    }

    fun setShowCreateDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateDialog = show) }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun filterThreads(threads: List<Post>, filter: ForumFilter): List<Post> {
        return when (filter) {
            ForumFilter.ALL -> threads
            ForumFilter.QUESTION -> threads.filter {
                it.title.startsWith("[Tanya]", ignoreCase = true) || it.title.contains("tanya", ignoreCase = true)
            }
            ForumFilter.SUGGESTION -> threads.filter {
                it.title.startsWith("[Saran]", ignoreCase = true) || it.title.contains("saran", ignoreCase = true)
            }
        }
    }
}
