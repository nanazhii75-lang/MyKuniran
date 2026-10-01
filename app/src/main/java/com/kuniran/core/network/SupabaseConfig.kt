package com.kuniran.core.network

import com.kuniran.BuildConfig

/**
 * Konfigurasi hanya dibaca dari BuildConfig (diisi Secrets Gradle Plugin dari file .env;
 * di CI, .env dibuat dari GitHub Secrets). TIDAK ADA nilai cadangan tertanam di kode.
 * Jika belum diisi, aplikasi berhenti dengan pesan yang jelas.
 */
object SupabaseConfig {
    /** Link undangan sementara: kuniran://join/{invite_username} */
    const val INVITE_SCHEME = "kuniran"
    const val INVITE_HOST = "join"

    private val placeholder = Regex("your[-_]|placeholder", RegexOption.IGNORE_CASE)

    private fun required(name: String, raw: String?): String {
        val value = raw.orEmpty().trim()
        check(value.isNotEmpty() && !placeholder.containsMatchIn(value)) {
            "Konfigurasi $name belum diisi. Isi file .env (lihat .env.example) atau GitHub Secrets."
        }
        return value
    }

    val baseUrl: String
        get() {
            val url = required("SUPABASE_URL", BuildConfig.SUPABASE_URL)
            return if (url.endsWith("/")) url else "$url/"
        }

    val anonKey: String
        get() = required("SUPABASE_ANON_KEY", BuildConfig.SUPABASE_ANON_KEY)

    val publishableKey: String
        get() = required("SUPABASE_PUBLISHABLE_KEY", BuildConfig.SUPABASE_PUBLISHABLE_KEY)

    val googleWebClientId: String
        get() = required("GOOGLE_WEB_CLIENT_ID", BuildConfig.GOOGLE_WEB_CLIENT_ID)

    val googleAndroidClientId: String
        get() = required("GOOGLE_ANDROID_CLIENT_ID", BuildConfig.GOOGLE_ANDROID_CLIENT_ID)
}
