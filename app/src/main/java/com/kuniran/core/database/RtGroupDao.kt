package com.kuniran.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RtGroupDao {
    @Query("SELECT * FROM rt_groups WHERE id = :id LIMIT 1")
    fun getRtGroupFlow(id: String): Flow<RtGroupEntity?>

    @Query("SELECT * FROM rt_groups WHERE id = :id LIMIT 1")
    suspend fun getRtGroup(id: String): RtGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRtGroup(group: RtGroupEntity)

    @Query("DELETE FROM rt_groups")
    suspend fun clearAll()
}
