package com.kuniran.core.common

import android.content.Context
import androidx.annotation.StringRes
import com.kuniran.R

sealed class AppError(@StringRes val messageRes: Int) {
    object SessionExpired : AppError(R.string.error_session_expired)
    object AccountInactive : AppError(R.string.error_account_inactive)
    object AlreadyInRt : AppError(R.string.error_already_in_rt)
    object NotInRt : AppError(R.string.error_not_in_rt)
    object HasPendingRequest : AppError(R.string.error_has_pending_request)
    object FieldInvalid : AppError(R.string.error_field_invalid)
    object UsernameInvalid : AppError(R.string.error_username_invalid)
    object UsernameTaken : AppError(R.string.error_username_taken)
    object RateLimitCreateRt : AppError(R.string.error_rate_limit_create_rt)
    object RateLimitPost : AppError(R.string.error_rate_limit_post)
    object RtNotFound : AppError(R.string.error_rt_not_found)
    object RtFull : AppError(R.string.error_rt_full)
    object RequestRejectedRecently : AppError(R.string.error_request_rejected_recently)
    object RequestNotFound : AppError(R.string.error_request_not_found)
    object RequestAlreadyDecided : AppError(R.string.error_request_already_decided)
    object ApplicantUnavailable : AppError(R.string.error_applicant_unavailable)
    object NotAdmin : AppError(R.string.error_not_admin)
    object AdminMustTransfer : AppError(R.string.error_admin_must_transfer)
    object TransferToSelf : AppError(R.string.error_transfer_to_self)
    object MemberNotFound : AppError(R.string.error_member_not_found)
    object CannotRemoveSelf : AppError(R.string.error_cannot_remove_self)
    object CategoryNotFound : AppError(R.string.error_category_not_found)
    object CategoryArchived : AppError(R.string.error_category_archived)
    object NotBendahara : AppError(R.string.error_not_bendahara)
    object NotAllowed : AppError(R.string.error_not_allowed)
    object PostNotFound : AppError(R.string.error_post_not_found)
    object ReportCannotDelete : AppError(R.string.error_report_cannot_delete)
    object TransactionNotFound : AppError(R.string.error_transaction_not_found)
    object TransactionLocked : AppError(R.string.error_transaction_locked)
    object AlreadyPublished : AppError(R.string.error_already_published)
    object RecapAlreadyPublished : AppError(R.string.error_recap_already_published)
    object MonthNotEnded : AppError(R.string.error_month_not_ended)
    object MonthAlreadyPublished : AppError(R.string.error_month_already_published)
    object NoTransactions : AppError(R.string.error_no_transactions)
    object DateInFuture : AppError(R.string.error_date_in_future)
    object CorrectionInvalid : AppError(R.string.error_correction_invalid)
    object RateLimitLookup : AppError(R.string.error_rate_limit_lookup)
    object DuplicateName : AppError(R.string.error_duplicate_name)
    object InvalidName : AppError(R.string.error_invalid_name)
    object AlreadyAttended : AppError(R.string.error_already_attended)
    object Network : AppError(R.string.error_network)
    data class LoginFailed(val detail: String? = null) : AppError(R.string.error_login_failed)
    data class Unknown(val technicalMessage: String? = null) : AppError(R.string.error_unknown)
}

/** SEMENTARA: kesalahan tak dikenal menampilkan detail teknis untuk pelacakan bug. Kembalikan ke build debug saja setelah bug selesai. */
fun AppError.asText(context: Context): String {
    val base = context.getString(messageRes)
    return if (this is AppError.Unknown && !technicalMessage.isNullOrBlank()) {
        "$base [${technicalMessage.take(160)}]"
    } else base
}
