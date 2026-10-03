package com.kuniran.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostMetaDto(
    @Json(name = "kind") val kind: String? = null,
    @Json(name = "category_name") val categoryName: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "amount") val amount: Long? = null,
    @Json(name = "contributor_name") val contributorName: String? = null,
    @Json(name = "transaction_date") val transactionDate: String? = null,
    @Json(name = "masuk") val masuk: Long? = null,
    @Json(name = "keluar") val keluar: Long? = null,
    @Json(name = "saldo_awal") val saldoAwal: Long? = null,
    @Json(name = "saldo_akhir") val saldoAkhir: Long? = null
)

@JsonClass(generateAdapter = true)
data class PostDto(
    @Json(name = "id") val id: String,
    @Json(name = "rt_id") val rtId: String,
    @Json(name = "author_id") val authorId: String,
    @Json(name = "title") val title: String,
    @Json(name = "content") val content: String,
    @Json(name = "type") val type: String,
    @Json(name = "event_date") val eventDate: String? = null,
    @Json(name = "event_location") val eventLocation: String? = null,
    @Json(name = "finance_ref_id") val financeRefId: String? = null,
    @Json(name = "category_ref_id") val categoryRefId: String? = null,
    @Json(name = "recap_month") val recapMonth: String? = null,
    @Json(name = "meta") val meta: PostMetaDto? = null,
    @Json(name = "is_pinned") val isPinned: Boolean? = false,
    @Json(name = "pinned_until") val pinnedUntil: String? = null,
    @Json(name = "deleted_at") val deletedAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "image_path") val imagePath: String? = null
)
