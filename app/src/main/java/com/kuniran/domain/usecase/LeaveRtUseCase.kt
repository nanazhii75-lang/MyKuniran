package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.RtRepository

class LeaveRtUseCase(
    private val rtRepository: RtRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return rtRepository.leaveRt()
    }
}
