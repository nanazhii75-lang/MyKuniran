package com.kuniran.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuniran.core.common.Resource
import com.kuniran.core.model.FinanceRecord
import com.kuniran.core.model.MonthlyTrendDataPoint
import com.kuniran.core.model.TransactionType
import com.kuniran.domain.usecase.GetCurrentUserUseCase
import com.kuniran.domain.usecase.GetFinanceRecordsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FinanceRecordViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getFinanceRecordsUseCase: GetFinanceRecordsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceRecordUiState(isLoading = true))
    val uiState: StateFlow<FinanceRecordUiState> = _uiState.asStateFlow()

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
                    fetchFinanceRecords(rtId)
                } else if (rtId == null) {
                    _uiState.update { it.copy(isLoading = false, records = emptyList(), filteredRecords = emptyList()) }
                }
            }
        }
    }

    fun fetchFinanceRecords(rtId: String? = currentRtId) {
        val targetRtId = rtId ?: return
        viewModelScope.launch {
            getFinanceRecordsUseCase(targetRtId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        val records = resource.data
                        val income = records.filter { it.type == TransactionType.MASUK }.sumOf { it.amount }
                        val expense = records.filter { it.type == TransactionType.KELUAR }.sumOf { it.amount }
                        val balance = income - expense
                        val trends = calculateMonthlyTrends(records)
                        val categorized = records.groupBy { it.categoryName }

                        _uiState.update { state ->
                            val filtered = filterRecords(records, state.filterType, state.searchQuery)
                            state.copy(
                                isLoading = false,
                                records = records,
                                filteredRecords = filtered,
                                monthlyTrends = trends,
                                categorizedRecords = categorized,
                                totalIncome = income,
                                totalExpense = expense,
                                netBalance = balance,
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

    fun setFilterType(type: TransactionType?) {
        _uiState.update { state ->
            val filtered = filterRecords(state.records, type, state.searchQuery)
            state.copy(filterType = type, filteredRecords = filtered)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = filterRecords(state.records, state.filterType, query)
            state.copy(searchQuery = query, filteredRecords = filtered)
        }
    }

    fun refresh() {
        currentRtId?.let { fetchFinanceRecords(it) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun filterRecords(
        records: List<FinanceRecord>,
        filterType: TransactionType?,
        query: String
    ): List<FinanceRecord> {
        val q = query.trim().lowercase()
        return records.filter { item ->
            val matchesType = filterType == null || item.type == filterType
            val matchesQuery = q.isEmpty() ||
                item.title.lowercase().contains(q) ||
                item.categoryName.lowercase().contains(q) ||
                (item.description?.lowercase()?.contains(q) == true) ||
                (item.contributorName?.lowercase()?.contains(q) == true)
            matchesType && matchesQuery
        }
    }

    private fun calculateMonthlyTrends(records: List<FinanceRecord>): List<MonthlyTrendDataPoint> {
        val monthNames = mapOf(
            "01" to "Jan", "02" to "Feb", "03" to "Mar", "04" to "Apr",
            "05" to "Mei", "06" to "Jun", "07" to "Jul", "08" to "Agu",
            "09" to "Sep", "10" to "Okt", "11" to "Nov", "12" to "Des"
        )
        val groupedByMonth = records.groupBy { record ->
            // extract YYYY-MM
            val raw = record.transactionDate
            if (raw.length >= 7 && raw[4] == '-') raw.take(7) else "Lainnya"
        }

        return groupedByMonth.entries
            .filter { it.key != "Lainnya" }
            .sortedBy { it.key }
            .takeLast(6)
            .map { (key, list) ->
                val monthPart = key.split("-").getOrNull(1) ?: key
                val label = monthNames[monthPart] ?: monthPart
                val inc = list.filter { it.type == TransactionType.MASUK }.sumOf { it.amount }
                val exp = list.filter { it.type == TransactionType.KELUAR }.sumOf { it.amount }
                MonthlyTrendDataPoint(
                    monthKey = key,
                    monthLabel = label,
                    income = inc,
                    expense = exp,
                    net = inc - exp
                )
            }
    }
}
