package com.kuniran.data.repository

import com.kuniran.core.common.AppError
import com.kuniran.core.common.ExceptionMapper
import com.kuniran.core.common.Resource
import com.kuniran.core.database.AppDatabase
import com.kuniran.core.database.ProfileDao
import com.kuniran.core.database.ProfileEntity
import com.kuniran.core.model.UserProfile
import com.kuniran.core.model.UserRole
import com.kuniran.core.network.SessionManager
import com.kuniran.core.network.SupabaseApiService
import com.kuniran.core.network.toDomain
import com.kuniran.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val apiService: SupabaseApiService,
    private val profileDao: ProfileDao,
    private val sessionManager: SessionManager,
    private val database: AppDatabase
) : AuthRepository {

    // Mengikuti perubahan sesi: saat login/logout, profil yang diamati ikut berganti.
    // Sebelumnya uid dibaca sekali ("" saat belum login) sehingga aliran tidak pernah
    // melihat profil akun yang baru login.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getCurrentUserFlow(): Flow<UserProfile?> =
        sessionManager.currentUserId.flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                kotlinx.coroutines.flow.flowOf(null)
            } else {
                profileDao.getProfileFlow(uid).map { it?.toDomain() }
            }
        }

    override suspend fun getCurrentUser(): UserProfile? = withContext(Dispatchers.IO) {
        val uid = sessionManager.getUserId() ?: return@withContext null
        profileDao.getProfile(uid)?.toDomain()
    }

    override suspend fun signInWithGoogle(idToken: String): Resource<UserProfile> = withContext(Dispatchers.IO) {
        try {
            if (idToken.isBlank()) {
                return@withContext Resource.Error(AppError.LoginFailed("Token Google kosong"))
            }

            // Sesuai dokumentasi Supabase: provider=google + id_token. client_id tidak dikirim
            // (hanya relevan untuk jalur tanpa provider yang dinyatakan deprecated oleh Supabase).
            val authResponse = apiService.signInWithIdToken(
                mapOf("provider" to "google", "id_token" to idToken)
            )
            if (!authResponse.isSuccessful) {
                val body = runCatching { authResponse.errorBody()?.string() }.getOrNull()
                return@withContext Resource.Error(
                    AppError.LoginFailed(describeAuthFailure(authResponse.code(), body))
                )
            }
            val authData = authResponse.body()
                ?: return@withContext Resource.Error(AppError.LoginFailed("Respons server kosong"))
            val userId = authData.user?.id
                ?: return@withContext Resource.Error(AppError.LoginFailed("Respons server tanpa data pengguna"))

            // Isolasi RT: cache lokal milik akun/RT sebelumnya tidak boleh terbawa ke akun ini.
            database.clearAllTables()

            // Sesi hanya disimpan dari token asli milik server. Tidak ada token cadangan.
            sessionManager.saveSession(
                accessToken = authData.accessToken,
                refreshToken = authData.refreshToken,
                userId = userId
            )

            // Profil dibuat server (trigger handle_new_user); selalu dibaca dari server.
            val dto = apiService.getProfile("eq.$userId").firstOrNull()
            if (dto == null) {
                sessionManager.clearSession()
                return@withContext Resource.Error(
                    AppError.LoginFailed("Profil pengguna tidak ditemukan di server")
                )
            }
            val profile = dto.toDomain()
            sessionManager.updateRtId(profile.rtId, profile.role.name)
            profileDao.insertProfile(ProfileEntity.fromDomain(profile))
            Resource.Success(profile)
        } catch (e: retrofit2.HttpException) {
            sessionManager.clearSession()
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            Resource.Error(AppError.LoginFailed(describeAuthFailure(e.code(), body)))
        } catch (e: Exception) {
            sessionManager.clearSession()
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    private fun describeAuthFailure(code: Int, body: String?): String {
        val detail = runCatching {
            val json = org.json.JSONObject(body.orEmpty())
            listOf("msg", "error_description", "message", "error")
                .firstNotNullOfOrNull { key -> json.optString(key, "").takeIf { it.isNotBlank() } }
        }.getOrNull()
        return "HTTP $code" + (detail?.let { ": $it" } ?: "")
    }

    override suspend fun signOut(): Resource<Unit> = withContext(Dispatchers.IO) {
        sessionManager.clearSession()
        database.clearAllTables()
        Resource.Success(Unit)
    }

    override suspend fun updateProfile(
        fullName: String,
        phoneNumber: String?,
        houseInfo: String?
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = sessionManager.getUserId()
                ?: return@withContext Resource.Error(com.kuniran.core.common.AppError.SessionExpired)
            val current = profileDao.getProfile(uid)?.toDomain()
                ?: return@withContext Resource.Error(com.kuniran.core.common.AppError.SessionExpired)

            // Server dulu: hanya field yang memang diubah user (email/avatar tidak dikirim).
            val body = mapOf(
                "full_name" to fullName,
                "phone_number" to phoneNumber,
                "house_info" to houseInfo
            )
            val response = apiService.updateProfile("eq.$uid", body)
            if (!response.isSuccessful) throw retrofit2.HttpException(response)

            // Cache lokal baru diperbarui setelah server menerima perubahan.
            val updated = current.copy(
                fullName = fullName,
                phoneNumber = phoneNumber,
                houseInfo = houseInfo,
                updatedAt = System.currentTimeMillis().toString()
            )
            profileDao.insertProfile(ProfileEntity.fromDomain(updated))

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }
}
