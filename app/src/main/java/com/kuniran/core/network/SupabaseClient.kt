package com.kuniran.core.network

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Supabase client utility class initializing real database and RPC connectivity
 * using the project URL and ANON key configured from .env.example / .env / BuildConfig.
 */
class SupabaseClient private constructor(
    val sessionManager: SessionManager
) {
    val baseUrl: String = SupabaseConfig.baseUrl
    val anonKey: String = SupabaseConfig.anonKey
    val publishableKey: String = SupabaseConfig.publishableKey

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(SupabaseAuthInterceptor(sessionManager))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val apiService: SupabaseApiService by lazy {
        retrofit.create(SupabaseApiService::class.java)
    }

    /**
     * Tests real database connectivity by pinging the Supabase PostgREST root endpoint.
     * Returns Result.success(true) if connected and authenticated with the ANON key.
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.pingRoot()
            if (response.isSuccessful) {
                Log.d(TAG, "Supabase connection verified: HTTP ${response.code()}")
                true
            } else {
                Log.w(TAG, "Supabase ping returned code: ${response.code()}")
                // Even a 404/401 on some customized PostgREST setups indicates reachability,
                // but for live Supabase instances 200 (OpenAPI spec) is standard.
                response.code() in 200..299
            }
        }.recoverCatching { throwable ->
            // Fallback direct probe via OkHttp
            val request = Request.Builder()
                .url("${baseUrl}rest/v1/")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .get()
                .build()

            val callResponse = okHttpClient.newCall(request).execute()
            callResponse.use { resp ->
                Log.d(TAG, "Direct fallback probe: code=${resp.code}")
                resp.isSuccessful || resp.code == 200
            }
        }
    }

    /**
     * Executes a raw Postgres RPC Stored Procedure directly with custom JSON payload.
     */
    suspend fun executeRpcRaw(functionName: String, jsonPayload: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${baseUrl}rest/v1/rpc/$functionName"
            val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val token = sessionManager.getAccessToken() ?: anonKey

            val request = Request.Builder()
                .url(url)
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.use { resp ->
                val responseBody = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    responseBody
                } else {
                    throw Exception("RPC $functionName failed with HTTP ${resp.code}: $responseBody")
                }
            }
        }
    }

    companion object {
        private const val TAG = "SupabaseClient"

        @Volatile
        private var instance: SupabaseClient? = null

        fun getInstance(sessionManager: SessionManager): SupabaseClient {
            return instance ?: synchronized(this) {
                instance ?: SupabaseClient(sessionManager).also { instance = it }
            }
        }

        fun create(context: Context): SupabaseClient {
            val sessionManager = SessionManager(context)
            return getInstance(sessionManager)
        }
    }
}
