package com.kuniran.feature.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuniran.core.common.Resource
import com.kuniran.core.model.Warga
import com.kuniran.domain.usecase.GetCurrentUserUseCase
import com.kuniran.domain.usecase.GetWargaListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResidentDirectoryViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getWargaListUseCase: GetWargaListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResidentDirectoryUiState(isLoading = true))
    val uiState: StateFlow<ResidentDirectoryUiState> = _uiState.asStateFlow()

    private var currentRtId: String? = null

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                val rtId = user?.rtId
                if (rtId != null && rtId != currentRtId) {
                    currentRtId = rtId
                    fetchResidents(rtId)
                } else if (rtId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            residents = emptyList(),
                            filteredResidents = emptyList(),
                            availableBlocks = emptyList()
                        )
                    }
                }
            }
        }
    }

    fun fetchResidents(rtId: String? = currentRtId) {
        val targetRtId = rtId ?: return
        viewModelScope.launch {
            getWargaListUseCase(targetRtId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        val wargaList = resource.data
                        val blocks = extractAvailableBlocks(wargaList)
                        _uiState.update { state ->
                            val filtered = filterResidents(wargaList, state.searchQuery, state.selectedBlock)
                            state.copy(
                                isLoading = false,
                                residents = wargaList,
                                availableBlocks = blocks,
                                filteredResidents = filtered,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = resource.error) }
                    }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = filterResidents(state.residents, query, state.selectedBlock)
            state.copy(searchQuery = query, filteredResidents = filtered)
        }
    }

    fun selectBlock(block: String?) {
        _uiState.update { state ->
            val newBlock = if (state.selectedBlock == block) null else block
            val filtered = filterResidents(state.residents, state.searchQuery, newBlock)
            state.copy(selectedBlock = newBlock, filteredResidents = filtered)
        }
    }

    fun refresh() {
        currentRtId?.let { fetchResidents(it) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun extractAvailableBlocks(residents: List<Warga>): List<String> {
        return residents.mapNotNull { warga ->
            val block = warga.houseBlock?.trim()
            if (!block.isNullOrBlank()) {
                block
            } else {
                extractBlockFromInfo(warga.houseInfo)
            }
        }.distinct().sorted()
    }

    private fun extractBlockFromInfo(info: String?): String? {
        if (info.isNullOrBlank()) return null
        val regex = """[Bb]lok\s*([A-Za-z0-9]+)""".toRegex()
        return regex.find(info)?.groupValues?.getOrNull(1)
    }

    private fun filterResidents(
        residents: List<Warga>,
        query: String,
        block: String?
    ): List<Warga> {
        val q = query.trim().lowercase()

        return residents.filter { warga ->
            // Block filter
            val matchesBlock = if (block.isNullOrBlank()) {
                true
            } else {
                warga.houseBlock.equals(block, ignoreCase = true) ||
                warga.houseInfo?.contains("blok $block", ignoreCase = true) == true
            }

            // Search query (strictly privacy-safe: full_name, phone_number, house_info, house_number)
            val matchesQuery = if (q.isEmpty()) {
                true
            } else {
                warga.fullName.lowercase().contains(q) ||
                (warga.phoneNumber?.contains(q) == true) ||
                (warga.houseNumber?.lowercase()?.contains(q) == true) ||
                (warga.houseInfo?.lowercase()?.contains(q) == true) ||
                (warga.houseBlock?.lowercase()?.contains(q) == true)
            }

            matchesBlock && matchesQuery
        }
    }
}
