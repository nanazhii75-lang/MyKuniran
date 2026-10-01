package com.kuniran.core.model

data class Post(
    val id: String,
    val rtId: String,
    val authorId: String,
    val authorName: String = "",
    val authorAvatar: String? = null,
    val title: String,
    val content: String,
    val type: PostType,
    val eventDate: String?,
    val eventLocation: String?,
    val financeRefId: String?,
    val categoryRefId: String?,
    val recapMonth: String?,
    val meta: FinanceReportMeta?,
    val isPinned: Boolean,
    val pinnedUntil: String?,
    val deletedAt: String?,
    val createdAt: String,
    val updatedAt: String
)
