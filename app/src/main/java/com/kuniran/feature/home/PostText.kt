package com.kuniran.feature.home

import com.kuniran.core.model.Post

/**
 * Postingan dari kolom tulis menyimpan judul = baris pertama dan isi = seluruh teks.
 * Judul terpisah hanya ditampilkan kalau isi tidak diawali judul itu
 * (pengumuman lama, laporan kas, agenda).
 */
internal fun Post.showsSeparateTitle(): Boolean =
    title.isNotBlank() && !content.startsWith(title)
