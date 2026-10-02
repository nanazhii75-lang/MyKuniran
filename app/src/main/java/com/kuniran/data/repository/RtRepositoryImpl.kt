package com.kuniran.data.repository

import com.kuniran.core.common.AppError
import com.kuniran.core.common.ExceptionMapper
import com.kuniran.core.common.Resource
import com.kuniran.core.database.ProfileDao
import com.kuniran.core.database.RtGroupDao
import com.kuniran.core.database.RtGroupEntity
import com.kuniran.core.model.JoinRequest
import com.kuniran.core.model.RtGroup
import com.kuniran.core.model.UserRole
import com.kuniran.core.network.ApproveJoinRequest
import com.kuniran.core.network.CheckUsernameRequest
import com.kuniran.core.network.CreateRtRequest
import com.kuniran.core.network.PreviewRtRequest
import com.kuniran.core.network.RejectJoinRequest
import com.kuniran.core.network.RequestJoinRtRequest
import com.kuniran.core.network.SessionManager
import com.kuniran.core.network.SetAutoApproveRequest
import com.kuniran.core.network.SetInviteUsernameRequest
import com.kuniran.core.network.SupabaseApiService
import com.kuniran.core.network.UpdateRtInfoRequest
import com.kuniran.core.network.toDomain
import com.kuniran.domain.repository.RtRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RtRepositoryImpl(
    private val apiService: SupabaseApiService,
    private val rtGroupDao: RtGroupDao,
    private val profileDao: ProfileDao,
    private val sessionManager: SessionManager
) : RtRepository {

    override fun getRtGroupFlow(rtId: String): Flow<RtGroup?> {
        return rtGroupDao.getRtGroupFlow(rtId).map { it?.toDomain() }
    }

    override suspend fun fetchRtGroup(rtId: String): Resource<RtGroup> = withContext(Dispatchers.IO) {
        try {
            val list = apiService.getRtGroup("eq.$rtId")
            if (list.isNotEmpty()) {
                val group = list.first().toDomain()
                rtGroupDao.insertRtGroup(RtGroupEntity.fromDomain(group))
                Resource.Success(group)
            } else {
                val cached = rtGroupDao.getRtGroup(rtId)?.toDomain()
                if (cached != null) Resource.Success(cached)
                else Resource.Error(com.kuniran.core.common.AppError.RtNotFound)
            }
        } catch (e: Exception) {
            val cached = rtGroupDao.getRtGroup(rtId)?.toDomain()
            if (cached != null) Resource.Success(cached)
            else Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun createRt(
        name: String,
        rtNumber: String,
        rwNumber: String,
        desa: String,
        dukuh: String,
        lingkungan: String,
        inviteUsername: String
    ): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val req = CreateRtRequest(
                name = name,
                rtNumber = rtNumber,
                rwNumber = rwNumber,
                desa = desa,
                dukuh = dukuh,
                lingkungan = lingkungan,
                inviteUsername = inviteUsername
            )
            val newRtId = apiService.createRt(req)
            sessionManager.updateRtId(newRtId, UserRole.ADMIN_RT.name)

            // Update local profile
            val uid = sessionManager.getUserId()
            if (uid != null) {
                val profile = profileDao.getProfile(uid)
                if (profile != null) {
                    profileDao.insertProfile(
                        profile.copy(rtId = newRtId, role = UserRole.ADMIN_RT)
                    )
                }
            }

            // Fetch created RT info
            runCatching { fetchRtGroup(newRtId) }

            Resource.Success(newRtId)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun previewRt(inviteUsername: String): Resource<String?> = withContext(Dispatchers.IO) {
        try {
            val label = apiService.previewRt(PreviewRtRequest(inviteUsername))
            Resource.Success(label)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun requestJoinRt(inviteUsername: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val result = apiService.requestJoinRt(RequestJoinRtRequest(inviteUsername))
            if (result == "NOT_FOUND") {
                return@withContext Resource.Error(AppError.RtNotFound)
            }
            if (result == "APPROVED") {
                // Auto-approve: profil HARUS dibaca ulang dari server; kegagalan tidak ditelan
                val uid = sessionManager.getUserId()
                    ?: return@withContext Resource.Error(AppError.SessionExpired)
                val p = apiService.getProfile("eq.$uid").firstOrNull()?.toDomain()
                    ?: return@withContext Resource.Error(AppError.Network)
                sessionManager.updateRtId(p.rtId, p.role.name)
                profileDao.insertProfile(com.kuniran.core.database.ProfileEntity.fromDomain(p))
            }
            Resource.Success(result)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun cancelJoinRequest(): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.cancelJoinRequest()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun listPendingRequests(): Resource<List<JoinRequest>> = withContext(Dispatchers.IO) {
        try {
            val dtos = apiService.listPendingJoinRequests()
            Resource.Success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun approveJoinRequest(requestId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.approveJoinRequest(ApproveJoinRequest(requestId))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun rejectJoinRequest(requestId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.rejectJoinRequest(RejectJoinRequest(requestId))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun setAutoApprove(enabled: Boolean): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.setAutoApprove(SetAutoApproveRequest(enabled))
            val rtId = sessionManager.getRtId()
            if (rtId != null) {
                val group = rtGroupDao.getRtGroup(rtId)
                if (group != null) {
                    rtGroupDao.insertRtGroup(group.copy(autoApproveJoin = enabled))
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun checkUsernameAvailable(username: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val result = apiService.checkUsernameAvailable(CheckUsernameRequest(username))
            Resource.Success(result)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun setInviteUsername(username: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.setInviteUsername(SetInviteUsernameRequest(username))
            val rtId = sessionManager.getRtId()
            if (rtId != null) {
                val group = rtGroupDao.getRtGroup(rtId)
                if (group != null) {
                    rtGroupDao.insertRtGroup(group.copy(inviteUsername = username))
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun updateRtInfo(
        name: String,
        rtNumber: String,
        rwNumber: String,
        desa: String,
        dukuh: String,
        lingkungan: String
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = UpdateRtInfoRequest(
                name = name,
                rtNumber = rtNumber,
                rwNumber = rwNumber,
                desa = desa,
                dukuh = dukuh,
                lingkungan = lingkungan
            )
            apiService.updateRtInfo(req)
            val rtId = sessionManager.getRtId()
            if (rtId != null) {
                fetchRtGroup(rtId)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun leaveRt(): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.leaveRt()
            sessionManager.updateRtId(null, UserRole.WARGA.name)
            val uid = sessionManager.getUserId()
            if (uid != null) {
                val p = profileDao.getProfile(uid)
                if (p != null) {
                    profileDao.insertProfile(p.copy(rtId = null, role = UserRole.WARGA))
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }
}
