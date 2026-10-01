package com.kuniran.core.model

data class UserProfile(
    val id: String,
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
)
