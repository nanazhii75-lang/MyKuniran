package com.kuniran.feature.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuniran.core.common.Resource
import com.kuniran.core.model.AgendaRsvpSummary
import com.kuniran.core.model.Post
import com.kuniran.core.model.PostType
import com.kuniran.core.model.RsvpStatus
import com.kuniran.domain.usecase.GetAgendaRsvpsUseCase
import com.kuniran.domain.usecase.GetCurrentUserUseCase
import com.kuniran.domain.usecase.GetRtFeedUseCase
import com.kuniran.domain.usecase.SubmitRsvpUseCase
import com.kuniran.domain.usecase.SyncFeedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CalendarRsvpViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getRtFeedUseCase: GetRtFeedUseCase,
    private val getAgendaRsvpsUseCase: GetAgendaRsvpsUseCase,
    private val submitRsvpUseCase: SubmitRsvpUseCase,
    private val syncFeedUseCase: SyncFeedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarRsvpUiState(isLoading = true))
    val uiState: StateFlow<CalendarRsvpUiState> = _uiState.asStateFlow()

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
                        observeAgendasAndRsvps(rtId, user.id)
                    }
                }
            }
        }
    }

    private fun observeAgendasAndRsvps(rtId: String, userId: String) {
        viewModelScope.launch {
            getRtFeedUseCase(rtId).collect { posts ->
                val agendaPosts = posts.filter { it.type == PostType.AGENDA }
                _uiState.update { state ->
                    val filtered = filterByDate(agendaPosts, state.selectedDate)
                    state.copy(
                        isLoading = false,
                        agendas = agendaPosts,
                        filteredAgendas = filtered
                    )
                }
            }
        }

        viewModelScope.launch {
            getAgendaRsvpsUseCase(rtId).collect { rsvps ->
                val summaries = mutableMapOf<String, AgendaRsvpSummary>()
                val grouped = rsvps.groupBy { it.postId }

                _uiState.value.agendas.forEach { agenda ->
                    val list = grouped[agenda.id].orEmpty()
                    val hadir = list.count { it.status == RsvpStatus.HADIR }
                    val tidakHadir = list.count { it.status == RsvpStatus.TIDAK_HADIR }
                    val ragu = list.count { it.status == RsvpStatus.RAGU }
                    val userStatus = list.find { it.userId == userId }?.status

                    summaries[agenda.id] = AgendaRsvpSummary(
                        hadirCount = hadir,
                        tidakHadirCount = tidakHadir,
                        raguCount = ragu,
                        userStatus = userStatus
                    )
                }

                _uiState.update { it.copy(rsvpSummaries = summaries) }
            }
        }
    }

    fun selectDate(date: String?) {
        _uiState.update { state ->
            val newSelected = if (state.selectedDate == date) null else date
            val filtered = filterByDate(state.agendas, newSelected)
            state.copy(selectedDate = newSelected, filteredAgendas = filtered)
        }
    }

    fun submitRsvp(postId: String, status: RsvpStatus) {
        val rtId = currentRtId ?: return
        if (currentUserId.isBlank()) return

        viewModelScope.launch {
            val result = submitRsvpUseCase(
                postId = postId,
                userId = currentUserId,
                rtId = rtId,
                status = status
            )

            when (result) {
                is Resource.Success -> {
                    // Update immediate local state
                    val currentSummaries = _uiState.value.rsvpSummaries.toMutableMap()
                    val prev = currentSummaries[postId] ?: AgendaRsvpSummary()
                    currentSummaries[postId] = prev.copy(userStatus = status)
                    _uiState.update {
                        it.copy(
                            rsvpSummaries = currentSummaries,
                            successMessage = "RSVP Disimpan"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.error) }
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

    fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun filterByDate(agendas: List<Post>, date: String?): List<Post> {
        if (date.isNullOrBlank()) return agendas
        return agendas.filter { post ->
            post.eventDate?.startsWith(date) == true
        }
    }
}
