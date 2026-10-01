package com.kuniran.data.repository

import com.kuniran.core.common.AppError
import com.kuniran.core.common.Resource
import com.kuniran.core.model.FinanceRecord
import com.kuniran.core.network.SupabaseClient
import com.kuniran.domain.repository.FinanceRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class FinanceRecordRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : FinanceRecordRepository {

    private val apiService get() = supabaseClient.apiService

    override fun getFinanceRecords(rtId: String): Flow<Resource<List<FinanceRecord>>> = flow {
        emit(Resource.Loading)
        val dtoList = apiService.getFinanceRecords(rtIdFilter = "eq.$rtId")
        val domainList = dtoList.map { it.toDomain() }
        emit(Resource.Success(domainList))
    }.catch { e ->
        emit(Resource.Error(AppError.Unknown(e.localizedMessage)))
    }.flowOn(Dispatchers.IO)

    override suspend fun createFinanceRecord(record: FinanceRecord): Resource<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = mutableMapOf<String, Any?>(
                "rt_id" to record.rtId,
                "category_name" to record.categoryName,
                "title" to record.title,
                "type" to record.type.name,
                "amount" to record.amount,
                "recorded_by" to record.recordedBy,
                "transaction_date" to record.transactionDate,
                "is_verified" to record.isVerified,
                "is_locked" to record.isLocked
            )
            record.categoryId?.let { body["category_id"] = it }
            record.description?.let { body["description"] = it }
            record.balanceAfter?.let { body["balance_after"] = it }
            record.proofPath?.let { body["proof_path"] = it }
            record.contributorName?.let { body["contributor_name"] = it }

            val response = apiService.insertFinanceRecord(body)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(AppError.Unknown("HTTP ${response.code()}"))
            }
        }.getOrElse { e ->
            Resource.Error(AppError.Unknown(e.localizedMessage))
        }
    }

    override suspend fun updateFinanceRecord(id: String, updates: Map<String, Any?>): Resource<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.updateFinanceRecord(
                idFilter = "eq.$id",
                body = updates
            )
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(AppError.Unknown("HTTP ${response.code()}"))
            }
        }.getOrElse { e ->
            Resource.Error(AppError.Unknown(e.localizedMessage))
        }
    }

    override suspend fun deleteFinanceRecord(id: String): Resource<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.deleteFinanceRecord(idFilter = "eq.$id")
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(AppError.Unknown("HTTP ${response.code()}"))
            }
        }.getOrElse { e ->
            Resource.Error(AppError.Unknown(e.localizedMessage))
        }
    }
}
