package com.kuniran.core.database

import androidx.room.TypeConverter
import com.kuniran.core.model.JoinStatus
import com.kuniran.core.model.PostType
import com.kuniran.core.model.TransactionType
import com.kuniran.core.model.UserRole

class DatabaseConverters {

    @TypeConverter
    fun toUserRole(value: String?): UserRole =
        value?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: UserRole.WARGA

    @TypeConverter
    fun fromUserRole(role: UserRole?): String = role?.name ?: UserRole.WARGA.name

    @TypeConverter
    fun toPostType(value: String?): PostType =
        value?.let { runCatching { PostType.valueOf(it) }.getOrNull() } ?: PostType.PENGUMUMAN

    @TypeConverter
    fun fromPostType(type: PostType?): String = type?.name ?: PostType.PENGUMUMAN.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType =
        value?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() } ?: TransactionType.MASUK

    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String = type?.name ?: TransactionType.MASUK.name

    @TypeConverter
    fun toJoinStatus(value: String?): JoinStatus =
        value?.let { runCatching { JoinStatus.valueOf(it) }.getOrNull() } ?: JoinStatus.PENDING

    @TypeConverter
    fun fromJoinStatus(status: JoinStatus?): String = status?.name ?: JoinStatus.PENDING.name
}
