package com.kuniran.core.fcm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.kuniran.core.network.RegisterDeviceTokenRequest
import com.kuniran.core.network.SessionManager
import com.kuniran.core.network.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manager untuk registrasi token perangkat Firebase Cloud Messaging (FCM).
 *
 * Menggunakan mekanisme `device_tokens` di backend Supabase (bukan Firebase Topics).
 * Setiap perangkat mendaftarkan token uniknya melalui RPC `register_device_token`,
 * sehingga pengiriman push notification dapat difilter per-RT secara dinamis dan aman
 * di sisi server tanpa risiko kebocoran data ke mantan anggota RT.
 */
object FcmManager {
    private const val TAG = "FcmManager"

    // Coroutine Scope non-blocking untuk operasi IO di background, mencegah ANR
    private val fcmScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Memeriksa dan memastikan FirebaseApp telah terinisialisasi secara aman
     * sebelum mengakses FirebaseMessaging.
     */
    private fun isFirebaseAvailable(context: Context): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context) != null
            } else {
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp is not initialized or google-services.json is not configured: ${e.message}")
            false
        }
    }

    /**
     * Memeriksa apakah aplikasi memiliki izin notifikasi (Android 13 / Tiramisu+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Mengambil FCM registration token terbaru dari Firebase SDK dan mendaftarkannya
     * ke backend Supabase (tabel device_tokens) secara asinkron tanpa memblokir thread UI.
     */
    fun registerCurrentToken(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        val appContext = context.applicationContext
        val sessionManager = SessionManager(appContext)

        if (!isFirebaseAvailable(appContext)) {
            Log.w(TAG, "FCM registration skipped: FirebaseApp is not available or not yet initialized.")
            onComplete?.invoke(false)
            return
        }

        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrBlank()) {
                        Log.d(TAG, "FCM Token retrieved successfully: ${token.take(12)}...")
                        sessionManager.saveDeviceToken(token)

                        // Sinkronisasi ke backend Supabase menggunakan coroutine IO
                        fcmScope.launch {
                            val success = syncTokenToSupabase(sessionManager, token)
                            withContext(Dispatchers.Main) {
                                onComplete?.invoke(success)
                            }
                        }
                    } else {
                        Log.w(TAG, "Retrieved FCM Token was null or blank")
                        onComplete?.invoke(false)
                    }
                } else {
                    Log.w(TAG, "Fetching FCM token failed: ${task.exception?.message}", task.exception)
                    onComplete?.invoke(false)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseMessaging is not accessible: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    /**
     * Mengirim token perangkat ke stored procedure `register_device_token` di Supabase.
     */
    private suspend fun syncTokenToSupabase(sessionManager: SessionManager, token: String): Boolean {
        return try {
            val supabaseClient = SupabaseClient.getInstance(sessionManager)
            supabaseClient.apiService.registerDeviceToken(
                RegisterDeviceTokenRequest(token = token)
            )
            Log.d(TAG, "FCM token registered into Supabase device_tokens table")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync device token to Supabase: ${e.message}")
            false
        }
    }
}
