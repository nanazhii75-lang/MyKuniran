package com.kuniran.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {
    private val WIB_TIMEZONE = TimeZone.getTimeZone("Asia/Jakarta")
    private val INDONESIA_LOCALE = Locale("id", "ID")

    fun formatWibDate(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "-"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val clean = isoString.substringBefore(".").substringBefore("+").substringBefore("Z")
            val date = parser.parse(clean) ?: return isoString
            val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", INDONESIA_LOCALE).apply {
                timeZone = WIB_TIMEZONE
            }
            formatter.format(date)
        } catch (_: Exception) {
            isoString
        }
    }

    fun formatShortDate(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "-"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val clean = isoString.substringBefore(".").substringBefore("+").substringBefore("Z")
            val date = parser.parse(clean) ?: return isoString
            val formatter = SimpleDateFormat("dd MMMM yyyy", INDONESIA_LOCALE).apply {
                timeZone = WIB_TIMEZONE
            }
            formatter.format(date)
        } catch (_: Exception) {
            isoString
        }
    }

    fun currentIsoTimestamp(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date())
    }

    fun currentWibMonthDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-01", Locale.US).apply {
            timeZone = WIB_TIMEZONE
        }
        return formatter.format(Date())
    }
}
