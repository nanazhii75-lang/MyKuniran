package com.kuniran.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MemberDao {
    @Query("SELECT * FROM members ORDER BY isMember DESC, fullName ASC")
    abstract fun getMembersFlow(): Flow<List<MemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMembers(members: List<MemberEntity>)

    @Query("DELETE FROM members")
    abstract suspend fun clearMembers()

    @Transaction
    open suspend fun replaceMembers(members: List<MemberEntity>) {
        clearMembers()
        insertMembers(members)
    }
}
