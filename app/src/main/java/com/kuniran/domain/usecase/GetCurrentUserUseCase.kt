package com.kuniran.domain.usecase

import com.kuniran.core.model.UserProfile
import com.kuniran.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class GetCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<UserProfile?> = authRepository.getCurrentUserFlow()
}
