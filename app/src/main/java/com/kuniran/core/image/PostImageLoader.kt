package com.kuniran.core.image

import android.content.Context
import coil.ImageLoader
import com.kuniran.core.network.SessionManager
import com.kuniran.core.network.SupabaseConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Foto berada di bucket PRIVAT. Resmi (dokumentasi Supabase Storage): ambil lewat
 * GET /storage/v1/object/authenticated/{bucket}/{path} dengan header Authorization pengguna.
 * Header hanya ditambahkan untuk host Supabase kita sendiri.
 */
object PostImageLoader {
    private const val BUCKET = "post-images"

    fun url(imagePath: String): String =
        "${SupabaseConfig.baseUrl}storage/v1/object/authenticated/$BUCKET/$imagePath"

    fun create(context: Context, sessionManager: SessionManager): ImageLoader {
        val supabaseHost = SupabaseConfig.baseUrl.toHttpUrl().host
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.url.host != supabaseHost) {
                    chain.proceed(request)
                } else {
                    val token = sessionManager.getAccessToken() ?: SupabaseConfig.anonKey
                    chain.proceed(
                        request.newBuilder()
                            .header("apikey", SupabaseConfig.anonKey)
                            .header("Authorization", "Bearer $token")
                            .build()
                    )
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return ImageLoader.Builder(context)
            .okHttpClient(client)
            .crossfade(true)
            .build()
    }
}
