package com.kuniran.data.repository

import com.kuniran.core.common.ExceptionMapper
import com.kuniran.core.common.AppError
import com.kuniran.core.common.Resource
import com.kuniran.core.model.Warga
import com.kuniran.core.model.WargaActivityLog
import com.kuniran.core.network.SupabaseClient
import com.kuniran.core.network.WargaDto
import com.kuniran.domain.repository.WargaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class WargaRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : WargaRepository {

    private val apiService get() = supabaseClient.apiService

    override fun getWargaList(rtId: String): Flow<Resource<List<Warga>>> = flow {
        emit(Resource.Loading)
        val dtoList = try {
            val members = apiService.rtPeople()
            if (members.isNotEmpty()) {
                members.map {
                    WargaDto(
                        id = it.id,
                        rtId = rtId,
                        fullName = it.fullName,
                        phoneNumber = it.phoneNumber,
                        houseInfo = it.houseInfo,
                        houseBlock = it.houseBlock,
                        rtRole = it.role ?: "WARGA",
                        isActive = it.isMember ?: true,
                        createdAt = null
                    )
                }
            } else {
                apiService.getWargaList(rtIdFilter = "eq.$rtId")
            }
        } catch (_: Exception) {
            apiService.getWargaList(rtIdFilter = "eq.$rtId")
        }
        val domainList = dtoList.map { it.toDomain() }
        emit(Resource.Success(domainList))
    }.catch { e ->
        emit(Resource.Error(AppError.Unknown(e.localizedMessage)))
    }.flowOn(Dispatchers.IO)

    override fun getWargaById(id: String): Flow<Resource<Warga>> = flow {
        emit(Resource.Loading)
        val dtoList = apiService.getWargaById(idFilter = "eq.$id")
        val warga = dtoList.firstOrNull()?.toDomain()
        if (warga != null) {
            emit(Resource.Success(warga))
        } else {
            emit(Resource.Error(AppError.MemberNotFound))
        }
    }.catch { e ->
        emit(Resource.Error(AppError.Unknown(e.localizedMessage)))
    }.flowOn(Dispatchers.IO)

    override suspend fun createWarga(warga: Warga): Resource<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = mutableMapOf<String, Any?>(
                "rt_id" to warga.rtId,
                "full_name" to warga.fullName,
                "rt_role" to warga.rtRole,
                "is_head_of_family" to warga.isHeadOfFamily,
                "is_verified" to warga.isVerified,
                "is_active" to warga.isActive
            )
            warga.authUserId?.let { body["auth_user_id"] = it }
            warga.phoneNumber?.let { body["phone_number"] = it }
            warga.houseNumber?.let { body["house_number"] = it }
            warga.houseInfo?.let { body["house_info"] = it }
            warga.houseBlock?.let { body["house_block"] = it }
            warga.occupation?.let { body["occupation"] = it }
            warga.gender?.let { body["gender"] = it }

            val response = apiService.insertWarga(body)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(AppError.Unknown("HTTP ${response.code()}"))
            }
        }.getOrElse { e ->
            Resource.Error(AppError.Unknown(e.localizedMessage))
        }
    }

    override suspend fun updateWarga(id: String, updates: Map<String, Any?>): Resource<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.updateWarga(
                idFilter = "eq.$id",
                body = updates
            )
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(AppError.Unknown("HTTP ${response.code()}"))
            }
        }.getOrElse { e ->
            Resource.Error(AppError.Unknown(e.localizedMessage))
        }
    }

    override suspend fun deleteWarga(id: String): Resource<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.deleteWarga(idFilter = "eq.$id")
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(AppError.Unknown("HTTP ${response.code()}"))
            }
        }.getOrElse { e ->
            Resource.Error(AppError.Unknown(e.localizedMessage))
        }
    }

    // Server dulu: presensi dianggap tercatat hanya setelah server menerima.
    override suspend fun logAttendance(
        rtId: String,
        wargaId: String,
        residentName: String,
        eventTitle: String,
        eventLocation: String?
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = mutableMapOf<String, Any?>(
                "rt_id" to rtId,
                "warga_id" to wargaId,
                "resident_name" to residentName,
                "event_title" to eventTitle
            )
            if (!eventLocation.isNullOrBlank()) {
                body["event_location"] = eventLocation
            }
            val response = apiService.insertWargaActivity(body)
            if (!response.isSuccessful) {
                return@withContext Resource.Error(
                    ExceptionMapper.mapResponse(response.code(), runCatching { response.errorBody()?.string() }.getOrNull())
                )
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    // Riwayat dibaca dari server (satu sumber kebenaran), bukan dari daftar memori.
    override fun getAttendanceHistory(wargaId: String): Flow<Resource<List<WargaActivityLog>>> = flow {
        emit(Resource.Loading)
        try {
            val filter = if (wargaId.isEmpty()) null else "eq.$wargaId"
            val list = apiService.getWargaActivities(filter).map { it.toDomain() }
            emit(Resource.Success(list))
        } catch (e: Exception) {
            emit(Resource.Error(ExceptionMapper.map(e)))
        }
    }.flowOn(Dispatchers.IO)
}
