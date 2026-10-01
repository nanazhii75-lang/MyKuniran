package com.kuniran.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JoinRequestDto(
    @Json(name = "request_id") val requestId: String,
    @Json(name = "profile_id") val profileId: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "phone_number") val phoneNumber: String? = null,
    @Json(name = "house_info") val houseInfo: String? = null,
    @Json(name = "requested_at") val requestedAt: String
)
