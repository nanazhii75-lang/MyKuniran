package com.kuniran.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateRtRequest(
    @Json(name = "p_name") val name: String,
    @Json(name = "p_rt_number") val rtNumber: String,
    @Json(name = "p_rw_number") val rwNumber: String,
    @Json(name = "p_desa") val desa: String,
    @Json(name = "p_dukuh") val dukuh: String,
    @Json(name = "p_lingkungan") val lingkungan: String,
    @Json(name = "p_invite_username") val inviteUsername: String
)

@JsonClass(generateAdapter = true)
data class PreviewRtRequest(
    @Json(name = "p_invite_username") val inviteUsername: String
)

@JsonClass(generateAdapter = true)
data class RequestJoinRtRequest(
    @Json(name = "p_invite_username") val inviteUsername: String
)

@JsonClass(generateAdapter = true)
data class ApproveJoinRequest(
    @Json(name = "p_request_id") val requestId: String
)

@JsonClass(generateAdapter = true)
data class RejectJoinRequest(
    @Json(name = "p_request_id") val requestId: String
)

@JsonClass(generateAdapter = true)
data class SetAutoApproveRequest(
    @Json(name = "p_enabled") val enabled: Boolean
)

@JsonClass(generateAdapter = true)
data class CheckUsernameRequest(
    @Json(name = "p_username") val username: String
)

@JsonClass(generateAdapter = true)
data class SetInviteUsernameRequest(
    @Json(name = "p_username") val username: String
)

@JsonClass(generateAdapter = true)
data class UpdateRtInfoRequest(
    @Json(name = "p_name") val name: String,
    @Json(name = "p_rt_number") val rtNumber: String,
    @Json(name = "p_rw_number") val rwNumber: String,
    @Json(name = "p_desa") val desa: String,
    @Json(name = "p_dukuh") val dukuh: String,
    @Json(name = "p_lingkungan") val lingkungan: String
)

@JsonClass(generateAdapter = true)
data class TransferAdminRequest(
    @Json(name = "p_new_admin_id") val newAdminId: String
)

@JsonClass(generateAdapter = true)
data class AssignBendaharaRequest(
    @Json(name = "p_category_id") val categoryId: String,
    @Json(name = "p_profile_id") val profileId: String?
)

@JsonClass(generateAdapter = true)
data class RemoveMemberRequest(
    @Json(name = "p_profile_id") val profileId: String
)

@JsonClass(generateAdapter = true)
data class PinPostRequest(
    @Json(name = "p_post_id") val postId: String
)

@JsonClass(generateAdapter = true)
data class UnpinPostRequest(
    @Json(name = "p_post_id") val postId: String
)

@JsonClass(generateAdapter = true)
data class DeletePostRequest(
    @Json(name = "p_post_id") val postId: String
)

@JsonClass(generateAdapter = true)
data class DeleteTransactionRequest(
    @Json(name = "p_id") val id: String
)

@JsonClass(generateAdapter = true)
data class PublishFinanceReportRequest(
    @Json(name = "p_finance_id") val financeId: String,
    @Json(name = "p_note") val note: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateFinanceAgendaRequest(
    @Json(name = "p_category_id") val categoryId: String,
    @Json(name = "p_title") val title: String,
    @Json(name = "p_event_date") val eventDate: String,
    @Json(name = "p_event_location") val eventLocation: String
)

@JsonClass(generateAdapter = true)
data class PublishMonthlyRecapRequest(
    @Json(name = "p_category_id") val categoryId: String,
    @Json(name = "p_bulan") val bulan: String
)

@JsonClass(generateAdapter = true)
data class RegisterDeviceTokenRequest(
    @Json(name = "p_token") val token: String
)

@JsonClass(generateAdapter = true)
data class CreateFinanceCategoryRequest(
    @Json(name = "p_name") val name: String,
    @Json(name = "p_description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateFinanceCategoryRequest(
    @Json(name = "p_category_id") val categoryId: String,
    @Json(name = "p_name") val name: String,
    @Json(name = "p_description") val description: String? = null,
    @Json(name = "p_is_archived") val isArchived: Boolean? = null
)
