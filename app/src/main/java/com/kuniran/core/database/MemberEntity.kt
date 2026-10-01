package com.kuniran.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuniran.core.model.RtMember
import com.kuniran.core.model.UserRole

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val avatarPath: String?,
    val houseInfo: String?,
    val phoneNumber: String?,
    val role: UserRole?,
    val isMember: Boolean
) {
    fun toDomain(): RtMember = RtMember(
        id = id,
        fullName = fullName,
        avatarPath = avatarPath,
        houseInfo = houseInfo,
        phoneNumber = phoneNumber,
        role = role,
        isMember = isMember
    )

    companion object {
        fun fromDomain(m: RtMember): MemberEntity = MemberEntity(
            id = m.id,
            fullName = m.fullName,
            avatarPath = m.avatarPath,
            houseInfo = m.houseInfo,
            phoneNumber = m.phoneNumber,
            role = m.role,
            isMember = m.isMember
        )
    }
}
