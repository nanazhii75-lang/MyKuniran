package com.kuniran.core.model

data class FinanceCategory(
    val id: String,
    val rtId: String,
    val name: String,
    val description: String?,
    val bendaharaId: String?,
    val isArchived: Boolean,
    val createdAt: String,
    val updatedAt: String
)
