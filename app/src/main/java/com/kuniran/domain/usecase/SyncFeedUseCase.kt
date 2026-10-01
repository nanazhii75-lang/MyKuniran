package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.domain.repository.PostRepository

class SyncFeedUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(rtId: String): Resource<Unit> {
        return postRepository.syncPosts(rtId)
    }
}
