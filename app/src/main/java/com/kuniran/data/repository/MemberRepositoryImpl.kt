package com.kuniran.data.repository

import com.kuniran.core.common.ExceptionMapper
import com.kuniran.core.common.Resource
import com.kuniran.core.database.MemberDao
import com.kuniran.core.database.MemberEntity
import com.kuniran.core.model.RtMember
import com.kuniran.core.network.AssignBendaharaRequest
import com.kuniran.core.network.RemoveMemberRequest
import com.kuniran.core.network.SupabaseApiService
import com.kuniran.core.network.TransferAdminRequest
import com.kuniran.core.network.toDomain
import com.kuniran.domain.repository.MemberRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MemberRepositoryImpl(
    private val apiService: SupabaseApiService,
    private val memberDao: MemberDao
) : MemberRepository {

    override fun getMembersFlow(): Flow<List<RtMember>> {
        return memberDao.getMembersFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun syncMembers(): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val list = apiService.rtPeople()
            val entities = list.map { MemberEntity.fromDomain(it.toDomain()) }
            memberDao.insertMembers(entities)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun transferAdmin(newAdminId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.transferAdmin(TransferAdminRequest(newAdminId))
            syncMembers()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun assignBendahara(categoryId: String, profileId: String?): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.assignBendahara(AssignBendaharaRequest(categoryId, profileId))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }

    override suspend fun removeMember(profileId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.removeMember(RemoveMemberRequest(profileId))
            syncMembers()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ExceptionMapper.map(e))
        }
    }
}
