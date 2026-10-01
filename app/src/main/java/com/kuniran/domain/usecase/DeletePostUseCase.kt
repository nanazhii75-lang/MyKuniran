package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.PostRepository

class DeletePostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String): Resource<Unit> {
        return postRepository.deletePost(postId)
    }
}
