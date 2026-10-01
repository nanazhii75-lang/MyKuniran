package com.kuniran.data.repository

import com.kuniran.core.common.DateTimeUtils
import com.kuniran.core.common.ExceptionMapper
import com.kuniran.core.common.Resource
import com.kuniran.core.database.PostDao
import com.kuniran.core.database.PostEntity
import com.kuniran.core.model.Post
import com.kuniran.core.model.PostRsvp
import com.kuniran.core.model.PostType
import com.kuniran.core.model.RsvpStatus
import com.kuniran.core.network.DeletePostRequest
import com.kuniran.core.network.PinPostRequest
import com.kuniran.core.network.SupabaseApiService
import com.kuniran.core.network.UnpinPostRequest
import com.kuniran.core.network.toDomain
import com.kuniran.domain.repository.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class PostRepositoryImpl(
    private val apiService: SupabaseApiService,
    private val postDao: PostDao
) : PostRepository {

    private val rsvpsState = MutableStateFlow<List<PostRsvp>>(emptyList())

    override fun getPostsFlow(rtId: String): Flow<List<Post>> {
        return postDao.getPostsFlow(rtId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun syncPosts(rtId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val dtoList = apiService.getPosts("eq.$rtId")
            val domainList = dtoList.map { it.toDomain() }
            postDao.insertPosts(domainList.map { PostEntity.fromDomain(it) })
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun createPost(
        rtId: String,
        authorId: String,
        title: String,
        content: String,
        type: PostType,
        eventDate: String?,
        eventLocation: String?
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        val newId = UUID.randomUUID().toString()
        val now = DateTimeUtils.currentIsoTimestamp()

        val post = Post(
            id = newId,
            rtId = rtId,
            authorId = authorId,
            title = title,
            content = content,
            type = type,
            eventDate = eventDate,
            eventLocation = eventLocation,
            financeRefId = null,
            categoryRefId = null,
            recapMonth = null,
            meta = null,
            isPinned = false,
            pinnedUntil = null,
            deletedAt = null,
            createdAt = now,
            updatedAt = now
        )

        // Save immediately to Room (Single Source of Truth)
        postDao.insertPost(PostEntity.fromDomain(post))

        // Push to Supabase
        try {
            val body = mutableMapOf<String, Any?>(
                "id" to newId,
                "rt_id" to rtId,
                "author_id" to authorId,
                "title" to title,
                "content" to content,
                "type" to type.name,
                "event_date" to eventDate,
                "event_location" to eventLocation
            )
            apiService.createPost(body)

            // Trigger Edge Function to notify RT members via device_tokens
            runCatching {
                val notifCategory = when (type) {
                    PostType.PENGUMUMAN -> "urgent"
                    PostType.AGENDA -> "agenda"
                    PostType.DISKUSI -> "forum"
                    PostType.FINANCE_REPORT -> "urgent"
                }
                apiService.sendRtNotification(
                    mapOf(
                        "rt_id" to rtId,
                        "category" to notifCategory,
                        "title" to title,
                        "body" to content.take(150),
                        "location" to eventLocation,
                        "time" to eventDate,
                        "post_id" to newId,
                        "exclude_profile_id" to authorId
                    )
                )
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            val err = ExceptionMapper.map(e)
            // Idempotent error 23505 is not a failure
            if (e.message?.contains("23505") == true) {
                Resource.Success(Unit)
            } else {
                Resource.Error(err)
            }
        }
    }

    override suspend fun pinPost(postId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.pinPost(PinPostRequest(postId))
            postDao.updatePinStatus(postId, true, null)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun unpinPost(postId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.unpinPost(UnpinPostRequest(postId))
            postDao.updatePinStatus(postId, false, null)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun deletePost(postId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.deletePost(DeletePostRequest(postId))
            postDao.softDeletePost(postId, DateTimeUtils.currentIsoTimestamp())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun submitRsvp(
        postId: String,
        userId: String,
        rtId: String,
        status: RsvpStatus
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = mapOf(
                "post_id" to postId,
                "user_id" to userId,
                "rt_id" to rtId,
                "status" to status.name
            )
            apiService.upsertPostRsvp(body)

            // Update local memory state
            val currentList = rsvpsState.value.filterNot { it.postId == postId && it.userId == userId }.toMutableList()
            currentList.add(
                PostRsvp(
                    id = UUID.randomUUID().toString(),
                    postId = postId,
                    userId = userId,
                    rtId = rtId,
                    status = status,
                    createdAt = DateTimeUtils.currentIsoTimestamp()
                )
            )
            rsvpsState.value = currentList
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override fun getRsvpsFlow(rtId: String): Flow<List<PostRsvp>> = flow {
        try {
            val dtoList = apiService.getPostRsvps("eq.$rtId")
            val domainList = dtoList.map { dto ->
                PostRsvp(
                    id = dto.id ?: UUID.randomUUID().toString(),
                    postId = dto.postId,
                    userId = dto.userId,
                    rtId = dto.rtId,
                    status = runCatching { RsvpStatus.valueOf(dto.status) }.getOrDefault(RsvpStatus.HADIR),
                    createdAt = dto.createdAt ?: ""
                )
            }
            rsvpsState.value = domainList
            emit(domainList)
        } catch (_: Exception) {
            emit(rsvpsState.value)
        }
    }.flowOn(Dispatchers.IO)
}
