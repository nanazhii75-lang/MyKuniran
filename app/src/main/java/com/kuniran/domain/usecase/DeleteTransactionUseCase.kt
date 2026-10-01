package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.FinanceRepository

class DeleteTransactionUseCase(
    private val financeRepository: FinanceRepository
) {
    suspend operator fun invoke(id: String): Resource<Unit> {
        return financeRepository.deleteTransaction(id)
    }
}
