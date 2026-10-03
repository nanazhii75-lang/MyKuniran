package com.kuniran.core.common

/**
 * Pengenal jenis gambar dari byte awal berkas (magic bytes), tanpa percaya nama file
 * atau tipe MIME dari perangkat. Dipakai sebelum foto diproses dan sebelum diunggah.
 */
enum class ImageKind { JPEG, PNG, GIF, WEBP, HEIC, UNKNOWN }

object ImageMagicBytes {
    /** Cukup 16 byte awal untuk semua format di bawah. */
    const val HEADER_SIZE = 16

    fun sniff(h: ByteArray): ImageKind {
        fun at(i: Int) = if (i < h.size) h[i].toInt() and 0xFF else -1
        fun ascii(from: Int, text: String) =
            text.indices.all { at(from + it) == text[it].code }

        return when {
            // JPEG: FF D8 FF
            at(0) == 0xFF && at(1) == 0xD8 && at(2) == 0xFF -> ImageKind.JPEG
            // PNG: 89 50 4E 47 0D 0A 1A 0A
            at(0) == 0x89 && ascii(1, "PNG") && at(4) == 0x0D && at(5) == 0x0A &&
                at(6) == 0x1A && at(7) == 0x0A -> ImageKind.PNG
            // GIF: "GIF87a" / "GIF89a"
            ascii(0, "GIF87a") || ascii(0, "GIF89a") -> ImageKind.GIF
            // WebP: "RIFF" + 4 byte ukuran + "WEBP"
            ascii(0, "RIFF") && ascii(8, "WEBP") -> ImageKind.WEBP
            // HEIC/HEIF: kotak "ftyp" di byte 4..7 lalu merek (heic, heix, mif1, msf1, hevc, ...)
            ascii(4, "ftyp") && HEIF_BRANDS.any { ascii(8, it) } -> ImageKind.HEIC
            else -> ImageKind.UNKNOWN
        }
    }

    fun isJpeg(bytes: ByteArray): Boolean = sniff(bytes) == ImageKind.JPEG

    private val HEIF_BRANDS = listOf("heic", "heix", "hevc", "hevx", "mif1", "msf1", "heim", "heis")
}
