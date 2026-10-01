package com.kuniran.domain.usecase

import com.kuniran.core.model.RtMember
import com.kuniran.domain.repository.MemberRepository
import kotlinx.coroutines.flow.Flow

class GetRtMembersUseCase(
    private val memberRepository: MemberRepository
) {
    operator fun invoke(): Flow<List<RtMember>> {
        return memberRepository.getMembersFlow()
    }
}
