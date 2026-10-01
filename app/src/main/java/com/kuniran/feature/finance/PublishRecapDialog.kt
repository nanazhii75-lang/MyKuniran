package com.kuniran.feature.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuniran.R
import com.kuniran.core.common.DateTimeUtils

@Composable
fun PublishRecapDialog(
    categoryName: String,
    onDismiss: () -> Unit,
    onPublish: (monthDate: String) -> Unit
) {
    var monthDate by remember { mutableStateOf<String>(DateTimeUtils.currentWibMonthDate()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.finance_btn_publish_recap),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Terbitkan laporan rekapitulasi pembukuan pos $categoryName ke Kabar RT.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = monthDate,
                    onValueChange = { monthDate = it },
                    label = { Text("Bulan Rekap (YYYY-MM-01)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_recap_month")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(monthDate) },
                enabled = monthDate.isNotBlank(),
                modifier = Modifier.testTag("btn_confirm_publish_recap")
            ) {
                Text("Terbitkan Rekap")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
