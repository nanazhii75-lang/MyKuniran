package com.kuniran.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RtMemberDto(
    @Json(name = "id") val id: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "avatar_path") val avatarPath: String? = null,
    @Json(name = "house_info") val houseInfo: String? = null,
    @Json(name = "house_block") val houseBlock: String? = null,
    @Json(name = "phone_number") val phoneNumber: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "is_member") val isMember: Boolean? = true
)
