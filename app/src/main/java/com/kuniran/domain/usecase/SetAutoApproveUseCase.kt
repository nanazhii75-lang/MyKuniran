package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.RtRepository

class SetAutoApproveUseCase(
    private val rtRepository: RtRepository
) {
    suspend operator fun invoke(enabled: Boolean): Resource<Unit> {
        return rtRepository.setAutoApprove(enabled)
    }
}
