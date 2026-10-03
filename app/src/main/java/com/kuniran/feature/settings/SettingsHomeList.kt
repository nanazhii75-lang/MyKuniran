package com.kuniran.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.kuniran.R

/** Layar utama Pengaturan: daftar menu ringkas; tiap baris membuka layar bagiannya sendiri. */
@Composable
internal fun SettingsHomeList(
    hasRt: Boolean,
    showRt: Boolean,
    onOpenProfile: () -> Unit,
    onOpenRt: () -> Unit,
    onOpenAccount: () -> Unit,
    onNavigateHelpFaq: () -> Unit,
    onNavigateResidentDirectory: () -> Unit,
    onNavigateFinanceDashboard: () -> Unit,
    onNavigateCalendarRsvp: () -> Unit,
    onNavigateForum: () -> Unit,
    onNavigateQrScanner: () -> Unit
) {
    SettingsMenuRow(
        icon = Icons.Default.Person,
        title = stringResource(R.string.settings_home_profile),
        onClick = onOpenProfile,
        modifier = Modifier.testTag("btn_open_settings_profile")
    )
    if (showRt) {
        SettingsMenuRow(
            icon = Icons.Default.Share,
            title = stringResource(R.string.settings_home_rt),
            onClick = onOpenRt,
            modifier = Modifier.testTag("btn_open_settings_rt")
        )
    }
    SettingsMenuRow(
        icon = Icons.Default.AccountCircle,
        title = stringResource(R.string.settings_home_account),
        onClick = onOpenAccount,
        modifier = Modifier.testTag("btn_open_settings_account")
    )

    Text(
        text = stringResource(R.string.settings_home_features),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    SettingsMenuTab(
        hasRt = hasRt,
        onNavigateHelpFaq = onNavigateHelpFaq,
        onNavigateResidentDirectory = onNavigateResidentDirectory,
        onNavigateFinanceDashboard = onNavigateFinanceDashboard,
        onNavigateCalendarRsvp = onNavigateCalendarRsvp,
        onNavigateForum = onNavigateForum,
        onNavigateQrScanner = onNavigateQrScanner
    )
}
