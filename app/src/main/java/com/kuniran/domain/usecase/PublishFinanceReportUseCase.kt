package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.FinanceRepository

class PublishFinanceReportUseCase(
    private val financeRepository: FinanceRepository
) {
    suspend operator fun invoke(financeId: String, note: String? = null): Resource<String> {
        return financeRepository.publishFinanceReport(financeId, note)
    }
}
