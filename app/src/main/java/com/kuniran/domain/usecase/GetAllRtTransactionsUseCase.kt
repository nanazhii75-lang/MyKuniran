package com.kuniran.domain.usecase

import com.kuniran.core.model.FinanceTransaction
import com.kuniran.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow

class GetAllRtTransactionsUseCase(
    private val financeRepository: FinanceRepository
) {
    operator fun invoke(rtId: String): Flow<List<FinanceTransaction>> {
        return financeRepository.getAllRtTransactionsFlow(rtId)
    }
}
