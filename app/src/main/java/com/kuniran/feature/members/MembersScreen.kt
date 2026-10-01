package com.kuniran.feature.members

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuniran.R
import com.kuniran.core.model.RtMember
import com.kuniran.core.model.UserRole
import com.kuniran.core.ui.components.KuniranBadge
import com.kuniran.core.ui.components.KuniranEmptyState
import com.kuniran.core.ui.components.KuniranTopAppBar

@Composable
fun MembersScreen(
    viewModel: MembersViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedMember by remember { mutableStateOf<RtMember?>(null) }
    var showPendingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(context.getString(err.messageRes))
            viewModel.clearError()
        }
    }

    val filteredMembers = remember(uiState.members, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.members
        } else {
            val q = uiState.searchQuery.lowercase()
            uiState.members.filter {
                it.fullName.lowercase().contains(q) || (it.houseInfo?.lowercase()?.contains(q) == true)
            }
        }
    }

    val isAdmin = uiState.currentUser?.role == UserRole.ADMIN_RT

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            KuniranTopAppBar(
                title = stringResource(R.string.members_title),
                actions = {
                    if (isAdmin && uiState.pendingRequests.isNotEmpty()) {
                        IconButton(
                            onClick = { showPendingDialog = true },
                            modifier = Modifier.testTag("btn_pending_requests_bell")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(uiState.pendingRequests.size.toString())
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = stringResource(
                                        R.string.members_pending_requests,
                                        uiState.pendingRequests.size
                                    )
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.syncMembers() },
                        modifier = Modifier.testTag("btn_refresh_members")
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.btn_retry)
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
        ) {
            // Search field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(stringResource(R.string.members_search_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("members_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredMembers.isEmpty()) {
                KuniranEmptyState(
                    icon = Icons.Default.Group,
                    message = stringResource(R.string.members_empty),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("members_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMembers, key = { it.id }) { member ->
                        MemberItemRow(
                            member = member,
                            onClick = { selectedMember = member }
                        )
                    }
                }
            }
        }

        selectedMember?.let { member ->
            MemberDetailDialog(
                member = member,
                currentUserId = uiState.currentUser?.id,
                currentUserRole = uiState.currentUser?.role,
                onTransferAdmin = {
                    viewModel.transferAdmin(member.id)
                    selectedMember = null
                },
                onRemoveMember = {
                    viewModel.removeMember(member.id)
                    selectedMember = null
                },
                onDismiss = { selectedMember = null }
            )
        }

        if (showPendingDialog) {
            PendingRequestsDialog(
                requests = uiState.pendingRequests,
                onApprove = { reqId -> viewModel.approveRequest(reqId) },
                onReject = { reqId -> viewModel.rejectRequest(reqId) },
                onDismiss = { showPendingDialog = false }
            )
        }
    }
}

@Composable
private fun MemberItemRow(
    member: RtMember,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("member_item_${member.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (member.role == UserRole.ADMIN_RT) {
                        KuniranBadge(
                            text = stringResource(R.string.members_badge_admin),
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                member.houseInfo?.let { house ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = house,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
