package com.kuniran.core.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.kuniran.core.common.ImageKind
import com.kuniran.core.common.ImageMagicBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Mengubah foto pilihan warga menjadi JPEG kecil sebelum diunggah:
 * sisi terpanjang maksimal 1280 px, kualitas 80, dan semua metadata (termasuk lokasi GPS)
 * hilang karena gambar dibuat ulang dari bitmap. Orientasi EXIF diterapkan ke piksel.
 *
 * Jenis berkas dikenali dari magic bytes (bukan ekstensi/MIME), dan hasil akhir diperiksa
 * lagi: wajib benar-benar JPEG sebelum dikirim ke server.
 */
class PostImageProcessor(private val contentResolver: ContentResolver) {

    /** null bila foto tidak bisa dibaca atau tetap terlalu besar setelah dikompres; alasannya masuk Logcat (tag PostImage). */
    suspend fun toJpeg(uri: Uri): ByteArray? = withContext(Dispatchers.Default) {
        try {
            process(uri)
        } catch (e: Throwable) {
            Log.e(TAG, "Gagal memproses foto: ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    private fun process(uri: Uri): ByteArray? {
        // 1) Kenali jenis berkas dari byte awal
        val header = ByteArray(ImageMagicBytes.HEADER_SIZE)
        val read = contentResolver.openInputStream(uri)?.use { it.read(header) }
        if (read == null || read <= 0) {
            Log.w(TAG, "Berkas tidak bisa dibuka atau kosong")
            return null
        }
        val kind = ImageMagicBytes.sniff(header)
        if (kind == ImageKind.UNKNOWN) {
            Log.w(TAG, "Magic bytes bukan gambar yang didukung")
            return null
        }

        // 2) Baca ukuran saja. Dengan inJustDecodeBounds=true decodeStream SELALU mengembalikan
        //    null (dokumentasi resmi Android), jadi hasilnya TIDAK boleh dipakai sebagai penanda gagal.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val opened = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
            true
        } ?: false
        if (!opened || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            Log.w(TAG, "Ukuran gambar tidak terbaca (jenis=$kind)")
            return null
        }

        val orientation = try {
            contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL   // PNG/WebP tanpa EXIF tetap bisa diproses
        }

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        if (decoded == null) {
            Log.w(TAG, "Dekode gagal (jenis=$kind, ${bounds.outWidth}x${bounds.outHeight})")
            return null
        }

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
                if (out.size() <= MAX_BYTES) {
                    val bytes = out.toByteArray()
                    // Pemeriksaan akhir: yang dikirim ke server harus benar-benar JPEG
                    if (!ImageMagicBytes.isJpeg(bytes)) {
                        Log.e(TAG, "Hasil kompres bukan JPEG (magic bytes tidak cocok)")
                        return null
                    }
                    return bytes
                }
            }
            Log.w(TAG, "Masih lebih dari $MAX_BYTES byte setelah kompres kualitas terendah")
            return null
        } finally {
            flat.recycle()
        }
    }

    private companion object {
        const val TAG = "PostImage"
        const val MAX_SIDE = 1280
        const val MAX_BYTES = 900_000          // batas bucket 1 MB (1.048.576)
        val QUALITIES = intArrayOf(80, 70, 60, 50)
    }
}
