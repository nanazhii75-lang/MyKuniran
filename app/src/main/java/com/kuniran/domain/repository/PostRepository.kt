package com.kuniran.domain.repository

import com.kuniran.core.common.Resource
import com.kuniran.core.model.Post
import com.kuniran.core.model.PostRsvp
import com.kuniran.core.model.PostType
import com.kuniran.core.model.RsvpStatus
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPostsFlow(rtId: String): Flow<List<Post>>
    suspend fun syncPosts(rtId: String): Resource<Unit>
    suspend fun createPost(
        rtId: String,
        authorId: String,
        title: String,
        content: String,
        type: PostType,
        eventDate: String? = null,
        eventLocation: String? = null,
        imageBytes: ByteArray? = null
    ): Resource<Unit>
    suspend fun pinPost(postId: String): Resource<Unit>
    suspend fun unpinPost(postId: String): Resource<Unit>
    suspend fun deletePost(postId: String): Resource<Unit>
    suspend fun submitRsvp(
        postId: String,
        userId: String,
        rtId: String,
        status: RsvpStatus
    ): Resource<Unit>
    fun getRsvpsFlow(rtId: String): Flow<List<PostRsvp>>
}
