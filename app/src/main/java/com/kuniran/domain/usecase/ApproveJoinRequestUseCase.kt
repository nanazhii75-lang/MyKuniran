package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.RtRepository

class ApproveJoinRequestUseCase(
    private val rtRepository: RtRepository
) {
    suspend operator fun invoke(requestId: String): Resource<Unit> {
        return rtRepository.approveJoinRequest(requestId)
    }
}
