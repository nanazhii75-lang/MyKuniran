package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.MemberRepository

class SyncMembersUseCase(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return memberRepository.syncMembers()
    }
}
