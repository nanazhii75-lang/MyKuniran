package com.kuniran.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuniran.core.model.FinanceTransaction
import com.kuniran.core.model.TransactionType

@Entity(tableName = "finances")
data class FinanceTransactionEntity(
    @PrimaryKey val id: String,
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
) {
    fun toDomain(): FinanceTransaction = FinanceTransaction(
        id = id,
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
        transactionDate = transactionDate,
        isLocked = isLocked,
        deletedAt = deletedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(f: FinanceTransaction): FinanceTransactionEntity = FinanceTransactionEntity(
            id = f.id,
            rtId = f.rtId,
            categoryId = f.categoryId,
            title = f.title,
            contributorName = f.contributorName,
            note = f.note,
            amount = f.amount,
            type = f.type,
            proofPath = f.proofPath,
            correctsId = f.correctsId,
            createdBy = f.createdBy,
            transactionDate = f.transactionDate,
            isLocked = f.isLocked,
            deletedAt = f.deletedAt,
            createdAt = f.createdAt,
            updatedAt = f.updatedAt
        )
    }
}
