package com.kuniran.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.kuniran.core.model.RtGroup

@Composable
fun EditRtInfoDialog(
    rtGroup: RtGroup,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        rtNumber: String,
        rwNumber: String,
        desa: String,
        dukuh: String,
        lingkungan: String
    ) -> Unit
) {
    var rtNumber by remember { mutableStateOf(rtGroup.rtNumber) }
    var rwNumber by remember { mutableStateOf(rtGroup.rwNumber) }
    var desa by remember { mutableStateOf(rtGroup.desa) }
    var dukuh by remember { mutableStateOf(rtGroup.dukuh) }
    var lingkungan by remember { mutableStateOf(rtGroup.lingkungan) }
    var name by remember { mutableStateOf(rtGroup.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_btn_edit_rt),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = rtNumber,
                        onValueChange = { rtNumber = it },
                        label = { Text(stringResource(R.string.field_rt_number)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_rt_num"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = rwNumber,
                        onValueChange = { rwNumber = it },
                        label = { Text(stringResource(R.string.field_rw_number)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_rw_num"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = desa,
                    onValueChange = { desa = it },
                    label = { Text(stringResource(R.string.field_desa)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_desa"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = dukuh,
                    onValueChange = { dukuh = it },
                    label = { Text(stringResource(R.string.field_dukuh)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_dukuh"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = lingkungan,
                    onValueChange = { lingkungan = it },
                    label = { Text(stringResource(R.string.field_lingkungan)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_lingkungan"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_rt_name_optional)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_label"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(name, rtNumber, rwNumber, desa, dukuh, lingkungan)
                },
                enabled = rtNumber.isNotBlank() && rwNumber.isNotBlank() && desa.isNotBlank() && dukuh.isNotBlank() && lingkungan.isNotBlank(),
                modifier = Modifier.testTag("btn_save_rt_info")
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
