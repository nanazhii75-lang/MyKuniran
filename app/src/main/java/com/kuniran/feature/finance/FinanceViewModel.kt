package com.kuniran.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuniran.core.common.Resource
import com.kuniran.core.model.FinanceCategory
import com.kuniran.core.model.TransactionType
import com.kuniran.domain.repository.FinanceRepository
import com.kuniran.domain.usecase.AssignBendaharaUseCase
import com.kuniran.domain.usecase.CreateFinanceAgendaUseCase
import com.kuniran.domain.usecase.CreateFinanceCategoryUseCase
import com.kuniran.domain.usecase.CreateTransactionUseCase
import com.kuniran.domain.usecase.DeleteTransactionUseCase
import com.kuniran.domain.usecase.GetCurrentUserUseCase
import com.kuniran.domain.usecase.GetFinanceCategoriesUseCase
import com.kuniran.domain.usecase.GetRtMembersUseCase
import com.kuniran.domain.usecase.GetTransactionsUseCase
import com.kuniran.domain.usecase.PublishFinanceReportUseCase
import com.kuniran.domain.usecase.PublishMonthlyRecapUseCase
import com.kuniran.domain.usecase.SyncFinancesUseCase
import com.kuniran.domain.usecase.SyncMembersUseCase
import com.kuniran.domain.usecase.UpdateFinanceCategoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FinanceViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getFinanceCategoriesUseCase: GetFinanceCategoriesUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val syncFinancesUseCase: SyncFinancesUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val publishFinanceReportUseCase: PublishFinanceReportUseCase,
    private val publishMonthlyRecapUseCase: PublishMonthlyRecapUseCase,
    private val createFinanceAgendaUseCase: CreateFinanceAgendaUseCase,
    private val createFinanceCategoryUseCase: CreateFinanceCategoryUseCase,
    private val updateFinanceCategoryUseCase: UpdateFinanceCategoryUseCase,
    private val financeRepository: FinanceRepository,
    private val getRtMembersUseCase: GetRtMembersUseCase,
    private val syncMembersUseCase: SyncMembersUseCase,
    private val assignBendaharaUseCase: AssignBendaharaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getRtMembersUseCase().collect { list ->
                _uiState.update { it.copy(members = list.filter { m -> m.isMember }) }
            }
        }
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _uiState.update { it.copy(currentUser = user) }
                if (user?.rtId != null) {
                    observeCategories(user.rtId)
                    observeAllTransactions(user.rtId)
                    refreshFinances(user.rtId)
                }
            }
        }
    }

    private fun observeCategories(rtId: String) {
        viewModelScope.launch {
            getFinanceCategoriesUseCase(rtId).collect { cats ->
                _uiState.update { current ->
                    val selected = current.selectedCategory ?: cats.firstOrNull()
                    val active = if (current.activeLedgerCategory != null) {
                        cats.find { it.id == current.activeLedgerCategory.id } ?: current.activeLedgerCategory
                    } else null
                    current.copy(categories = cats, selectedCategory = selected, activeLedgerCategory = active)
                }
                _uiState.value.selectedCategory?.let { observeTransactions(it.id) }
            }
        }
    }

    private fun observeAllTransactions(rtId: String) {
        viewModelScope.launch {
            financeRepository.getAllRtTransactionsFlow(rtId).collect { allTxs ->
                var totalInc = 0L
                var totalExp = 0L
                allTxs.forEach { t ->
                    if (t.type == TransactionType.MASUK) totalInc += t.amount
                    else totalExp += t.amount
                }
                _uiState.update { current ->
                    current.copy(
                        allTransactions = allTxs,
                        totalIncome = totalInc,
                        totalExpense = totalExp,
                        balance = totalInc - totalExp
                    )
                }
            }
        }
    }

    fun openCategoryLedger(category: FinanceCategory) {
        _uiState.update { it.copy(activeLedgerCategory = category, selectedCategory = category, searchQuery = "", filterType = "ALL") }
        observeTransactions(category.id)
    }

    fun closeCategoryLedger() {
        _uiState.update { it.copy(activeLedgerCategory = null, searchQuery = "", filterType = "ALL") }
    }

    fun selectCategory(cat: FinanceCategory) {
        _uiState.update { it.copy(selectedCategory = cat) }
        observeTransactions(cat.id)
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilterType(type: String) {
        _uiState.update { it.copy(filterType = type) }
    }

    private fun observeTransactions(categoryId: String) {
        viewModelScope.launch {
            getTransactionsUseCase(categoryId).collect { txs ->
                var inc = 0L
                var exp = 0L
                txs.forEach { t ->
                    if (t.type == TransactionType.MASUK) inc += t.amount
                    else exp += t.amount
                }
                _uiState.update {
                    it.copy(
                        transactions = txs,
                        ledgerIncome = inc,
                        ledgerExpense = exp,
                        ledgerBalance = inc - exp
                    )
                }
            }
        }
    }

    fun refreshFinances(rtId: String? = _uiState.value.currentUser?.rtId) {
        val target = rtId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val res = syncFinancesUseCase(target)
            // Daftar warga dipakai untuk memilih bendahara; kegagalannya tidak mengganggu layar keuangan
            syncMembersUseCase()
            when (res) {
                is Resource.Error -> _uiState.update { it.copy(isRefreshing = false, error = res.error) }
                else -> _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun createTransaction(
        title: String,
        amount: Long,
        type: TransactionType,
        contributorName: String?,
        note: String?,
        transactionDate: String,
        onSuccess: () -> Unit
    ) {
        val user = _uiState.value.currentUser ?: return
        val rtId = user.rtId ?: return
        val catId = _uiState.value.activeLedgerCategory?.id ?: _uiState.value.selectedCategory?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = createTransactionUseCase(
                rtId = rtId,
                categoryId = catId,
                title = title,
                contributorName = contributorName,
                note = note,
                amount = amount,
                type = type,
                proofPath = null,
                correctsId = null,
                createdBy = user.id,
                transactionDate = transactionDate
            )) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    financeRepository.syncTransactions(rtId)
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            when (val res = deleteTransactionUseCase(id)) {
                is Resource.Error -> _uiState.update { it.copy(error = res.error) }
                else -> Unit
            }
        }
    }

    fun publishTransaction(financeId: String, note: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = publishFinanceReportUseCase(financeId, note)) {
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

    fun publishMonthlyRecap(monthDate: String, onSuccess: () -> Unit) {
        val catId = _uiState.value.activeLedgerCategory?.id ?: _uiState.value.selectedCategory?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = publishMonthlyRecapUseCase(catId, monthDate)) {
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

    fun createCategory(name: String, description: String?, onSuccess: () -> Unit) {
        val rtId = _uiState.value.currentUser?.rtId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = createFinanceCategoryUseCase(rtId, name, description)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    financeRepository.syncCategories(rtId)
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun updateCategory(categoryId: String, name: String, description: String?, onSuccess: () -> Unit) {
        val rtId = _uiState.value.currentUser?.rtId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = updateFinanceCategoryUseCase(categoryId, name, description)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    financeRepository.syncCategories(rtId)
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun assignBendahara(categoryId: String, profileId: String?, onSuccess: () -> Unit) {
        val rtId = _uiState.value.currentUser?.rtId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = assignBendaharaUseCase(categoryId, profileId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    financeRepository.syncCategories(rtId)
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.error) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
