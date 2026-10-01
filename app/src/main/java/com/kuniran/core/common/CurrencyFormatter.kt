package com.kuniran.core.common

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val localeId = Locale("id", "ID")

    fun formatRupiah(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(localeId)
        return "Rp " + formatter.format(amount)
    }

    fun parseRupiah(raw: String): Long {
        val clean = raw.replace("[^0-9]".toRegex(), "")
        return clean.toLongOrNull() ?: 0L
    }
}
