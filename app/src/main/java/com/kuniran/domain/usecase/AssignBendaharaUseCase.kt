package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.MemberRepository

class AssignBendaharaUseCase(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(categoryId: String, profileId: String?): Resource<Unit> {
        return memberRepository.assignBendahara(categoryId, profileId)
    }
}
