package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.FinanceRepository

class CreateFinanceAgendaUseCase(
    private val financeRepository: FinanceRepository
) {
    suspend operator fun invoke(categoryId: String, title: String, eventDate: String, location: String): Resource<String> {
        return financeRepository.createFinanceAgenda(categoryId, title, eventDate, location)
    }
}
