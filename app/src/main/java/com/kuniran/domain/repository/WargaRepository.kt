package com.kuniran.domain.repository

import com.kuniran.core.common.Resource
import com.kuniran.core.model.Warga
import com.kuniran.core.model.WargaActivityLog
import kotlinx.coroutines.flow.Flow

interface WargaRepository {
    fun getWargaList(rtId: String): Flow<Resource<List<Warga>>>
    fun getWargaById(id: String): Flow<Resource<Warga>>
    suspend fun createWarga(warga: Warga): Resource<Unit>
    suspend fun updateWarga(id: String, updates: Map<String, Any?>): Resource<Unit>
    suspend fun deleteWarga(id: String): Resource<Unit>
    suspend fun logAttendance(
        rtId: String,
        wargaId: String,
        residentName: String,
        eventTitle: String,
        eventLocation: String?
    ): Resource<Unit>
    fun getAttendanceHistory(wargaId: String): Flow<Resource<List<WargaActivityLog>>>
}
