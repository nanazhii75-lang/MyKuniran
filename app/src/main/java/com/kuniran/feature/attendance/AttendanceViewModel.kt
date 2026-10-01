package com.kuniran.feature.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuniran.core.common.Resource
import com.kuniran.domain.usecase.GetCurrentUserUseCase
import com.kuniran.domain.usecase.GetAttendanceHistoryUseCase
import com.kuniran.domain.usecase.LogAttendanceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AttendanceViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logAttendanceUseCase: LogAttendanceUseCase,
    private val getAttendanceHistoryUseCase: GetAttendanceHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private var currentRtId: String? = null
    private var currentUserId: String = ""
    private var currentUserName: String = "Warga RT"

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                if (user != null) {
                    currentRtId = user.rtId
                    currentUserId = user.id
                    currentUserName = user.fullName
                    loadHistory(user.id)
                }
            }
        }
    }

    fun loadHistory(wargaId: String = currentUserId) {
        viewModelScope.launch {
            getAttendanceHistoryUseCase(wargaId).collect { res ->
                when (res) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update { it.copy(isLoading = false, history = res.data) }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
            }
        }
    }

    fun onQrCodeScanned(qrContent: String) {
        val rtId = currentRtId ?: return
        val raw = qrContent.trim()
        if (raw.isBlank() || _uiState.value.lastScannedEvent == raw) return

        // Parse event title & location if formatted as JSON or "title|location"
        val (eventTitle, eventLocation) = parseQrPayload(raw)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, lastScannedEvent = raw) }
            val result = logAttendanceUseCase(
                rtId = rtId,
                wargaId = currentUserId,
                residentName = currentUserName,
                eventTitle = eventTitle,
                eventLocation = eventLocation
            )

            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = eventTitle
                        )
                    }
                    loadHistory(currentUserId)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun logManualAttendance(title: String, location: String?) {
        val rtId = currentRtId ?: return
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showManualDialog = false) }
            val result = logAttendanceUseCase(
                rtId = rtId,
                wargaId = currentUserId,
                residentName = currentUserName,
                eventTitle = cleanTitle,
                eventLocation = location?.trim()?.ifBlank { null }
            )

            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = cleanTitle) }
                    loadHistory(currentUserId)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun setShowManualDialog(show: Boolean) {
        _uiState.update { it.copy(showManualDialog = show) }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun parseQrPayload(raw: String): Pair<String, String?> {
        return try {
            if (raw.startsWith("{") && raw.contains("title")) {
                val titleRegex = """"title"\s*:\s*"([^"]+)"""".toRegex()
                val locRegex = """"location"\s*:\s*"([^"]+)"""".toRegex()
                val title = titleRegex.find(raw)?.groupValues?.getOrNull(1) ?: raw
                val loc = locRegex.find(raw)?.groupValues?.getOrNull(1)
                Pair(title, loc)
            } else if (raw.contains("|")) {
                val parts = raw.split("|")
                Pair(parts[0].trim(), parts.getOrNull(1)?.trim())
            } else {
                Pair(raw, null)
            }
        } catch (_: Exception) {
            Pair(raw, null)
        }
    }
}
