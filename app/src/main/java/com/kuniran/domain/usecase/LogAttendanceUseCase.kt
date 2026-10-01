package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.WargaRepository

class LogAttendanceUseCase(
    private val wargaRepository: WargaRepository
) {
    suspend operator fun invoke(
        rtId: String,
        wargaId: String,
        residentName: String,
        eventTitle: String,
        eventLocation: String? = null
    ): Resource<Unit> {
        return wargaRepository.logAttendance(
            rtId = rtId,
            wargaId = wargaId,
            residentName = residentName,
            eventTitle = eventTitle,
            eventLocation = eventLocation
        )
    }
}
