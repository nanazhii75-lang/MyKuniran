package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.RtRepository

class RequestJoinRtUseCase(
    private val rtRepository: RtRepository
) {
    suspend operator fun invoke(inviteUsername: String): Resource<String> {
        return rtRepository.requestJoinRt(inviteUsername)
    }
}
