package com.kuniran.domain.usecase

import com.kuniran.core.model.FinanceCategory
import com.kuniran.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow

class GetFinanceCategoriesUseCase(
    private val financeRepository: FinanceRepository
) {
    operator fun invoke(rtId: String): Flow<List<FinanceCategory>> {
        return financeRepository.getCategoriesFlow(rtId)
    }
}
