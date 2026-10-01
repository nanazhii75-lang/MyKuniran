package com.kuniran

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.kuniran.app.AppContainer
import com.kuniran.core.fcm.FcmManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KuniranApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Initialize FirebaseApp safely
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.w("KuniranApp", "FirebaseApp initialization skipped: ${e.message}")
        }

        // Initialize and register FCM device token safely
        appScope.launch {
            runCatching {
                FcmManager.registerCurrentToken(this@KuniranApp)
            }
        }
    }
}
