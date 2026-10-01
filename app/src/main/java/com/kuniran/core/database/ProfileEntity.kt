package com.kuniran.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuniran.core.model.UserProfile
import com.kuniran.core.model.UserRole

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val rtId: String?,
    val fullName: String,
    val email: String?,
    val avatarPath: String?,
    val houseInfo: String?,
    val phoneNumber: String?,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
) {
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        rtId = rtId,
        fullName = fullName,
        email = email,
        avatarPath = avatarPath,
        houseInfo = houseInfo,
        phoneNumber = phoneNumber,
        role = role,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(p: UserProfile): ProfileEntity = ProfileEntity(
            id = p.id,
            rtId = p.rtId,
            fullName = p.fullName,
            email = p.email,
            avatarPath = p.avatarPath,
            houseInfo = p.houseInfo,
            phoneNumber = p.phoneNumber,
            role = p.role,
            isActive = p.isActive,
            createdAt = p.createdAt,
            updatedAt = p.updatedAt
        )
    }
}
