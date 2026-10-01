package com.kuniran.feature.finance

import com.kuniran.core.common.AppError
import com.kuniran.core.model.FinanceCategory
import com.kuniran.core.model.FinanceTransaction
import com.kuniran.core.model.UserProfile

data class FinanceUiState(
    val categories: List<FinanceCategory> = emptyList(),
    val transactions: List<FinanceTransaction> = emptyList(),
    val allTransactions: List<FinanceTransaction> = emptyList(),
    val selectedCategory: FinanceCategory? = null,
    val activeLedgerCategory: FinanceCategory? = null,
    val currentUser: UserProfile? = null,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val balance: Long = 0L,
    val ledgerIncome: Long = 0L,
    val ledgerExpense: Long = 0L,
    val ledgerBalance: Long = 0L,
    val searchQuery: String = "",
    val filterType: String = "ALL", // "ALL", "MASUK", "KELUAR"
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: AppError? = null
) {
    val isBendaharaOfSelected: Boolean
        get() {
            val user = currentUser ?: return false
            val cat = activeLedgerCategory ?: selectedCategory ?: return false
            return cat.bendaharaId == user.id || user.role == com.kuniran.core.model.UserRole.ADMIN_RT
        }

    val canManageAnyPos: Boolean
        get() {
            val user = currentUser ?: return false
            if (user.role == com.kuniran.core.model.UserRole.ADMIN_RT) return true
            return categories.any { it.bendaharaId == user.id }
        }

    fun getCategoryBalance(categoryId: String): Long {
        val catTxs = allTransactions.filter { it.categoryId == categoryId }
        val inc = catTxs.filter { it.type == com.kuniran.core.model.TransactionType.MASUK }.sumOf { it.amount }
        val exp = catTxs.filter { it.type == com.kuniran.core.model.TransactionType.KELUAR }.sumOf { it.amount }
        return inc - exp
    }

    fun getCategoryIncome(categoryId: String): Long {
        return allTransactions.filter { it.categoryId == categoryId && it.type == com.kuniran.core.model.TransactionType.MASUK }.sumOf { it.amount }
    }

    fun getCategoryExpense(categoryId: String): Long {
        return allTransactions.filter { it.categoryId == categoryId && it.type == com.kuniran.core.model.TransactionType.KELUAR }.sumOf { it.amount }
    }
}
