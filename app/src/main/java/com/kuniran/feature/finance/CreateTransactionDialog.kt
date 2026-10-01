package com.kuniran.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.kuniran.core.common.CurrencyFormatter
import com.kuniran.core.common.DateTimeUtils
import com.kuniran.core.model.TransactionType

@Composable
fun CreateTransactionDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        amount: Long,
        type: TransactionType,
        contributor: String?,
        note: String?,
        date: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.MASUK) }
    var contributor by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<String>(DateTimeUtils.currentIsoTimestamp()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.finance_btn_add_transaction),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == TransactionType.MASUK,
                        onClick = { selectedType = TransactionType.MASUK },
                        label = { Text(stringResource(R.string.finance_type_in)) }
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.KELUAR,
                        onClick = { selectedType = TransactionType.KELUAR },
                        label = { Text(stringResource(R.string.finance_type_out)) }
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.field_trans_amount)) },
                    placeholder = { Text(stringResource(R.string.field_trans_amount_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tx_amount"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_trans_title)) },
                    placeholder = { Text(stringResource(R.string.field_trans_title_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tx_title"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = contributor,
                    onValueChange = { contributor = it },
                    label = { Text(stringResource(R.string.field_trans_contributor)) },
                    placeholder = { Text(stringResource(R.string.field_trans_contributor_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tx_contributor"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.field_trans_note)) },
                    placeholder = { Text(stringResource(R.string.field_trans_note_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tx_note"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            val amt = CurrencyFormatter.parseRupiah(amountText)
            Button(
                onClick = {
                    if (title.isNotBlank() && amt > 0) {
                        onSubmit(
                            title,
                            amt,
                            selectedType,
                            contributor.ifBlank { null },
                            note.ifBlank { null },
                            date
                        )
                    }
                },
                enabled = title.isNotBlank() && amt > 0,
                modifier = Modifier.testTag("btn_submit_transaction")
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
