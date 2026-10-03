package com.kuniran.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuniran.R
import com.kuniran.core.common.asText
import com.kuniran.core.model.UserRole
import com.kuniran.core.ui.components.KuniranTopAppBar

private enum class SettingsTab(@StringRes val labelRes: Int) {
    PROFILE(R.string.settings_tab_profile),
    MENU(R.string.settings_tab_menu),
    RT(R.string.settings_tab_rt)
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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err.asText(context))
            viewModel.clearError()
        }
    }

    val isAdmin = uiState.currentUser?.role == UserRole.ADMIN_RT
    val hasRt = !uiState.currentUser?.rtId.isNullOrBlank()
    val rtGroup = uiState.rtGroup

    val tabs = buildList {
        add(SettingsTab.PROFILE)
        add(SettingsTab.MENU)
        if (isAdmin && rtGroup != null) add(SettingsTab.RT)
    }
    val activeIndex = selectedTab.coerceIn(0, tabs.lastIndex)

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
        ) {
            TabRow(selectedTabIndex = activeIndex) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = index == activeIndex,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = stringResource(tab.labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("settings_tab_${tab.name.lowercase()}")
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (tabs[activeIndex]) {
                    SettingsTab.PROFILE -> SettingsProfileTab(
                        uiState = uiState,
                        hasRt = hasRt,
                        viewModel = viewModel,
                        onLoggedOut = onLoggedOut
                    )
                    SettingsTab.MENU -> SettingsMenuTab(
                        hasRt = hasRt,
                        onNavigateHelpFaq = onNavigateHelpFaq,
                        onNavigateResidentDirectory = onNavigateResidentDirectory,
                        onNavigateFinanceDashboard = onNavigateFinanceDashboard,
                        onNavigateCalendarRsvp = onNavigateCalendarRsvp,
                        onNavigateForum = onNavigateForum,
                        onNavigateQrScanner = onNavigateQrScanner
                    )
                    SettingsTab.RT -> if (rtGroup != null) {
                        SettingsRtTab(
                            group = rtGroup,
                            viewModel = viewModel,
                            onEditRt = { showEditRtDialog = true },
                            onChangeUsername = { showChangeUsernameDialog = true }
                        )
                    }
                }
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
