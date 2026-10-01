package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.core.model.UserProfile
import com.kuniran.domain.repository.AuthRepository

class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String, email: String, name: String): Resource<UserProfile> {
        return authRepository.signInWithGoogle(idToken, email, name)
    }
}
