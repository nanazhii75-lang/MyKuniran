package com.kuniran.domain.usecase

import com.kuniran.core.common.Resource
import com.kuniran.core.model.Warga
import com.kuniran.domain.repository.WargaRepository
import kotlinx.coroutines.flow.Flow

class GetWargaListUseCase(
    private val wargaRepository: WargaRepository
) {
    operator fun invoke(rtId: String): Flow<Resource<List<Warga>>> {
        return wargaRepository.getWargaList(rtId)
    }
}
