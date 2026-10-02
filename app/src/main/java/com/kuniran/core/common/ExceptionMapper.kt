package com.kuniran.core.common

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ExceptionMapper {

    fun map(throwable: Throwable): AppError {
        return when (throwable) {
            is UnknownHostException,
            is SocketTimeoutException -> AppError.Network
            is IOException -> AppError.Network
            is HttpException -> mapHttpException(throwable)
            else -> mapGenericThrowable(throwable)
        }
    }

    private fun mapHttpException(exception: HttpException): AppError {
        val code = exception.code()
        val errorBody = try {
            exception.response()?.errorBody()?.string() ?: ""
        } catch (_: Exception) {
            ""
        }

        if (code == 401) {
            return AppError.SessionExpired
        }

        if (code == 403 || errorBody.contains("42501") || errorBody.contains("FORBIDDEN_DIRECT_CHANGE")) {
            return AppError.NotAllowed
        }

        if (code in 500..599) {
            return AppError.Network
        }

        // Try extracting hint or message code from error body (PostgREST JSON)
        return mapErrorCodeString(errorBody)
    }

    fun mapErrorCodeString(raw: String): AppError {
        val upper = raw.uppercase()
        return when {
            upper.contains("SESSION_EXPIRED") -> AppError.SessionExpired
            upper.contains("ACCOUNT_INACTIVE") -> AppError.AccountInactive
            upper.contains("ALREADY_IN_RT") -> AppError.AlreadyInRt
            upper.contains("NOT_IN_RT") -> AppError.NotInRt
            upper.contains("HAS_PENDING_REQUEST") -> AppError.HasPendingRequest
            upper.contains("FIELD_INVALID") -> AppError.FieldInvalid
            upper.contains("USERNAME_INVALID") -> AppError.UsernameInvalid
            upper.contains("USERNAME_TAKEN") -> AppError.UsernameTaken
            upper.contains("RATE_LIMIT_CREATE_RT") -> AppError.RateLimitCreateRt
            upper.contains("RATE_LIMIT_POST") -> AppError.RateLimitPost
            upper.contains("RT_NOT_FOUND") -> AppError.RtNotFound
            upper.contains("RT_FULL") -> AppError.RtFull
            upper.contains("REQUEST_REJECTED_RECENTLY") -> AppError.RequestRejectedRecently
            upper.contains("REQUEST_NOT_FOUND") -> AppError.RequestNotFound
            upper.contains("REQUEST_ALREADY_DECIDED") -> AppError.RequestAlreadyDecided
            upper.contains("APPLICANT_UNAVAILABLE") -> AppError.ApplicantUnavailable
            upper.contains("NOT_ADMIN") -> AppError.NotAdmin
            upper.contains("ADMIN_MUST_TRANSFER") -> AppError.AdminMustTransfer
            upper.contains("TRANSFER_TO_SELF") -> AppError.TransferToSelf
            upper.contains("MEMBER_NOT_FOUND") -> AppError.MemberNotFound
            upper.contains("CANNOT_REMOVE_SELF") -> AppError.CannotRemoveSelf
            upper.contains("CATEGORY_NOT_FOUND") -> AppError.CategoryNotFound
            upper.contains("CATEGORY_ARCHIVED") -> AppError.CategoryArchived
            upper.contains("NOT_BENDAHARA") -> AppError.NotBendahara
            upper.contains("NOT_ALLOWED") -> AppError.NotAllowed
            upper.contains("POST_NOT_FOUND") -> AppError.PostNotFound
            upper.contains("REPORT_CANNOT_DELETE") -> AppError.ReportCannotDelete
            upper.contains("TRANSACTION_NOT_FOUND") -> AppError.TransactionNotFound
            upper.contains("TRANSACTION_LOCKED") -> AppError.TransactionLocked
            upper.contains("ALREADY_PUBLISHED") -> AppError.AlreadyPublished
            upper.contains("RECAP_ALREADY_PUBLISHED") -> AppError.RecapAlreadyPublished
            upper.contains("MONTH_NOT_ENDED") -> AppError.MonthNotEnded
            upper.contains("MONTH_ALREADY_PUBLISHED") -> AppError.MonthAlreadyPublished
            upper.contains("NO_TRANSACTIONS") -> AppError.NoTransactions
            upper.contains("DATE_IN_FUTURE") -> AppError.DateInFuture
            upper.contains("CORRECTION_INVALID") -> AppError.CorrectionInvalid
            upper.contains("RATE_LIMIT_LOOKUP") -> AppError.RateLimitLookup
            upper.contains("NOT_AUTHORIZED") -> AppError.NotAllowed
            upper.contains("DUPLICATE_NAME") -> AppError.DuplicateName
            upper.contains("INVALID_NAME") -> AppError.InvalidName
            upper.contains("NO_RT") -> AppError.NotInRt
            else -> AppError.Unknown(raw)
        }
    }

    private fun mapGenericThrowable(throwable: Throwable): AppError {
        val msg = throwable.message ?: return AppError.Unknown()
        val mapped = mapErrorCodeString(msg)
        return if (mapped is AppError.Unknown) AppError.Unknown(msg) else mapped
    }
}
