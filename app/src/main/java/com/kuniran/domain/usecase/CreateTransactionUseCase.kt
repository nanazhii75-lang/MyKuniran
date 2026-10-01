package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.core.model.TransactionType
import com.kuniran.domain.repository.FinanceRepository

class CreateTransactionUseCase(
    private val financeRepository: FinanceRepository
) {
    suspend operator fun invoke(
        rtId: String,
        categoryId: String,
        title: String,
        contributorName: String?,
        note: String?,
        amount: Long,
        type: TransactionType,
        proofPath: String? = null,
        correctsId: String? = null,
        createdBy: String,
        transactionDate: String
    ): Resource<Unit> {
        return financeRepository.createTransaction(
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
            transactionDate = transactionDate
        )
    }
}
