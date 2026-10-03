package com.kuniran.feature.attendance

import android.net.Uri

/**
 * Isi QR presensi: kuniran://presensi?rt=<id RT>&t=<nama kegiatan>&l=<lokasi>
 * ID RT ikut tertanam supaya QR dari RT lain ditolak saat dipindai.
 */
data class AttendanceQrPayload(
    val rtId: String,
    val title: String,
    val location: String?
) {
    fun encode(): String {
        val builder = Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST)
            .appendQueryParameter("rt", rtId)
            .appendQueryParameter("t", title.trim().take(MAX_FIELD))
        val loc = location?.trim()?.take(MAX_FIELD)
        if (!loc.isNullOrBlank()) builder.appendQueryParameter("l", loc)
        return builder.build().toString()
    }

    companion object {
        const val SCHEME = "kuniran"
        const val HOST = "presensi"
        const val MAX_FIELD = 80

        /** Mengembalikan null jika teks bukan QR presensi Kuniran (QR lama/biasa tetap ditangani pemanggil). */
        fun decode(raw: String): AttendanceQrPayload? {
            val uri = runCatching { Uri.parse(raw.trim()) }.getOrNull() ?: return null
            if (uri.scheme != SCHEME || uri.host != HOST) return null
            val rt = uri.getQueryParameter("rt")?.trim()?.takeIf { it.isNotBlank() } ?: return null
            val title = uri.getQueryParameter("t")?.trim()?.takeIf { it.isNotBlank() } ?: return null
            val loc = uri.getQueryParameter("l")?.trim()?.takeIf { it.isNotBlank() }
            return AttendanceQrPayload(rt, title.take(MAX_FIELD), loc?.take(MAX_FIELD))
        }
    }
}
