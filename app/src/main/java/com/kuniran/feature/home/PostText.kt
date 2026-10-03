package com.kuniran.feature.home

import com.kuniran.core.model.Post

/** Potong tanpa memenggal pasangan surrogate (emoji) di tengah. */
internal fun String.takeSafe(max: Int): String {
    if (length <= max) return this
    val cut = if (this[max - 1].isHighSurrogate()) max - 1 else max
    return substring(0, cut)
}

/** Judul yang dibentuk kolom tulis: baris pertama isi, maksimal 100 karakter. */
internal fun titleFromContent(content: String): String =
    content.trim().lineSequence().first().takeSafe(100).trim()

/**
 * Pos dari kolom tulis berjudul sama persis dengan baris pertama isinya, jadi judulnya
 * tidak ditampilkan dua kali. Pos lain (pengumuman lama, laporan kas, agenda) tetap
 * menampilkan judul terpisah.
 */
internal fun Post.showsSeparateTitle(): Boolean =
    title.isNotBlank() && title != titleFromContent(content)
