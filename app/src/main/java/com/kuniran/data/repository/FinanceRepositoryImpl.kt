package com.kuniran.data.repository

import com.kuniran.core.common.AppError
import com.kuniran.core.common.DateTimeUtils
import com.kuniran.core.common.ExceptionMapper
import com.kuniran.core.common.Resource
import com.kuniran.core.database.CategoryDao
import com.kuniran.core.database.FinanceCategoryEntity
import com.kuniran.core.database.FinanceDao
import com.kuniran.core.database.FinanceTransactionEntity
import com.kuniran.core.model.FinanceCategory
import com.kuniran.core.model.FinanceTransaction
import com.kuniran.core.model.TransactionType
import com.kuniran.core.network.CreateFinanceAgendaRequest
import com.kuniran.core.network.CreateFinanceCategoryRequest
import com.kuniran.core.network.DeleteTransactionRequest
import com.kuniran.core.network.PublishFinanceReportRequest
import com.kuniran.core.network.PublishMonthlyRecapRequest
import com.kuniran.core.network.SupabaseApiService
import com.kuniran.core.network.UpdateFinanceCategoryRequest
import com.kuniran.core.network.toDomain
import com.kuniran.domain.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class FinanceRepositoryImpl(
    private val apiService: SupabaseApiService,
    private val financeDao: FinanceDao,
    private val categoryDao: CategoryDao
) : FinanceRepository {

    override fun getCategoriesFlow(rtId: String): Flow<List<FinanceCategory>> {
        return categoryDao.getCategoriesFlow(rtId).map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsFlow(categoryId: String): Flow<List<FinanceTransaction>> {
        return financeDao.getTransactionsFlow(categoryId).map { list -> list.map { it.toDomain() } }
    }

    override fun getAllRtTransactionsFlow(rtId: String): Flow<List<FinanceTransaction>> {
        return financeDao.getAllRtTransactionsFlow(rtId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun syncCategories(rtId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val list = apiService.getCategories("eq.$rtId")
            categoryDao.insertCategories(list.map { FinanceCategoryEntity.fromDomain(it.toDomain()) })
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun syncTransactions(rtId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val list = apiService.getTransactions("eq.$rtId")
            financeDao.insertTransactions(list.map { FinanceTransactionEntity.fromDomain(it.toDomain()) })
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    private fun mapFailedResponse(code: Int, body: String?): AppError {
        val text = body.orEmpty()
        return when {
            code == 401 -> AppError.SessionExpired
            code == 403 || text.contains("42501") -> AppError.NotAllowed
            code in 500..599 -> AppError.Network
            else -> ExceptionMapper.mapErrorCodeString(text)
        }
    }

    // Server dulu: data lokal hanya berubah setelah server menerima. Tidak ada pos hantu.
    override suspend fun createCategory(rtId: String, name: String, description: String?): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createFinanceCategory(
                CreateFinanceCategoryRequest(name = name, description = description)
            )
            if (!response.isSuccessful) {
                return@withContext Resource.Error(
                    mapFailedResponse(response.code(), runCatching { response.errorBody()?.string() }.getOrNull())
                )
            }
            syncCategories(rtId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun updateCategory(
        categoryId: String,
        name: String,
        description: String?,
        isArchived: Boolean?
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateFinanceCategory(
                UpdateFinanceCategoryRequest(
                    categoryId = categoryId,
                    name = name,
                    description = description,
                    isArchived = isArchived
                )
            )
            if (!response.isSuccessful) {
                return@withContext Resource.Error(
                    mapFailedResponse(response.code(), runCatching { response.errorBody()?.string() }.getOrNull())
                )
            }
            categoryDao.updateCategoryInfo(categoryId, name, description)
            if (isArchived != null) categoryDao.updateArchived(categoryId, isArchived)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun createTransaction(
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
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        val newId = UUID.randomUUID().toString()
        val now = DateTimeUtils.currentIsoTimestamp()

        val tx = FinanceTransaction(
            id = newId,
            rtId = rtId,
            categoryId = categoryId,
            title = title,
            contributorName = contributorName,
            note = note,
            amount = amount,
            type = type,
            proofPath = proofPath,
            correctsId = correctsId,
            createdBy = createdBy,
            transactionDate = transactionDate.ifBlank { now },
            isLocked = false,
            deletedAt = null,
            createdAt = now,
            updatedAt = now
        )

        // Save immediately to Room
        financeDao.insertTransaction(FinanceTransactionEntity.fromDomain(tx))

        try {
            val body = mutableMapOf<String, Any?>(
                "id" to newId,
                "rt_id" to rtId,
                "category_id" to categoryId,
                "title" to title,
                "contributor_name" to contributorName,
                "note" to note,
                "amount" to amount,
                "type" to type.name,
                "proof_path" to proofPath,
                "corrects_id" to correctsId,
                "created_by" to createdBy,
                "transaction_date" to tx.transactionDate
            )
            apiService.createTransaction(body)
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e.message?.contains("23505") == true) {
                Resource.Success(Unit)
            } else {
                Resource.Error(ExceptionMapper.map(e))
            }
        }
    }

    override suspend fun deleteTransaction(id: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.deleteTransaction(DeleteTransactionRequest(id))
            financeDao.softDelete(id, DateTimeUtils.currentIsoTimestamp())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun publishFinanceReport(financeId: String, note: String?): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val postId = apiService.publishFinanceReport(PublishFinanceReportRequest(financeId, note))
            financeDao.updateLockStatus(financeId, true)
            Resource.Success(postId)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun createFinanceAgenda(
        categoryId: String,
        title: String,
        eventDate: String,
        location: String
    ): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val req = CreateFinanceAgendaRequest(categoryId, title, eventDate, location)
            val postId = apiService.createFinanceAgenda(req)
            Resource.Success(postId)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun publishMonthlyRecap(categoryId: String, monthDate: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val req = PublishMonthlyRecapRequest(categoryId, monthDate)
            val postId = apiService.publishMonthlyRecap(req)
            Resource.Success(postId)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }
}
