package com.kuniran.core.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Mengubah foto pilihan warga menjadi JPEG kecil sebelum diunggah:
 * sisi terpanjang maksimal 1280 px, kualitas 80, dan semua metadata (termasuk lokasi GPS)
 * hilang karena gambar dibuat ulang dari bitmap. Orientasi EXIF diterapkan ke piksel.
 */
class PostImageProcessor(private val contentResolver: ContentResolver) {

    /** null bila foto tidak bisa dibaca atau tetap terlalu besar setelah dikompres. */
    suspend fun toJpeg(uri: Uri): ByteArray? = withContext(Dispatchers.Default) {
        runCatching { process(uri) }.getOrNull()
    }

    private fun process(uri: Uri): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val orientation = contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val longest = max(decoded.width, decoded.height)
        val scale = if (longest > MAX_SIDE) MAX_SIDE.toFloat() / longest else 1f
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(-90f)
            }
            if (scale < 1f) postScale(scale, scale)
        }

        val transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (transformed !== decoded) decoded.recycle()

        // JPEG tidak punya transparansi: gambar di atas latar putih agar tidak jadi hitam
        val flat = if (transformed.hasAlpha()) {
            Bitmap.createBitmap(transformed.width, transformed.height, Bitmap.Config.ARGB_8888).also {
                Canvas(it).apply {
                    drawColor(Color.WHITE)
                    drawBitmap(transformed, 0f, 0f, null)
                }
                transformed.recycle()
            }
        } else transformed

        try {
            for (quality in QUALITIES) {
                val out = ByteArrayOutputStream()
                flat.compress(Bitmap.CompressFormat.JPEG, quality, out)
                if (out.size() <= MAX_BYTES) return out.toByteArray()
            }
            return null
        } finally {
            flat.recycle()
        }
    }

    private companion object {
        const val MAX_SIDE = 1280
        const val MAX_BYTES = 900_000          // batas bucket 1 MB (1.048.576)
        val QUALITIES = intArrayOf(80, 70, 60, 50)
    }
}
