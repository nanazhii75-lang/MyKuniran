package com.kuniran.feature.help

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class FaqItem(
    val id: String,
    @StringRes val questionRes: Int,
    @StringRes val answerRes: Int
)

data class FaqSection(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val icon: ImageVector,
    val items: List<FaqItem>
)
