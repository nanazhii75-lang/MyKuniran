package com.kuniran.core.network

import com.kuniran.core.model.Warga
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WargaDto(
    @Json(name = "id") val id: String,
    @Json(name = "auth_user_id") val authUserId: String? = null,
    @Json(name = "rt_id") val rtId: String? = null,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "phone_number") val phoneNumber: String? = null,
    @Json(name = "house_number") val houseNumber: String? = null,
    @Json(name = "house_info") val houseInfo: String? = null,
    @Json(name = "house_block") val houseBlock: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "rt_role") val rtRole: String? = null,
    @Json(name = "occupation") val occupation: String? = null,
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "is_head_of_family") val isHeadOfFamily: Boolean? = false,
    @Json(name = "is_verified") val isVerified: Boolean? = true,
    @Json(name = "is_active") val isActive: Boolean? = true,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    fun toDomain(): Warga = Warga(
        id = id,
        authUserId = authUserId,
        rtId = rtId.orEmpty(),
        fullName = fullName,
        phoneNumber = phoneNumber,
        houseNumber = houseNumber,
        houseInfo = houseInfo,
        houseBlock = houseBlock,
        rtRole = rtRole ?: role ?: "WARGA",
        occupation = occupation,
        gender = gender,
        isHeadOfFamily = isHeadOfFamily ?: false,
        isVerified = isVerified ?: true,
        isActive = isActive ?: true,
        createdAt = createdAt.orEmpty(),
        updatedAt = updatedAt.orEmpty()
    )
}
