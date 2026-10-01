package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.core.model.JoinRequest
import com.kuniran.domain.repository.RtRepository

class ListPendingJoinRequestsUseCase(
    private val rtRepository: RtRepository
) {
    suspend operator fun invoke(): Resource<List<JoinRequest>> {
        return rtRepository.listPendingRequests()
    }
}
