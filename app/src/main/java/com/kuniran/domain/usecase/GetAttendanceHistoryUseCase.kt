package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.core.model.WargaActivityLog
import com.kuniran.domain.repository.WargaRepository
import kotlinx.coroutines.flow.Flow

class GetAttendanceHistoryUseCase(
    private val wargaRepository: WargaRepository
) {
    operator fun invoke(wargaId: String): Flow<Resource<List<WargaActivityLog>>> {
        return wargaRepository.getAttendanceHistory(wargaId)
    }
}
