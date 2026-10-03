package com.kuniran.feature.attendance

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kuniran.R

@Composable
fun AttendanceQrGeneratorDialog(
    rtId: String,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var qrContent by remember { mutableStateOf<String?>(null) }
    val qrBitmap = remember(qrContent) { qrContent?.let { QrBitmapGenerator.generate(it) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.qr_generator_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.qr_generator_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it.take(AttendanceQrPayload.MAX_FIELD)
                        qrContent = null
                    },
                    label = { Text(stringResource(R.string.qr_scanner_event_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_qr_event_name")
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = {
                        location = it.take(AttendanceQrPayload.MAX_FIELD)
                        qrContent = null
                    },
                    label = { Text(stringResource(R.string.qr_scanner_event_location)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_qr_event_location")
                )
                Button(
                    onClick = {
                        qrContent = AttendanceQrPayload(
                            rtId = rtId,
                            title = title,
                            location = location.ifBlank { null }
                        ).encode()
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("btn_generate_qr")
                ) {
                    Text(stringResource(R.string.qr_generator_btn_create))
                }
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = stringResource(R.string.qr_generator_title),
                            modifier = Modifier.size(240.dp).testTag("img_attendance_qr")
                        )
                    }
                    Text(
                        text = stringResource(R.string.qr_generator_show_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_qr_generator")) {
                Text(stringResource(R.string.qr_generator_btn_close))
            }
        }
    )
}
