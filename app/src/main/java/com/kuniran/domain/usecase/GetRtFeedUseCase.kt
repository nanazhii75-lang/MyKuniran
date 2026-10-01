package com.kuniran.domain.usecase

import com.kuniran.core.model.Post
import com.kuniran.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow

class GetRtFeedUseCase(
    private val postRepository: PostRepository
) {
    operator fun invoke(rtId: String): Flow<List<Post>> {
        return postRepository.getPostsFlow(rtId)
    }
}
