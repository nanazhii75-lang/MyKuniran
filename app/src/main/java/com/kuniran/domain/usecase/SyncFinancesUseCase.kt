package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.FinanceRepository

class SyncFinancesUseCase(
    private val financeRepository: FinanceRepository
) {
    suspend operator fun invoke(rtId: String): Resource<Unit> {
        val catResult = financeRepository.syncCategories(rtId)
        if (catResult is Resource.Error) return catResult
        return financeRepository.syncTransactions(rtId)
    }
}
