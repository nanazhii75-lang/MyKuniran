package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.MemberRepository

class RemoveMemberUseCase(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(profileId: String): Resource<Unit> {
        return memberRepository.removeMember(profileId)
    }
}
