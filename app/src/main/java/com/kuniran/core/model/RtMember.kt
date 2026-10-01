package com.kuniran.core.model

data class RtMember(
    val id: String,
    val fullName: String,
    val avatarPath: String?,
    val houseInfo: String?,
    val phoneNumber: String?,
    val role: UserRole?,
    val isMember: Boolean
)
