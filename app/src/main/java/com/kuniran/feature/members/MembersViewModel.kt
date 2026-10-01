package com.kuniran.feature.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuniran.core.common.Resource
import com.kuniran.core.model.UserRole
import com.kuniran.domain.usecase.ApproveJoinRequestUseCase
import com.kuniran.domain.usecase.GetCurrentUserUseCase
import com.kuniran.domain.usecase.GetRtMembersUseCase
import com.kuniran.domain.usecase.ListPendingJoinRequestsUseCase
import com.kuniran.domain.usecase.RejectJoinRequestUseCase
import com.kuniran.domain.usecase.RemoveMemberUseCase
import com.kuniran.domain.usecase.SyncMembersUseCase
import com.kuniran.domain.usecase.TransferAdminUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MembersViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getRtMembersUseCase: GetRtMembersUseCase,
    private val syncMembersUseCase: SyncMembersUseCase,
    private val listPendingJoinRequestsUseCase: ListPendingJoinRequestsUseCase,
    private val approveJoinRequestUseCase: ApproveJoinRequestUseCase,
    private val rejectJoinRequestUseCase: RejectJoinRequestUseCase,
    private val transferAdminUseCase: TransferAdminUseCase,
    private val removeMemberUseCase: RemoveMemberUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MembersUiState())
    val uiState: StateFlow<MembersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _uiState.update { it.copy(currentUser = user) }
                if (user?.role == UserRole.ADMIN_RT) {
                    loadPendingRequests()
                }
            }
        }

        viewModelScope.launch {
            getRtMembersUseCase().collect { members ->
                _uiState.update { it.copy(members = members) }
            }
        }

        syncMembers()
    }

    fun syncMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val res = syncMembersUseCase()) {
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = res.error) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadPendingRequests() {
        viewModelScope.launch {
            when (val res = listPendingJoinRequestsUseCase()) {
                is Resource.Success -> _uiState.update { it.copy(pendingRequests = res.data) }
                is Resource.Error -> _uiState.update { it.copy(error = res.error) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            when (val res = approveJoinRequestUseCase(requestId)) {
                is Resource.Success -> {
                    loadPendingRequests()
                    syncMembers()
                }
                is Resource.Error -> _uiState.update { it.copy(error = res.error) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            when (val res = rejectJoinRequestUseCase(requestId)) {
                is Resource.Success -> {
                    loadPendingRequests()
                }
                is Resource.Error -> _uiState.update { it.copy(error = res.error) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun transferAdmin(newAdminId: String) {
        viewModelScope.launch {
            when (val res = transferAdminUseCase(newAdminId)) {
                is Resource.Success -> syncMembers()
                is Resource.Error -> _uiState.update { it.copy(error = res.error) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun removeMember(profileId: String) {
        viewModelScope.launch {
            when (val res = removeMemberUseCase(profileId)) {
                is Resource.Success -> syncMembers()
                is Resource.Error -> _uiState.update { it.copy(error = res.error) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
