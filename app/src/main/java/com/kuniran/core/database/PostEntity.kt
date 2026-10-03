package com.kuniran.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuniran.core.model.FinanceReportMeta
import com.kuniran.core.model.Post
import com.kuniran.core.model.PostType
import com.kuniran.core.model.TransactionType

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val rtId: String,
    val authorId: String,
    val authorName: String = "",
    val authorAvatar: String? = null,
    val title: String,
    val content: String,
    val type: PostType,
    val eventDate: String?,
    val eventLocation: String?,
    val financeRefId: String?,
    val categoryRefId: String?,
    val recapMonth: String?,
    val metaKind: String? = null,
    val metaCategoryName: String? = null,
    val metaType: String? = null,
    val metaAmount: Long? = null,
    val metaContributorName: String? = null,
    val metaTransactionDate: String? = null,
    val metaMasuk: Long? = null,
    val metaKeluar: Long? = null,
    val metaSaldoAwal: Long? = null,
    val metaSaldoAkhir: Long? = null,
    val isPinned: Boolean,
    val pinnedUntil: String?,
    val deletedAt: String?,
    val createdAt: String,
    val updatedAt: String,
    val imagePath: String? = null
) {
    fun toDomain(): Post {
        val meta = if (metaKind != null) {
            FinanceReportMeta(
                kind = metaKind,
                categoryName = metaCategoryName ?: "",
                type = metaType?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() },
                amount = metaAmount,
                contributorName = metaContributorName,
                transactionDate = metaTransactionDate,
                masuk = metaMasuk,
                keluar = metaKeluar,
                saldoAwal = metaSaldoAwal,
                saldoAkhir = metaSaldoAkhir
            )
        } else null

        return Post(
            id = id,
            rtId = rtId,
            authorId = authorId,
            authorName = authorName,
            authorAvatar = authorAvatar,
            title = title,
            content = content,
            type = type,
            eventDate = eventDate,
            eventLocation = eventLocation,
            financeRefId = financeRefId,
            categoryRefId = categoryRefId,
            recapMonth = recapMonth,
            meta = meta,
            isPinned = isPinned,
            pinnedUntil = pinnedUntil,
            deletedAt = deletedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            imagePath = imagePath
        )
    }

    companion object {
        fun fromDomain(p: Post): PostEntity = PostEntity(
            id = p.id,
            rtId = p.rtId,
            authorId = p.authorId,
            authorName = p.authorName,
            authorAvatar = p.authorAvatar,
            title = p.title,
            content = p.content,
            type = p.type,
            eventDate = p.eventDate,
            eventLocation = p.eventLocation,
            financeRefId = p.financeRefId,
            categoryRefId = p.categoryRefId,
            recapMonth = p.recapMonth,
            metaKind = p.meta?.kind,
            metaCategoryName = p.meta?.categoryName,
            metaType = p.meta?.type?.name,
            metaAmount = p.meta?.amount,
            metaContributorName = p.meta?.contributorName,
            metaTransactionDate = p.meta?.transactionDate,
            metaMasuk = p.meta?.masuk,
            metaKeluar = p.meta?.keluar,
            metaSaldoAwal = p.meta?.saldoAwal,
            metaSaldoAkhir = p.meta?.saldoAkhir,
            isPinned = p.isPinned,
            pinnedUntil = p.pinnedUntil,
            deletedAt = p.deletedAt,
            createdAt = p.createdAt,
            updatedAt = p.updatedAt,
            imagePath = p.imagePath
        )
    }
}
