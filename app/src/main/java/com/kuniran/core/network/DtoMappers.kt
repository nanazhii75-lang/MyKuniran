package com.kuniran.core.network

import com.kuniran.core.model.FinanceCategory
import com.kuniran.core.model.FinanceReportMeta
import com.kuniran.core.model.FinanceTransaction
import com.kuniran.core.model.JoinRequest
import com.kuniran.core.model.JoinStatus
import com.kuniran.core.model.Post
import com.kuniran.core.model.PostType
import com.kuniran.core.model.RtGroup
import com.kuniran.core.model.RtMember
import com.kuniran.core.model.TransactionType
import com.kuniran.core.model.UserProfile
import com.kuniran.core.model.UserRole

fun ProfileDto.toDomain(): UserProfile = UserProfile(
    id = id,
    rtId = rtId,
    fullName = fullName,
    email = email,
    avatarPath = avatarPath,
    houseInfo = houseInfo,
    phoneNumber = phoneNumber,
    role = role?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: UserRole.WARGA,
    isActive = isActive ?: true,
    createdAt = createdAt ?: "",
    updatedAt = updatedAt ?: ""
)

fun RtGroupDto.toDomain(): RtGroup = RtGroup(
    id = id,
    name = name,
    rtNumber = rtNumber,
    rwNumber = rwNumber,
    desa = desa,
    dukuh = dukuh,
    lingkungan = lingkungan,
    inviteUsername = inviteUsername,
    autoApproveJoin = autoApproveJoin ?: false,
    createdBy = createdBy,
    displayLabel = displayLabel ?: "RT $rtNumber RW $rwNumber $lingkungan",
    createdAt = createdAt ?: "",
    updatedAt = updatedAt ?: ""
)

fun CategoryDto.toDomain(): FinanceCategory = FinanceCategory(
    id = id,
    rtId = rtId,
    name = name,
    description = description,
    bendaharaId = bendaharaId,
    isArchived = isArchived ?: false,
    createdAt = createdAt ?: "",
    updatedAt = updatedAt ?: ""
)

fun FinanceDto.toDomain(): FinanceTransaction = FinanceTransaction(
    id = id,
    rtId = rtId,
    categoryId = categoryId,
    title = title,
    contributorName = contributorName,
    note = note,
    amount = amount,
    type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.MASUK),
    proofPath = proofPath,
    correctsId = correctsId,
    createdBy = createdBy,
    transactionDate = transactionDate,
    isLocked = false,
    deletedAt = deletedAt,
    createdAt = createdAt ?: "",
    updatedAt = updatedAt ?: ""
)

fun PostDto.toDomain(): Post {
    val domainMeta = meta?.let {
        FinanceReportMeta(
            kind = it.kind ?: "",
            categoryName = it.categoryName ?: "",
            type = it.type?.let { t -> runCatching { TransactionType.valueOf(t) }.getOrNull() },
            amount = it.amount,
            contributorName = it.contributorName,
            transactionDate = it.transactionDate,
            masuk = it.masuk,
            keluar = it.keluar,
            saldoAwal = it.saldoAwal,
            saldoAkhir = it.saldoAkhir
        )
    }

    return Post(
        id = id,
        rtId = rtId,
        authorId = authorId,
        title = title,
        content = content,
        type = runCatching { PostType.valueOf(type) }.getOrDefault(PostType.PENGUMUMAN),
        eventDate = eventDate,
        eventLocation = eventLocation,
        financeRefId = financeRefId,
        categoryRefId = categoryRefId,
        recapMonth = recapMonth,
        meta = domainMeta,
        imagePath = imagePath,
        isPinned = isPinned ?: false,
        pinnedUntil = pinnedUntil,
        deletedAt = deletedAt,
        createdAt = createdAt ?: "",
        updatedAt = updatedAt ?: ""
    )
}

fun RtMemberDto.toDomain(): RtMember = RtMember(
    id = id,
    fullName = fullName,
    avatarPath = avatarPath,
    houseInfo = houseInfo,
    phoneNumber = phoneNumber,
    role = role?.let { runCatching { UserRole.valueOf(it) }.getOrNull() },
    isMember = isMember ?: true
)

fun JoinRequestDto.toDomain(): JoinRequest = JoinRequest(
    id = requestId,
    rtId = "",
    profileId = profileId,
    fullName = fullName,
    phoneNumber = phoneNumber,
    houseInfo = houseInfo,
    status = JoinStatus.PENDING,
    requestedAt = requestedAt,
    decidedAt = null,
    decidedBy = null
)
