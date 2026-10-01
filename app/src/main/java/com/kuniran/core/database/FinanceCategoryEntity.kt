package com.kuniran.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuniran.core.model.FinanceCategory

@Entity(tableName = "finance_categories")
data class FinanceCategoryEntity(
    @PrimaryKey val id: String,
    val rtId: String,
    val name: String,
    val description: String?,
    val bendaharaId: String?,
    val isArchived: Boolean,
    val createdAt: String,
    val updatedAt: String
) {
    fun toDomain(): FinanceCategory = FinanceCategory(
        id = id,
        rtId = rtId,
        name = name,
        description = description,
        bendaharaId = bendaharaId,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(c: FinanceCategory): FinanceCategoryEntity = FinanceCategoryEntity(
            id = c.id,
            rtId = c.rtId,
            name = c.name,
            description = c.description,
            bendaharaId = c.bendaharaId,
            isArchived = c.isArchived,
            createdAt = c.createdAt,
            updatedAt = c.updatedAt
        )
    }
}
