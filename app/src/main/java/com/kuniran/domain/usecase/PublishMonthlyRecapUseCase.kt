package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.FinanceRepository

class PublishMonthlyRecapUseCase(
    private val financeRepository: FinanceRepository
) {
    suspend operator fun invoke(categoryId: String, monthDate: String): Resource<String> {
        return financeRepository.publishMonthlyRecap(categoryId, monthDate)
    }
}
