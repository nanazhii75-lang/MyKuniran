package com.kuniran.feature.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuniran.R
import com.kuniran.core.common.asText
import com.kuniran.core.model.UserRole
import com.kuniran.core.ui.components.KuniranTopAppBar

/** Bagian Pengaturan yang dibuka dari daftar menu (layar utama = daftar, tanpa bagian terbuka). */
private enum class SettingsSection(@StringRes val titleRes: Int) {
    PROFILE(R.string.settings_home_profile),
    RT_IDENTITY(R.string.settings_home_rt),
    ACCOUNT(R.string.settings_home_account)
}

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

    var showEditRtDialog by remember { mutableStateOf(false) }
    var showChangeUsernameDialog by remember { mutableStateOf(false) }
    var sectionName by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err.asText(context))
            viewModel.clearError()
        }
    }

    val isAdmin = uiState.currentUser?.role == UserRole.ADMIN_RT
    val hasRt = !uiState.currentUser?.rtId.isNullOrBlank()
    val rtGroup = uiState.rtGroup
    val rtReady = hasRt && rtGroup != null

    val section = SettingsSection.values()
        .firstOrNull { it.name == sectionName }
        ?.takeIf { it != SettingsSection.RT_IDENTITY || rtReady }

    BackHandler(enabled = section != null) { sectionName = null }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            KuniranTopAppBar(
                title = if (section == null) {
                    stringResource(R.string.settings_title)
                } else {
                    stringResource(section.titleRes)
                },
                navigationIcon = {
                    if (section != null) {
                        IconButton(
                            onClick = { sectionName = null },
                            modifier = Modifier.testTag("btn_settings_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.settings_btn_back)
                            )
                        }
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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (section) {
                null -> SettingsHomeList(
                    hasRt = hasRt,
                    showRt = rtReady,
                    onOpenProfile = { sectionName = SettingsSection.PROFILE.name },
                    onOpenRt = { sectionName = SettingsSection.RT_IDENTITY.name },
                    onOpenAccount = { sectionName = SettingsSection.ACCOUNT.name },
                    onNavigateHelpFaq = onNavigateHelpFaq,
                    onNavigateResidentDirectory = onNavigateResidentDirectory,
                    onNavigateFinanceDashboard = onNavigateFinanceDashboard,
                    onNavigateCalendarRsvp = onNavigateCalendarRsvp,
                    onNavigateForum = onNavigateForum,
                    onNavigateQrScanner = onNavigateQrScanner
                )
                SettingsSection.PROFILE -> SettingsProfileTab(
                    uiState = uiState,
                    viewModel = viewModel
                )
                SettingsSection.RT_IDENTITY -> if (rtGroup != null) {
                    SettingsInviteCard(group = rtGroup, isAdmin = isAdmin)
                    if (isAdmin) {
                        SettingsRtTab(
                            group = rtGroup,
                            viewModel = viewModel,
                            onEditRt = { showEditRtDialog = true },
                            onChangeUsername = { showChangeUsernameDialog = true }
                        )
                    }
                }
                SettingsSection.ACCOUNT -> SettingsAccountSection(
                    hasRt = hasRt,
                    viewModel = viewModel,
                    onLoggedOut = onLoggedOut
                )
            }
        }

        if (showEditRtDialog && rtGroup != null) {
            EditRtInfoDialog(
                rtGroup = rtGroup,
                onDismiss = { showEditRtDialog = false },
                onSubmit = { name, rtNum, rwNum, desa, dukuh, lingkungan ->
                    showEditRtDialog = false
                    viewModel.updateRtInfo(name, rtNum, rwNum, desa, dukuh, lingkungan) {}
                }
            )
        }

        if (showChangeUsernameDialog && rtGroup != null) {
            ChangeUsernameDialog(
                currentUsername = rtGroup.inviteUsername,
                onDismiss = { showChangeUsernameDialog = false },
                onSubmit = { newUsername ->
                    showChangeUsernameDialog = false
                    viewModel.setInviteUsername(newUsername) {}
                }
            )
        }
    }
}
