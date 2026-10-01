package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.core.model.FinanceRecord
import com.kuniran.domain.repository.FinanceRecordRepository
import kotlinx.coroutines.flow.Flow

class GetFinanceRecordsUseCase(
    private val financeRecordRepository: FinanceRecordRepository
) {
    operator fun invoke(rtId: String): Flow<Resource<List<FinanceRecord>>> {
        return financeRecordRepository.getFinanceRecords(rtId)
    }
}
