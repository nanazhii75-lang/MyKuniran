package com.kuniran.core.model

data class WargaActivityLog(
    val id: String,
    val rtId: String,
    val wargaId: String,
    val residentName: String,
    val eventTitle: String,
    val eventLocation: String? = null,
    val timestamp: String
)
