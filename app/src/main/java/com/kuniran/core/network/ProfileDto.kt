package com.kuniran.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "rt_id") val rtId: String?,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "email") val email: String?,
    @Json(name = "avatar_path") val avatarPath: String?,
    @Json(name = "house_info") val houseInfo: String?,
    @Json(name = "phone_number") val phoneNumber: String?,
    @Json(name = "role") val role: String?,
    @Json(name = "is_active") val isActive: Boolean?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)
