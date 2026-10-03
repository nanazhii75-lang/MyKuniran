package com.kuniran.feature.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuniran.R
import com.kuniran.core.model.FinanceCategory
import com.kuniran.core.model.RtMember

/**
 * Pengurus RT memilih warga aktif RT ini sebagai bendahara satu pos kas.
 * Penegakan hak tetap di server (RPC assign_bendahara); dialog ini hanya antarmuka.
 */
@Composable
fun BendaharaPickerDialog(
    category: FinanceCategory,
    members: List<RtMember>,
    onDismiss: () -> Unit,
    onAssign: (profileId: String?) -> Unit
) {
    var selectedId by remember { mutableStateOf(category.bendaharaId) }
    val sorted = remember(members) { members.sortedBy { it.fullName.lowercase() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.finance_dialog_assign_bendahara_title, category.name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.finance_assign_bendahara_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (sorted.isEmpty()) {
                    Text(
                        text = stringResource(R.string.members_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .testTag("bendahara_member_list")
                    ) {
                        items(sorted, key = { it.id }) { m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedId = m.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedId == m.id,
                                    onClick = { selectedId = m.id }
                                )
                                Column {
                                    Text(text = m.fullName, style = MaterialTheme.typography.bodyLarge)
                                    m.houseInfo?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAssign(selectedId) },
                enabled = selectedId != null && selectedId != category.bendaharaId,
                modifier = Modifier.testTag("btn_save_bendahara")
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            Row {
                if (category.bendaharaId != null) {
                    TextButton(
                        onClick = { onAssign(null) },
                        modifier = Modifier.testTag("btn_remove_bendahara")
                    ) {
                        Text(stringResource(R.string.finance_btn_remove_bendahara))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        }
    )
}
