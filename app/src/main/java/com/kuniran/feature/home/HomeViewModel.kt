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
import com.kuniran.domain.usecase.GetRtMembersUseCase
import com.kuniran.domain.usecase.PinPostUseCase
import com.kuniran.domain.usecase.SyncFeedUseCase
import com.kuniran.domain.usecase.SyncMembersUseCase
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
    private val rtRepository: RtRepository,
    private val getRtMembersUseCase: GetRtMembersUseCase,
    private val syncMembersUseCase: SyncMembersUseCase
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
        // Nama penulis diambil dari daftar orang di RT (sudah dibatasi server per RT)
        viewModelScope.launch {
            getRtMembersUseCase().collect { members ->
                _uiState.update { state ->
                    state.copy(authorNames = members.associate { it.id to it.fullName })
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

    /**
     * @param silent true untuk penyegaran otomatis: tanpa indikator dan tanpa pesan galat
     * (misalnya saat sinyal lemah), supaya warga tidak terganggu.
     */
    fun refreshFeed(rtId: String? = _uiState.value.currentUser?.rtId, silent: Boolean = false) {
        val targetRt = rtId ?: return
        viewModelScope.launch {
            if (!silent) _uiState.update { it.copy(isRefreshing = true) }
            val res = syncFeedUseCase(targetRt)

            // Muat ulang daftar orang hanya bila ada penulis yang namanya belum dikenal
            val state = _uiState.value
            val hasUnknownAuthor = state.posts.any { it.authorId !in state.authorNames }
            if (!silent || hasUnknownAuthor) syncMembersUseCase()

            when {
                res is Resource.Error && !silent ->
                    _uiState.update { it.copy(isRefreshing = false, error = res.error) }
                else -> _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Kirim tulisan dari kolom tulis di home. Judul dibentuk dari baris pertama
     * (database mewajibkan judul), isi menyimpan seluruh teks.
     */
    fun sendPost(text: String, asAnnouncement: Boolean, onSuccess: () -> Unit) {
        if (_uiState.value.isLoading) return
        val content = text.trim().takeSafe(CONTENT_MAX)
        if (content.isEmpty()) return
        val title = content.lineSequence().first().takeSafe(TITLE_MAX).trim()
        createPost(
            title = title,
            content = content,
            type = if (asAnnouncement) PostType.PENGUMUMAN else PostType.DISKUSI,
            eventDate = null,
            eventLocation = null,
            onSuccess = {
                onSuccess()
                refreshFeed()
            }
        )
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
                else -> refreshFeed(silent = true)
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private companion object {
        const val TITLE_MAX = 100     // batas database 150
        const val CONTENT_MAX = 5000  // batas database 5000
    }
}

/** Potong tanpa memenggal pasangan surrogate (emoji) di tengah. */
private fun String.takeSafe(max: Int): String {
    if (length <= max) return this
    val cut = if (this[max - 1].isHighSurrogate()) max - 1 else max
    return substring(0, cut)
}
