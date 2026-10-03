package com.kuniran.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE rtId = :rtId AND deletedAt IS NULL ORDER BY isPinned DESC, createdAt DESC")
    fun getPostsFlow(rtId: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Query("UPDATE posts SET isPinned = :isPinned, pinnedUntil = :pinnedUntil WHERE id = :postId")
    suspend fun updatePinStatus(postId: String, isPinned: Boolean, pinnedUntil: String?)

    @Query("UPDATE posts SET deletedAt = :deletedAt WHERE id = :postId")
    suspend fun softDeletePost(postId: String, deletedAt: String)

    @Query("SELECT imagePath FROM posts WHERE id = :postId")
    suspend fun getImagePath(postId: String): String?

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("DELETE FROM posts WHERE rtId = :rtId")
    suspend fun clearRtPosts(rtId: String)
}
