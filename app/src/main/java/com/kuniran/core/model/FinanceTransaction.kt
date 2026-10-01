package com.kuniran.core.model

data class FinanceTransaction(
    val id: String,
    val rtId: String,
    val categoryId: String,
    val title: String,
    val contributorName: String?,
    val note: String?,
    val amount: Long,
    val type: TransactionType,
    val proofPath: String?,
    val correctsId: String?,
    val createdBy: String,
    val transactionDate: String,
    val isLocked: Boolean = false,
    val deletedAt: String?,
    val createdAt: String,
    val updatedAt: String
)
