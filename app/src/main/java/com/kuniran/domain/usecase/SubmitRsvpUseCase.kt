package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.core.model.RsvpStatus
import com.kuniran.domain.repository.PostRepository

class SubmitRsvpUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(
        postId: String,
        userId: String,
        rtId: String,
        status: RsvpStatus
    ): Resource<Unit> {
        return postRepository.submitRsvp(
            postId = postId,
            userId = userId,
            rtId = rtId,
            status = status
        )
    }
}
