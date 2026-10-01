package com.kuniran.core.model

enum class RsvpStatus {
    HADIR,
    TIDAK_HADIR,
    RAGU
}

data class PostRsvp(
    val id: String,
    val postId: String,
    val userId: String,
    val rtId: String,
    val status: RsvpStatus,
    val createdAt: String
)

data class AgendaRsvpSummary(
    val hadirCount: Int = 0,
    val tidakHadirCount: Int = 0,
    val raguCount: Int = 0,
    val userStatus: RsvpStatus? = null
)
