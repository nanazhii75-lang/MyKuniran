package com.kuniran.domain.usecase

import com.kuniran.core.model.PostRsvp
import com.kuniran.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow

class GetAgendaRsvpsUseCase(
    private val postRepository: PostRepository
) {
    operator fun invoke(rtId: String): Flow<List<PostRsvp>> {
        return postRepository.getRsvpsFlow(rtId)
    }
}
