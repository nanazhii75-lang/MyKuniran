package com.kuniran.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuniran.R
import com.kuniran.core.common.asText

/** Layar wajib setelah login: nama asli dan nomor HP (alamat opsional). */
@Composable
fun CompleteProfileScreen(
    viewModel: CompleteProfileViewModel,
    onDone: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var house by remember { mutableStateOf("") }

    LaunchedEffect(state.loaded) {
        if (state.loaded) {
            name = state.initialName
            phone = state.initialPhone
            house = state.initialHouse
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.complete_profile_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.complete_profile_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; viewModel.onInputChanged() },
                label = { Text(stringResource(R.string.field_profile_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("complete_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; viewModel.onInputChanged() },
                label = { Text(stringResource(R.string.field_profile_phone)) },
                singleLine = true,
                isError = state.phoneInvalid,
                supportingText = {
                    Text(
                        stringResource(
                            if (state.phoneInvalid) R.string.complete_profile_phone_error
                            else R.string.complete_profile_phone_note
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth().testTag("complete_phone_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = house,
                onValueChange = { house = it; viewModel.onInputChanged() },
                label = { Text(stringResource(R.string.complete_profile_house_optional)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("complete_house_input"),
                shape = RoundedCornerShape(12.dp)
            )

            state.error?.let { err ->
                Text(
                    text = err.asText(context),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = { viewModel.save(name, phone, house, onDone) },
                enabled = state.loaded && !state.isSaving && name.isNotBlank() && phone.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("complete_save_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.complete_profile_btn))
                }
            }

            TextButton(
                onClick = { viewModel.signOut(onSignedOut) },
                modifier = Modifier.fillMaxWidth().testTag("complete_signout_button")
            ) {
                Text(stringResource(R.string.complete_profile_wrong_account))
            }
        }
    }
}
