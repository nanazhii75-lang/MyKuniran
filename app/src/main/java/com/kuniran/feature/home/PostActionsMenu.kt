package com.kuniran.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kuniran.R

/** Menu titik tiga untuk satu pos; tidak menampilkan apa pun bila tidak ada aksi yang boleh. */
@Composable
internal fun PostActionsMenu(
    canPin: Boolean,
    canDelete: Boolean,
    isPinned: Boolean,
    onPinClick: () -> Unit,
    onUnpinClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!canPin && !canDelete) return
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.post_menu_more),
                tint = MaterialTheme.colorScheme.outline
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (canPin) {
                if (isPinned) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.post_menu_unpin)) },
                        onClick = {
                            expanded = false
                            onUnpinClick()
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.post_menu_pin)) },
                        onClick = {
                            expanded = false
                            onPinClick()
                        }
                    )
                }
            }
            if (canDelete) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.post_menu_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        expanded = false
                        onDeleteClick()
                    }
                )
            }
        }
    }
}
