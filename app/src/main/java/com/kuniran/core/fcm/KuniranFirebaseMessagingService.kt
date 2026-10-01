package com.kuniran.core.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kuniran.MainActivity
import com.kuniran.R
import com.kuniran.core.network.RegisterDeviceTokenRequest
import com.kuniran.core.network.SessionManager
import com.kuniran.core.network.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KuniranFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received")
        val sessionManager = SessionManager(applicationContext)
        sessionManager.saveDeviceToken(token)

        // Sync device token to Supabase RPC register_device_token
        serviceScope.launch {
            runCatching {
                val supabaseClient = SupabaseClient.getInstance(sessionManager)
                supabaseClient.apiService.registerDeviceToken(
                    RegisterDeviceTokenRequest(token = token)
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed to register FCM token with Supabase: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val category = data["category"] ?: "agenda"
        val title = data["title"] ?: remoteMessage.notification?.title ?: getString(R.string.fcm_new_agenda_title)
        val body = data["body"] ?: remoteMessage.notification?.body ?: getString(R.string.fcm_new_agenda_body)
        val location = data["location"]
        val time = data["time"]

        val displayContent = buildString {
            append(body)
            if (!location.isNullOrBlank()) {
                append("\nTempat: ").append(location)
            }
            if (!time.isNullOrBlank()) {
                append("\nWaktu: ").append(time)
            }
        }

        showCategorizedNotification(category, title, displayContent)
    }

    private fun showCategorizedNotification(category: String, title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val (channelId, channelName, channelDesc, priority) = when (category.lowercase()) {
            "urgent", "mendesak" -> Quadruple(
                CHANNEL_URGENT,
                getString(R.string.fcm_channel_urgent_name),
                getString(R.string.fcm_channel_urgent_desc),
                NotificationManager.IMPORTANCE_HIGH
            )
            "forum", "diskusi" -> Quadruple(
                CHANNEL_FORUM,
                getString(R.string.fcm_channel_forum_name),
                getString(R.string.fcm_channel_forum_desc),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            else -> Quadruple(
                CHANNEL_AGENDA,
                getString(R.string.fcm_channel_name),
                getString(R.string.fcm_channel_desc),
                NotificationManager.IMPORTANCE_HIGH
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, priority).apply {
                description = channelDesc
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dynamic notification ID to prevent overwrite
        val notificationId = (System.currentTimeMillis() % 100000).toInt()

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(if (priority == NotificationManager.IMPORTANCE_HIGH) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    companion object {
        private const val TAG = "KuniranFCM"
        const val CHANNEL_URGENT = "rt_urgent_channel"
        const val CHANNEL_AGENDA = "rt_agenda_channel"
        const val CHANNEL_FORUM = "rt_forum_channel"
    }
}
