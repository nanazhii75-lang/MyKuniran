package com.kuniran.domain.repository

import com.kuniran.core.common.Resource
import com.kuniran.core.model.FinanceCategory
import com.kuniran.core.model.FinanceTransaction
import com.kuniran.core.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    fun getCategoriesFlow(rtId: String): Flow<List<FinanceCategory>>
    fun getTransactionsFlow(categoryId: String): Flow<List<FinanceTransaction>>
    fun getAllRtTransactionsFlow(rtId: String): Flow<List<FinanceTransaction>>
    suspend fun syncCategories(rtId: String): Resource<Unit>
    suspend fun syncTransactions(rtId: String): Resource<Unit>
    suspend fun createCategory(rtId: String, name: String, description: String?): Resource<Unit>
    suspend fun updateCategory(categoryId: String, name: String, description: String?, isArchived: Boolean?): Resource<Unit>
    suspend fun createTransaction(
        rtId: String,
        categoryId: String,
        title: String,
        contributorName: String?,
        note: String?,
        amount: Long,
        type: TransactionType,
        proofPath: String?,
        correctsId: String?,
        createdBy: String,
        transactionDate: String
    ): Resource<Unit>
    suspend fun deleteTransaction(id: String): Resource<Unit>
    suspend fun publishFinanceReport(financeId: String, note: String?): Resource<String>
    suspend fun createFinanceAgenda(categoryId: String, title: String, eventDate: String, location: String): Resource<String>
    suspend fun publishMonthlyRecap(categoryId: String, monthDate: String): Resource<String>
}
