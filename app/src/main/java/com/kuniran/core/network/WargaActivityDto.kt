package com.kuniran.core.network

import com.kuniran.core.common.DateTimeUtils
import com.kuniran.core.model.WargaActivityLog
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WargaActivityDto(
    @Json(name = "id") val id: String,
    @Json(name = "rt_id") val rtId: String,
    @Json(name = "warga_id") val wargaId: String,
    @Json(name = "resident_name") val residentName: String,
    @Json(name = "event_title") val eventTitle: String,
    @Json(name = "event_location") val eventLocation: String? = null,
    @Json(name = "scanned_at") val scannedAt: String? = null
) {
    fun toDomain(): WargaActivityLog = WargaActivityLog(
        id = id,
        rtId = rtId,
        wargaId = wargaId,
        residentName = residentName,
        eventTitle = eventTitle,
        eventLocation = eventLocation,
        timestamp = DateTimeUtils.formatWibDate(scannedAt)
    )
}
