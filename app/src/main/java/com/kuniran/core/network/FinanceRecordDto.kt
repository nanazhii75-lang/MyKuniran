package com.kuniran.core.network

import com.kuniran.core.model.FinanceRecord
import com.kuniran.core.model.TransactionType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FinanceRecordDto(
    @Json(name = "id") val id: String,
    @Json(name = "rt_id") val rtId: String,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "category_name") val categoryName: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "type") val type: String,
    @Json(name = "amount") val amount: Long,
    @Json(name = "balance_after") val balanceAfter: Long? = null,
    @Json(name = "proof_path") val proofPath: String? = null,
    @Json(name = "contributor_name") val contributorName: String? = null,
    @Json(name = "recorded_by") val recordedBy: String,
    @Json(name = "transaction_date") val transactionDate: String,
    @Json(name = "is_verified") val isVerified: Boolean = true,
    @Json(name = "is_locked") val isLocked: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    fun toDomain(): FinanceRecord = FinanceRecord(
        id = id,
        rtId = rtId,
        categoryId = categoryId,
        categoryName = categoryName,
        title = title,
        description = description,
        type = if (type.equals("MASUK", ignoreCase = true)) TransactionType.MASUK else TransactionType.KELUAR,
        amount = amount,
        balanceAfter = balanceAfter,
        proofPath = proofPath,
        contributorName = contributorName,
        recordedBy = recordedBy,
        transactionDate = transactionDate,
        isVerified = isVerified,
        isLocked = isLocked,
        createdAt = createdAt.orEmpty(),
        updatedAt = updatedAt.orEmpty()
    )
}
