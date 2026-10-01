package com.kuniran.core.model

data class Warga(
    val id: String,
    val authUserId: String?,
    val rtId: String,
    val fullName: String,
    val phoneNumber: String?,
    val houseNumber: String?,
    val houseInfo: String?,
    val houseBlock: String? = null,
    val rtRole: String,
    val occupation: String?,
    val gender: String?,
    val isHeadOfFamily: Boolean,
    val isVerified: Boolean,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)
