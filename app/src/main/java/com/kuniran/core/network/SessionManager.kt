package com.kuniran.core.network

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kuniran_auth_session", Context.MODE_PRIVATE)

    private val _currentUserId = MutableStateFlow(prefs.getString(KEY_USER_ID, null))
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _currentRtId = MutableStateFlow(prefs.getString(KEY_RT_ID, null))
    val currentRtId: StateFlow<String?> = _currentRtId.asStateFlow()

    private val _sessionExpiredFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredFlow: SharedFlow<Unit> = _sessionExpiredFlow.asSharedFlow()

    fun saveSession(
        accessToken: String,
        refreshToken: String? = null,
        userId: String,
        rtId: String? = null,
        role: String? = null
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply {
                if (refreshToken != null) putString(KEY_REFRESH_TOKEN, refreshToken)
            }
            .putString(KEY_USER_ID, userId)
            .putString(KEY_RT_ID, rtId)
            .putString(KEY_USER_ROLE, role)
            .apply()
        _currentUserId.value = userId
        _currentRtId.value = rtId
    }

    fun updateTokens(accessToken: String, refreshToken: String? = null) {
        val editor = prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken)
        if (refreshToken != null) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        editor.apply()
    }

    fun updateRtId(rtId: String?, role: String? = null) {
        prefs.edit()
            .putString(KEY_RT_ID, rtId)
            .putString(KEY_USER_ROLE, role)
            .apply()
        _currentRtId.value = rtId
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getRtId(): String? = prefs.getString(KEY_RT_ID, null)

    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)

    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank() && !getUserId().isNullOrBlank()

    fun clearSession() {
        prefs.edit().clear().apply()
        _currentUserId.value = null
        _currentRtId.value = null
    }

    fun notifySessionExpired() {
        clearSession()
        _sessionExpiredFlow.tryEmit(Unit)
    }

    fun saveDeviceToken(token: String) {
        prefs.edit().putString(KEY_DEVICE_TOKEN, token).apply()
    }

    fun getDeviceToken(): String? = prefs.getString(KEY_DEVICE_TOKEN, null)

    companion object {
        private const val KEY_ACCESS_TOKEN = "key_access_token"
        private const val KEY_REFRESH_TOKEN = "key_refresh_token"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_RT_ID = "key_rt_id"
        private const val KEY_USER_ROLE = "key_user_role"
        private const val KEY_DEVICE_TOKEN = "key_device_token"
    }
}
