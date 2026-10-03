package com.kuniran.core.common

/** Pembersihan dan validasi nomor HP, selaras dengan aturan server (profiles.phone_number). */
object PhoneFormat {
    private val separators = Regex("[\\s\\-().]")
    private val valid = Regex("^\\+?[0-9]{8,15}$")

    fun normalize(raw: String): String = raw.trim().replace(separators, "")

    fun isValid(normalized: String): Boolean = valid.matches(normalized)
}
