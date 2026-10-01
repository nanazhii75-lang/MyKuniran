package com.kuniran.core.model

data class JoinRequest(
    val id: String,
    val rtId: String,
    val profileId: String,
    val fullName: String = "",
    val phoneNumber: String? = null,
    val houseInfo: String? = null,
    val status: JoinStatus,
    val requestedAt: String,
    val decidedAt: String?,
    val decidedBy: String?
)
