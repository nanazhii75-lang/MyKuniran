package com.kuniran.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finances WHERE categoryId = :categoryId AND deletedAt IS NULL ORDER BY transactionDate DESC")
    fun getTransactionsFlow(categoryId: String): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finances WHERE rtId = :rtId AND deletedAt IS NULL ORDER BY transactionDate DESC")
    fun getAllRtTransactionsFlow(rtId: String): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finances WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): FinanceTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<FinanceTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinanceTransactionEntity)

    @Query("UPDATE finances SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)

    @Query("UPDATE finances SET isLocked = :isLocked WHERE id = :id")
    suspend fun updateLockStatus(id: String, isLocked: Boolean)

    @Query("DELETE FROM finances WHERE rtId = :rtId")
    suspend fun clearRtFinances(rtId: String)
}
