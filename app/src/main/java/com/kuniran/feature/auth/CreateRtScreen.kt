package com.kuniran.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuniran.R
import com.kuniran.core.ui.components.KuniranTopAppBar

@Composable
fun CreateRtScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onRtCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var rtNumber by remember { mutableStateOf("02") }
    var rwNumber by remember { mutableStateOf("01") }
    var desa by remember { mutableStateOf("Klepu") }
    var dukuh by remember { mutableStateOf("Krajan") }
    var lingkungan by remember { mutableStateOf("Sooko") }
    var inviteUsername by remember { mutableStateOf("rtklepu_02") }
    var rtLabel by remember { mutableStateOf("") }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(context.getString(err.messageRes))
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            KuniranTopAppBar(
                title = stringResource(R.string.create_rt_header),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_cancel)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = rtNumber,
                    onValueChange = { rtNumber = it },
                    label = { Text(stringResource(R.string.field_rt_number)) },
                    placeholder = { Text(stringResource(R.string.field_rt_number_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_rt_num_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = rwNumber,
                    onValueChange = { rwNumber = it },
                    label = { Text(stringResource(R.string.field_rw_number)) },
                    placeholder = { Text(stringResource(R.string.field_rw_number_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_rw_num_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = desa,
                onValueChange = { desa = it },
                label = { Text(stringResource(R.string.field_desa)) },
                placeholder = { Text(stringResource(R.string.field_desa_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_desa_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = dukuh,
                onValueChange = { dukuh = it },
                label = { Text(stringResource(R.string.field_dukuh)) },
                placeholder = { Text(stringResource(R.string.field_dukuh_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_dukuh_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = lingkungan,
                onValueChange = { lingkungan = it },
                label = { Text(stringResource(R.string.field_lingkungan)) },
                placeholder = { Text(stringResource(R.string.field_lingkungan_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_lingkungan_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = inviteUsername,
                onValueChange = {
                    inviteUsername = it.trim()
                    if (it.length >= 5) viewModel.checkUsername(it)
                },
                label = { Text(stringResource(R.string.field_invite_username)) },
                placeholder = { Text(stringResource(R.string.field_invite_username_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_username_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = rtLabel,
                onValueChange = { rtLabel = it },
                label = { Text(stringResource(R.string.field_rt_name_optional)) },
                placeholder = { Text(stringResource(R.string.field_rt_name_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_label_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.createRt(
                        name = rtLabel,
                        rtNumber = rtNumber,
                        rwNumber = rwNumber,
                        desa = desa,
                        dukuh = dukuh,
                        lingkungan = lingkungan,
                        inviteUsername = inviteUsername,
                        onSuccess = { onRtCreated() }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_submit_create_rt"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = rtNumber.isNotBlank() && rwNumber.isNotBlank() &&
                        desa.isNotBlank() && dukuh.isNotBlank() &&
                        lingkungan.isNotBlank() && inviteUsername.length >= 5 && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.btn_save_rt),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
