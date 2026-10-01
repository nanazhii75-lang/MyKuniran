package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.AuthRepository

class UpdateProfileUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(fullName: String, phoneNumber: String?, houseInfo: String?): Resource<Unit> {
        return authRepository.updateProfile(fullName, phoneNumber, houseInfo)
    }
}
