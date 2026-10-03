package com.kuniran.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kuniran.R

/** Tab Menu: pintasan fitur sebagai daftar baris ringkas (ikon + judul). */
@Composable
internal fun SettingsMenuTab(
    hasRt: Boolean,
    onNavigateHelpFaq: () -> Unit,
    onNavigateResidentDirectory: () -> Unit,
    onNavigateFinanceDashboard: () -> Unit,
    onNavigateCalendarRsvp: () -> Unit,
    onNavigateForum: () -> Unit,
    onNavigateQrScanner: () -> Unit
) {
    SettingsMenuRow(
        icon = Icons.Default.HelpOutline,
        title = stringResource(R.string.settings_btn_help_faq),
        onClick = onNavigateHelpFaq,
        modifier = Modifier.testTag("btn_open_help_faq")
    )

    if (hasRt) {
        SettingsMenuRow(
            icon = Icons.Default.Group,
            title = stringResource(R.string.settings_btn_resident_directory),
            onClick = onNavigateResidentDirectory,
            modifier = Modifier.testTag("btn_open_resident_directory")
        )
        SettingsMenuRow(
            icon = Icons.Default.Payments,
            title = stringResource(R.string.settings_btn_finance_dashboard),
            onClick = onNavigateFinanceDashboard,
            modifier = Modifier.testTag("btn_open_finance_dashboard")
        )
        SettingsMenuRow(
            icon = Icons.Default.CalendarToday,
            title = stringResource(R.string.settings_btn_calendar_rsvp),
            onClick = onNavigateCalendarRsvp,
            modifier = Modifier.testTag("btn_open_calendar_rsvp")
        )
        SettingsMenuRow(
            icon = Icons.Default.Forum,
            title = stringResource(R.string.settings_btn_forum),
            onClick = onNavigateForum,
            modifier = Modifier.testTag("btn_open_forum")
        )
        SettingsMenuRow(
            icon = Icons.Default.QrCodeScanner,
            title = stringResource(R.string.settings_btn_qr_scanner),
            onClick = onNavigateQrScanner,
            modifier = Modifier.testTag("btn_open_qr_scanner")
        )
    }
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
