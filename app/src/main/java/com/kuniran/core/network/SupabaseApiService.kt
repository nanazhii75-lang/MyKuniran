package com.kuniran.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApiService {

    // Health / Root Connectivity Ping
    @GET("rest/v1/")
    suspend fun pingRoot(): Response<Map<String, Any?>>

    // Auth / Supabase ID Token Exchange
    @POST("auth/v1/token?grant_type=id_token")
    suspend fun signInWithIdToken(
        @Body body: Map<String, String>
    ): Response<SupabaseAuthResponseDto>

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(
        @Body body: Map<String, String>
    ): Response<SupabaseAuthResponseDto>

    // Profiles
    @GET("rest/v1/profiles?select=*")
    suspend fun getProfile(
        @Query("id") idFilter: String
    ): List<ProfileDto>

    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(
        @Query("id") idFilter: String,
        @Body body: Map<String, String?>
    ): Response<Unit>

    // RT Groups
    @GET("rest/v1/rt_groups?select=*")
    suspend fun getRtGroup(
        @Query("id") idFilter: String
    ): List<RtGroupDto>

    // Posts
    @GET("rest/v1/posts?deleted_at=is.null&order=is_pinned.desc,created_at.desc&select=*")
    suspend fun getPosts(
        @Query("rt_id") rtIdFilter: String
    ): List<PostDto>

    @POST("rest/v1/posts")
    suspend fun createPost(
        @Body body: Map<String, Any?>
    ): Response<Unit>

    // Finance Categories
    @GET("rest/v1/finance_categories?select=*")
    suspend fun getCategories(
        @Query("rt_id") rtIdFilter: String
    ): List<CategoryDto>

    @POST("rest/v1/finance_categories")
    suspend fun createCategory(
        @Body body: Map<String, Any?>
    ): Response<Unit>

    @PATCH("rest/v1/finance_categories")
    suspend fun updateCategory(
        @Query("id") idFilter: String,
        @Body body: Map<String, Any?>
    ): Response<Unit>

    // Finances (Transactions)
    @GET("rest/v1/finances?deleted_at=is.null&order=transaction_date.desc&select=*")
    suspend fun getTransactions(
        @Query("rt_id") rtIdFilter: String
    ): List<FinanceDto>

    @POST("rest/v1/finances")
    suspend fun createTransaction(
        @Body body: Map<String, Any?>
    ): Response<Unit>

    // RPCs
    @POST("rest/v1/rpc/create_rt")
    suspend fun createRt(
        @Body request: CreateRtRequest
    ): String

    @POST("rest/v1/rpc/preview_rt")
    suspend fun previewRt(
        @Body request: PreviewRtRequest
    ): String?

    @POST("rest/v1/rpc/request_join_rt")
    suspend fun requestJoinRt(
        @Body request: RequestJoinRtRequest
    ): String

    @POST("rest/v1/rpc/cancel_join_request")
    suspend fun cancelJoinRequest(): Response<Unit>

    @POST("rest/v1/rpc/list_pending_join_requests")
    suspend fun listPendingJoinRequests(): List<JoinRequestDto>

    @POST("rest/v1/rpc/approve_join_request")
    suspend fun approveJoinRequest(
        @Body request: ApproveJoinRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/reject_join_request")
    suspend fun rejectJoinRequest(
        @Body request: RejectJoinRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/set_auto_approve")
    suspend fun setAutoApprove(
        @Body request: SetAutoApproveRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/check_username_available")
    suspend fun checkUsernameAvailable(
        @Body request: CheckUsernameRequest
    ): String

    @POST("rest/v1/rpc/set_invite_username")
    suspend fun setInviteUsername(
        @Body request: SetInviteUsernameRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/update_rt_info")
    suspend fun updateRtInfo(
        @Body request: UpdateRtInfoRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/transfer_admin")
    suspend fun transferAdmin(
        @Body request: TransferAdminRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/assign_bendahara")
    suspend fun assignBendahara(
        @Body request: AssignBendaharaRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/leave_rt")
    suspend fun leaveRt(): Response<Unit>

    @POST("rest/v1/rpc/remove_member")
    suspend fun removeMember(
        @Body request: RemoveMemberRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/pin_post")
    suspend fun pinPost(
        @Body request: PinPostRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/unpin_post")
    suspend fun unpinPost(
        @Body request: UnpinPostRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/delete_post")
    suspend fun deletePost(
        @Body request: DeletePostRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/delete_transaction")
    suspend fun deleteTransaction(
        @Body request: DeleteTransactionRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/publish_finance_report")
    suspend fun publishFinanceReport(
        @Body request: PublishFinanceReportRequest
    ): String

    @POST("rest/v1/rpc/create_finance_agenda")
    suspend fun createFinanceAgenda(
        @Body request: CreateFinanceAgendaRequest
    ): String

    @POST("rest/v1/rpc/publish_monthly_recap")
    suspend fun publishMonthlyRecap(
        @Body request: PublishMonthlyRecapRequest
    ): String

    @POST("rest/v1/rpc/create_finance_category")
    suspend fun createFinanceCategory(
        @Body request: CreateFinanceCategoryRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/update_finance_category")
    suspend fun updateFinanceCategory(
        @Body request: UpdateFinanceCategoryRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/register_device_token")
    suspend fun registerDeviceToken(
        @Body request: RegisterDeviceTokenRequest
    ): Response<Unit>

    @POST("rest/v1/rpc/rt_people")
    suspend fun rtPeople(): List<RtMemberDto>

    // Warga (Residents Directory)
    @GET("rest/v1/warga?order=full_name.asc&select=*")
    suspend fun getWargaList(
        @Query("rt_id") rtIdFilter: String
    ): List<WargaDto>

    @GET("rest/v1/warga?select=*")
    suspend fun getWargaById(
        @Query("id") idFilter: String
    ): List<WargaDto>

    @POST("rest/v1/warga")
    suspend fun insertWarga(
        @Body body: Map<String, Any?>
    ): Response<Unit>

    @PATCH("rest/v1/warga")
    suspend fun updateWarga(
        @Query("id") idFilter: String,
        @Body body: Map<String, Any?>
    ): Response<Unit>

    @DELETE("rest/v1/warga")
    suspend fun deleteWarga(
        @Query("id") idFilter: String
    ): Response<Unit>

    // Finance Records (Transactions)
    @GET("rest/v1/finance_records?order=transaction_date.desc&select=*")
    suspend fun getFinanceRecords(
        @Query("rt_id") rtIdFilter: String
    ): List<FinanceRecordDto>

    @POST("rest/v1/finance_records")
    suspend fun insertFinanceRecord(
        @Body body: Map<String, Any?>
    ): Response<Unit>

    @PATCH("rest/v1/finance_records")
    suspend fun updateFinanceRecord(
        @Query("id") idFilter: String,
        @Body body: Map<String, Any?>
    ): Response<Unit>

    @DELETE("rest/v1/finance_records")
    suspend fun deleteFinanceRecord(
        @Query("id") idFilter: String
    ): Response<Unit>

    // Warga Activity & Attendance
    @POST("rest/v1/warga_activities")
    suspend fun insertWargaActivity(
        @Body body: Map<String, Any?>
    ): Response<Unit>

    // Post RSVPs (Calendar + RSVP)
    @GET("rest/v1/post_rsvps?select=*")
    suspend fun getPostRsvps(
        @Query("rt_id") rtIdFilter: String
    ): List<PostRsvpDto>

    @POST("rest/v1/post_rsvps")
    suspend fun upsertPostRsvp(
        @Body body: Map<String, Any?>
    ): Response<Unit>

    @POST("rest/v1/rpc/get_finance_summary")
    suspend fun getFinanceSummary(): Map<String, Any?>

    // Edge Functions for Device Tokens Push Notifications
    @POST("functions/v1/send-rt-notification")
    suspend fun sendRtNotification(
        @Body payload: Map<String, Any?>
    ): Response<Unit>

    @POST("functions/v1/agenda-reminder-h1")
    suspend fun triggerAgendaReminder(): Response<Unit>
}
