package com.kuniran.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

class SupabaseAuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = sessionManager.getAccessToken() ?: SupabaseConfig.anonKey

        val requestBuilder = originalRequest.newBuilder()
            .header("apikey", SupabaseConfig.anonKey)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")

        val response = chain.proceed(requestBuilder.build())

        // If response is 401 Unauthorized, attempt refresh token
        if (response.code == 401 && !sessionManager.getRefreshToken().isNullOrBlank()) {
            val refreshToken = sessionManager.getRefreshToken()
            if (!refreshToken.isNullOrBlank()) {
                val refreshed = attemptTokenRefresh(chain, refreshToken)
                if (refreshed != null) {
                    response.close()
                    // Retry original request with newly refreshed access token
                    val retryRequest = originalRequest.newBuilder()
                        .header("apikey", SupabaseConfig.anonKey)
                        .header("Authorization", "Bearer $refreshed")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build()
                    return chain.proceed(retryRequest)
                }
            }
        }

        // If still 401 and user was considered logged in, notify session expired
        if (response.code == 401 && sessionManager.isLoggedIn()) {
            Log.w("SupabaseAuthInterceptor", "Session expired (HTTP 401). Notifying SessionManager.")
            sessionManager.notifySessionExpired()
        }

        return response
    }

    private fun attemptTokenRefresh(chain: Interceptor.Chain, refreshToken: String): String? {
        return try {
            val refreshUrl = "${SupabaseConfig.baseUrl}auth/v1/token?grant_type=refresh_token"
            val payload = JSONObject().apply {
                put("refresh_token", refreshToken)
            }.toString()

            val request = Request.Builder()
                .url(refreshUrl)
                .header("apikey", SupabaseConfig.anonKey)
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val refreshResponse = chain.proceed(request)
            if (refreshResponse.isSuccessful) {
                val bodyStr = refreshResponse.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                val newAccessToken = json.optString("access_token", null)
                val newRefreshToken = json.optString("refresh_token", null)
                if (!newAccessToken.isNullOrBlank()) {
                    sessionManager.updateTokens(newAccessToken, newRefreshToken)
                    return newAccessToken
                }
            }
            null
        } catch (e: Exception) {
            Log.e("SupabaseAuthInterceptor", "Token refresh failed: ${e.message}")
            null
        }
    }
}
