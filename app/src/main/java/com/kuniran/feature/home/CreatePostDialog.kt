package com.kuniran.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.kuniran.core.common.DateTimeUtils
import com.kuniran.core.model.PostType

@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, content: String, type: PostType, eventDate: String?, eventLocation: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PostType.PENGUMUMAN) }
    var location by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf<String>(DateTimeUtils.currentIsoTimestamp()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_create_post_title),
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
                        selected = selectedType == PostType.PENGUMUMAN,
                        onClick = { selectedType = PostType.PENGUMUMAN },
                        label = { Text(stringResource(R.string.post_type_pengumuman)) }
                    )
                    FilterChip(
                        selected = selectedType == PostType.AGENDA,
                        onClick = { selectedType = PostType.AGENDA },
                        label = { Text(stringResource(R.string.post_type_agenda)) }
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_post_title)) },
                    placeholder = { Text(stringResource(R.string.field_post_title_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_post_title"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.field_post_content)) },
                    placeholder = { Text(stringResource(R.string.field_post_content_hint)) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_post_content"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (selectedType == PostType.AGENDA) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text(stringResource(R.string.field_agenda_location)) },
                        placeholder = { Text(stringResource(R.string.field_agenda_location_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_agenda_location"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = eventDate,
                        onValueChange = { eventDate = it },
                        label = { Text(stringResource(R.string.field_agenda_time)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSubmit(
                            title,
                            content,
                            selectedType,
                            if (selectedType == PostType.AGENDA) eventDate else null,
                            if (selectedType == PostType.AGENDA) location else null
                        )
                    }
                },
                enabled = title.isNotBlank() && (selectedType != PostType.AGENDA || location.isNotBlank()),
                modifier = Modifier.testTag("btn_submit_post")
            ) {
                Text(stringResource(R.string.btn_publish_post))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
