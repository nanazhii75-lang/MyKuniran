package com.kuniran.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuniran.core.common.Resource
import com.kuniran.core.model.PostType
import com.kuniran.domain.repository.RtRepository
import com.kuniran.domain.usecase.CreatePostUseCase
import com.kuniran.domain.usecase.DeletePostUseCase
import com.kuniran.domain.usecase.GetCurrentUserUseCase
import com.kuniran.domain.usecase.GetRtFeedUseCase
import com.kuniran.domain.usecase.PinPostUseCase
import com.kuniran.domain.usecase.SyncFeedUseCase
import com.kuniran.domain.usecase.UnpinPostUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getRtFeedUseCase: GetRtFeedUseCase,
    private val syncFeedUseCase: SyncFeedUseCase,
    private val createPostUseCase: CreatePostUseCase,
    private val pinPostUseCase: PinPostUseCase,
    private val unpinPostUseCase: UnpinPostUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val rtRepository: RtRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _uiState.update { it.copy(currentUser = user) }
                if (user?.rtId != null) {
                    observeFeed(user.rtId)
                    refreshFeed(user.rtId)
                    fetchRtInfo(user.rtId)
                }
            }
        }
    }

    private fun observeFeed(rtId: String) {
        viewModelScope.launch {
            getRtFeedUseCase(rtId).collect { feed ->
                _uiState.update { it.copy(posts = feed) }
            }
        }
    }

    private fun fetchRtInfo(rtId: String) {
        viewModelScope.launch {
            rtRepository.getRtGroupFlow(rtId).collect { group ->
                _uiState.update { it.copy(rtGroup = group) }
            }
        }
    }

    fun refreshFeed(rtId: String? = _uiState.value.currentUser?.rtId) {
        val targetRt = rtId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            when (val res = syncFeedUseCase(targetRt)) {
                is Resource.Error -> _uiState.update { it.copy(isRefreshing = false, error = res.error) }
                else -> _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun createPost(
        title: String,
        content: String,
        type: PostType,
        eventDate: String?,
        eventLocation: String?,
        onSuccess: () -> Unit
    ) {
        val user = _uiState.value.currentUser ?: return
        val rtId = user.rtId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = createPostUseCase(
                rtId = rtId,
                authorId = user.id,
                title = title,
                content = content,
                type = type,
                eventDate = eventDate,
                eventLocation = eventLocation
            )) {
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

    fun pinPost(postId: String) {
        viewModelScope.launch {
            when (val res = pinPostUseCase(postId)) {
                is Resource.Error -> _uiState.update { it.copy(error = res.error) }
                else -> Unit
            }
        }
    }

    fun unpinPost(postId: String) {
        viewModelScope.launch {
            when (val res = unpinPostUseCase(postId)) {
                is Resource.Error -> _uiState.update { it.copy(error = res.error) }
                else -> Unit
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            when (val res = deletePostUseCase(postId)) {
                is Resource.Error -> _uiState.update { it.copy(error = res.error) }
                else -> Unit
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
