package com.kuniran.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuniran.R
import com.kuniran.core.model.RtGroup

/** Tab Wilayah RT (khusus pengurus): data wilayah, persetujuan otomatis, nama pengenal. */
@Composable
internal fun SettingsRtTab(
    group: RtGroup,
    viewModel: SettingsViewModel,
    onEditRt: () -> Unit,
    onChangeUsername: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_rt_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = group.displayLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Nama Pengenal: @${group.inviteUsername}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_auto_approve),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.settings_auto_approve_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = group.autoApproveJoin,
                    onCheckedChange = { viewModel.setAutoApprove(it) },
                    modifier = Modifier.testTag("switch_auto_approve")
                )
            }

            OutlinedButton(
                onClick = onEditRt,
                modifier = Modifier.fillMaxWidth().testTag("btn_open_edit_rt")
            ) {
                Text(stringResource(R.string.settings_btn_edit_rt))
            }

            OutlinedButton(
                onClick = onChangeUsername,
                modifier = Modifier.fillMaxWidth().testTag("btn_open_change_username")
            ) {
                Text(stringResource(R.string.settings_btn_change_username))
            }
        }
    }
}
