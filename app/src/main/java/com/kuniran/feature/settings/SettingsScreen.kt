package com.kuniran.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuniran.R
import com.kuniran.core.model.UserRole
import com.kuniran.core.ui.components.KuniranTopAppBar

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onLoggedOut: () -> Unit,
    onNavigateHelpFaq: () -> Unit,
    onNavigateResidentDirectory: () -> Unit = {},
    onNavigateFinanceDashboard: () -> Unit = {},
    onNavigateCalendarRsvp: () -> Unit = {},
    onNavigateForum: () -> Unit = {},
    onNavigateQrScanner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var houseInfo by remember { mutableStateOf("") }

    var showEditRtDialog by remember { mutableStateOf(false) }
    var showChangeUsernameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.currentUser) {
        uiState.currentUser?.let { user ->
            fullName = user.fullName
            phoneNumber = user.phoneNumber ?: ""
            houseInfo = user.houseInfo ?: ""
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(context.getString(err.messageRes))
            viewModel.clearError()
        }
    }

    val isAdmin = uiState.currentUser?.role == UserRole.ADMIN_RT
    val hasRt = !uiState.currentUser?.rtId.isNullOrBlank()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            KuniranTopAppBar(title = stringResource(R.string.settings_title))
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
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
                        text = stringResource(R.string.settings_profile_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text(stringResource(R.string.field_profile_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text(stringResource(R.string.field_profile_phone)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_phone_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = houseInfo,
                        onValueChange = { houseInfo = it },
                        label = { Text(stringResource(R.string.field_profile_house)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_house_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.updateProfile(
                                fullName,
                                phoneNumber.ifBlank { null },
                                houseInfo.ifBlank { null }
                            ) {
                                // Success
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_save_profile"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = fullName.isNotBlank() && !uiState.isLoading
                    ) {
                        Text(stringResource(R.string.btn_save_profile))
                    }
                }
            }

            // RT Administration (if Admin)
            if (isAdmin && uiState.rtGroup != null) {
                val group = uiState.rtGroup!!
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

                        // Auto approve toggle
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
                            onClick = { showEditRtDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("btn_open_edit_rt")
                        ) {
                            Text(stringResource(R.string.settings_btn_edit_rt))
                        }

                        OutlinedButton(
                            onClick = { showChangeUsernameDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("btn_open_change_username")
                        ) {
                            Text(stringResource(R.string.settings_btn_change_username))
                        }
                    }
                }
            }

            // Help & FAQ Button
            OutlinedButton(
                onClick = onNavigateHelpFaq,
                modifier = Modifier.fillMaxWidth().testTag("btn_open_help_faq")
            ) {
                Text(stringResource(R.string.settings_btn_help_faq))
            }

            // Resident Directory Button (Supabase warga table)
            if (hasRt) {
                OutlinedButton(
                    onClick = onNavigateResidentDirectory,
                    modifier = Modifier.fillMaxWidth().testTag("btn_open_resident_directory")
                ) {
                    Text(stringResource(R.string.settings_btn_resident_directory))
                }

                // Finance Analytics Dashboard Button
                OutlinedButton(
                    onClick = onNavigateFinanceDashboard,
                    modifier = Modifier.fillMaxWidth().testTag("btn_open_finance_dashboard")
                ) {
                    Text(stringResource(R.string.settings_btn_finance_dashboard))
                }

                // Calendar & RSVP Button
                OutlinedButton(
                    onClick = onNavigateCalendarRsvp,
                    modifier = Modifier.fillMaxWidth().testTag("btn_open_calendar_rsvp")
                ) {
                    Text(stringResource(R.string.settings_btn_calendar_rsvp))
                }

                // Discussion Forum Button
                OutlinedButton(
                    onClick = onNavigateForum,
                    modifier = Modifier.fillMaxWidth().testTag("btn_open_forum")
                ) {
                    Text(stringResource(R.string.settings_btn_forum))
                }

                // QR Attendance Scanner Button
                OutlinedButton(
                    onClick = onNavigateQrScanner,
                    modifier = Modifier.fillMaxWidth().testTag("btn_open_qr_scanner")
                ) {
                    Text(stringResource(R.string.settings_btn_qr_scanner))
                }
            }

            // Membership Actions
            if (hasRt) {
                OutlinedButton(
                    onClick = {
                        viewModel.leaveRt {
                            onLoggedOut()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_leave_rt"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.settings_btn_leave_rt))
                }
            }

            // Sign out
            Button(
                onClick = {
                    viewModel.signOut {
                        onLoggedOut()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_signout"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(stringResource(R.string.settings_btn_signout))
            }
        }

        if (showEditRtDialog && uiState.rtGroup != null) {
            EditRtInfoDialog(
                rtGroup = uiState.rtGroup!!,
                onDismiss = { showEditRtDialog = false },
                onSubmit = { name, rtNum, rwNum, desa, dukuh, lingkungan ->
                    showEditRtDialog = false
                    viewModel.updateRtInfo(name, rtNum, rwNum, desa, dukuh, lingkungan) {}
                }
            )
        }

        if (showChangeUsernameDialog && uiState.rtGroup != null) {
            ChangeUsernameDialog(
                currentUsername = uiState.rtGroup!!.inviteUsername,
                onDismiss = { showChangeUsernameDialog = false },
                onSubmit = { newUsername ->
                    showChangeUsernameDialog = false
                    viewModel.setInviteUsername(newUsername) {}
                }
            )
        }
    }
}
