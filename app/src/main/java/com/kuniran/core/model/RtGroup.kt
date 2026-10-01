package com.kuniran.core.model

data class RtGroup(
    val id: String,
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
)
