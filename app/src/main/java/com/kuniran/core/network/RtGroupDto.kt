package com.kuniran.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RtGroupDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "rt_number") val rtNumber: String,
    @Json(name = "rw_number") val rwNumber: String,
    @Json(name = "desa") val desa: String,
    @Json(name = "dukuh") val dukuh: String,
    @Json(name = "lingkungan") val lingkungan: String,
    @Json(name = "invite_username") val inviteUsername: String,
    @Json(name = "auto_approve_join") val autoApproveJoin: Boolean?,
    @Json(name = "created_by") val createdBy: String?,
    @Json(name = "display_label") val displayLabel: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)
