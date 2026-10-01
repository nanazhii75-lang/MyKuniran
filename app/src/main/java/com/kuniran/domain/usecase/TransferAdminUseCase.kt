package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.MemberRepository

class TransferAdminUseCase(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(newAdminId: String): Resource<Unit> {
        return memberRepository.transferAdmin(newAdminId)
    }
}
