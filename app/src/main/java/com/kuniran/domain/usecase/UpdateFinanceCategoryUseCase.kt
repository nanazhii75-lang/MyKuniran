package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.FinanceRepository

class UpdateFinanceCategoryUseCase(
    private val financeRepository: FinanceRepository
) {
    suspend operator fun invoke(
        categoryId: String,
        name: String,
        description: String?,
        isArchived: Boolean = false
    ): Resource<Unit> {
        return financeRepository.updateCategory(categoryId, name, description, isArchived)
    }
}
