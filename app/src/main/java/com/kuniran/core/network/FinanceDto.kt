package com.kuniran.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FinanceDto(
    @Json(name = "id") val id: String,
    @Json(name = "rt_id") val rtId: String,
    @Json(name = "category_id") val categoryId: String,
    @Json(name = "title") val title: String,
    @Json(name = "contributor_name") val contributorName: String? = null,
    @Json(name = "note") val note: String? = null,
    @Json(name = "amount") val amount: Long,
    @Json(name = "type") val type: String,
    @Json(name = "proof_path") val proofPath: String? = null,
    @Json(name = "corrects_id") val correctsId: String? = null,
    @Json(name = "created_by") val createdBy: String,
    @Json(name = "transaction_date") val transactionDate: String,
    @Json(name = "deleted_at") val deletedAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)
