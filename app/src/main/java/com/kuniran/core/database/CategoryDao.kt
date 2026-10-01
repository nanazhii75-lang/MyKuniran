package com.kuniran.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM finance_categories WHERE rtId = :rtId ORDER BY name ASC")
    fun getCategoriesFlow(rtId: String): Flow<List<FinanceCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<FinanceCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: FinanceCategoryEntity)

    @Query("UPDATE finance_categories SET bendaharaId = :bendaharaId WHERE id = :categoryId")
    suspend fun updateBendahara(categoryId: String, bendaharaId: String?)

    @Query("UPDATE finance_categories SET isArchived = :isArchived WHERE id = :categoryId")
    suspend fun updateArchived(categoryId: String, isArchived: Boolean)

    @Query("UPDATE finance_categories SET name = :name, description = :description WHERE id = :categoryId")
    suspend fun updateCategoryInfo(categoryId: String, name: String, description: String?)

    @Query("DELETE FROM finance_categories WHERE rtId = :rtId")
    suspend fun clearRtCategories(rtId: String)
}
