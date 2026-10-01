package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.FinanceRepository

class CreateFinanceCategoryUseCase(
    private val financeRepository: FinanceRepository
) {
    suspend operator fun invoke(rtId: String, name: String, description: String?): Resource<Unit> {
        return financeRepository.createCategory(rtId, name, description)
    }
}
