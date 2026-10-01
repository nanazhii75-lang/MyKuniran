package com.kuniran.core.model

data class FinanceRecord(
    val id: String,
    val rtId: String,
    val categoryId: String?,
    val categoryName: String,
    val title: String,
    val description: String?,
    val type: TransactionType,
    val amount: Long,
    val balanceAfter: Long?,
    val proofPath: String?,
    val contributorName: String?,
    val recordedBy: String,
    val transactionDate: String,
    val isVerified: Boolean = true,
    val isLocked: Boolean = false,
    val createdAt: String,
    val updatedAt: String
)
