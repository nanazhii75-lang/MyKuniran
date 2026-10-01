package com.kuniran.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuniran.core.model.RtGroup

@Entity(tableName = "rt_groups")
data class RtGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val rtNumber: String,
    val rwNumber: String,
    val desa: String,
    val dukuh: String,
    val lingkungan: String,
    val inviteUsername: String,
    val autoApproveJoin: Boolean,
    val createdBy: String?,
    val displayLabel: String,
    val createdAt: String,
    val updatedAt: String
) {
    fun toDomain(): RtGroup = RtGroup(
        id = id,
        name = name,
        rtNumber = rtNumber,
        rwNumber = rwNumber,
        desa = desa,
        dukuh = dukuh,
        lingkungan = lingkungan,
        inviteUsername = inviteUsername,
        autoApproveJoin = autoApproveJoin,
        createdBy = createdBy,
        displayLabel = displayLabel,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(g: RtGroup): RtGroupEntity = RtGroupEntity(
            id = g.id,
            name = g.name,
            rtNumber = g.rtNumber,
            rwNumber = g.rwNumber,
            desa = g.desa,
            dukuh = g.dukuh,
            lingkungan = g.lingkungan,
            inviteUsername = g.inviteUsername,
            autoApproveJoin = g.autoApproveJoin,
            createdBy = g.createdBy,
            displayLabel = g.displayLabel,
            createdAt = g.createdAt,
            updatedAt = g.updatedAt
        )
    }
}
