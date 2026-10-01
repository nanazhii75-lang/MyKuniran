package com.kuniran.domain.usecase

import com.kuniran.core.model.FinanceTransaction
import com.kuniran.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow

class GetTransactionsUseCase(
    private val financeRepository: FinanceRepository
) {
    operator fun invoke(categoryId: String): Flow<List<FinanceTransaction>> {
        return financeRepository.getTransactionsFlow(categoryId)
    }
}
