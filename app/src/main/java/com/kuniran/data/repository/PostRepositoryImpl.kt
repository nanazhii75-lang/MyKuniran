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

        // Server dulu: data lokal baru disimpan setelah server menerima (tanpa postingan hantu)
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
            val response = apiService.createPost(body)
            val errBody = if (response.isSuccessful) null
                else runCatching { response.errorBody()?.string() }.getOrNull()
            // 409 + 23505 pada id yang kita kirim sendiri = percobaan ulang yang sudah sampai (idempoten)
            val duplicate = response.code() == 409 && errBody?.contains("23505") == true
            if (!response.isSuccessful && !duplicate) {
                return@withContext Resource.Error(ExceptionMapper.mapResponse(response.code(), errBody))
            }
            postDao.insertPost(PostEntity.fromDomain(post))

            // Push hanya untuk pengumuman dan agenda; obrolan (DISKUSI) tidak memicu notifikasi.
            // Hanya post_id yang dikirim; server memverifikasi pemanggil, RT, dan isi pos dari database.
            // Kegagalan push tidak boleh menggagalkan pos.
            if (type != PostType.DISKUSI) {
                runCatching {
                    apiService.sendRtNotification(mapOf("post_id" to newId))
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
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
            val response = apiService.upsertPostRsvp(body)
            if (!response.isSuccessful) {
                return@withContext Resource.Error(
                    ExceptionMapper.mapResponse(response.code(), runCatching { response.errorBody()?.string() }.getOrNull())
                )
            }

            // Perbarui status lokal hanya setelah server menerima
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
