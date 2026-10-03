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

    private fun parseUtc(isoString: String?): Date? {
        if (isoString.isNullOrBlank()) return null
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            parser.parse(isoString.substringBefore(".").substringBefore("+").substringBefore("Z"))
        } catch (_: Exception) {
            null
        }
    }

    /** Waktu ala media sosial: Baru saja, 5 menit lalu, 2 jam lalu, Kemarin, 3 hari lalu, lalu tanggal. */
    fun formatRelative(isoString: String?): String {
        val date = parseUtc(isoString) ?: return formatWibDate(isoString)
        val diffSec = (System.currentTimeMillis() - date.time) / 1000
        return when {
            diffSec < 60 -> "Baru saja"
            diffSec < 3_600 -> "${diffSec / 60} menit lalu"
            diffSec < 86_400 -> "${diffSec / 3_600} jam lalu"
            diffSec < 172_800 -> "Kemarin"
            diffSec < 604_800 -> "${diffSec / 86_400} hari lalu"
            else -> formatShortDate(isoString)
        }
    }

    /** true bila waktu itu masih di masa depan (dipakai untuk masa berlaku sematan). */
    fun isFuture(isoString: String?): Boolean = parseUtc(isoString)?.after(Date()) ?: false

    /** true bila waktu itu jatuh pada tanggal hari ini menurut WIB. */
    fun isTodayWib(isoString: String?): Boolean {
        val date = parseUtc(isoString) ?: return false
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = WIB_TIMEZONE }
        return fmt.format(date) == fmt.format(Date())
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
