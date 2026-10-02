package com.kuniran.feature.home

import com.kuniran.core.common.asText

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuniran.R
import com.kuniran.core.model.PostType
import com.kuniran.core.ui.components.KuniranEmptyState
import com.kuniran.core.ui.components.KuniranTopAppBar

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateCalendarRsvp: () -> Unit = {},
    onNavigateForum: () -> Unit = {},
    onNavigateQrScanner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err.asText(context))
            viewModel.clearError()
        }
    }

    val filteredPosts = remember(uiState.posts, uiState.selectedFilter) {
        when (uiState.selectedFilter) {
            "PENGUMUMAN" -> uiState.posts.filter { it.type == PostType.PENGUMUMAN }
            "AGENDA" -> uiState.posts.filter { it.type == PostType.AGENDA }
            "FINANCE_REPORT" -> uiState.posts.filter { it.type == PostType.FINANCE_REPORT }
            else -> uiState.posts
        }
    }

    val rtLabel = uiState.rtGroup?.displayLabel ?: stringResource(R.string.nav_home)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            KuniranTopAppBar(
                title = rtLabel,
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshFeed() },
                        modifier = Modifier.testTag("btn_refresh_feed")
                    ) {
                        if (uiState.isRefreshing) {
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("btn_create_post_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.home_btn_new_post)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Quick Access Features (Agenda, Forum, Presensi)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAccessButton(
                    icon = Icons.Default.CalendarToday,
                    label = stringResource(R.string.home_tab_agenda),
                    onClick = onNavigateCalendarRsvp,
                    modifier = Modifier.weight(1f).testTag("btn_home_quick_calendar")
                )
                QuickAccessButton(
                    icon = Icons.Default.Forum,
                    label = "Forum",
                    onClick = onNavigateForum,
                    modifier = Modifier.weight(1f).testTag("btn_home_quick_forum")
                )
                QuickAccessButton(
                    icon = Icons.Default.QrCodeScanner,
                    label = "Presensi",
                    onClick = onNavigateQrScanner,
                    modifier = Modifier.weight(1f).testTag("btn_home_quick_qr")
                )
            }

            // Filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedFilter == "ALL",
                    onClick = { viewModel.setFilter("ALL") },
                    label = { Text(stringResource(R.string.home_tab_all), maxLines = 1, softWrap = false) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == "PENGUMUMAN",
                    onClick = { viewModel.setFilter("PENGUMUMAN") },
                    label = { Text(stringResource(R.string.home_tab_pengumuman), maxLines = 1, softWrap = false) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == "AGENDA",
                    onClick = { viewModel.setFilter("AGENDA") },
                    label = { Text(stringResource(R.string.home_tab_agenda), maxLines = 1, softWrap = false) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == "FINANCE_REPORT",
                    onClick = { viewModel.setFilter("FINANCE_REPORT") },
                    label = { Text(stringResource(R.string.home_tab_finance), maxLines = 1, softWrap = false) }
                )
            }

            if (filteredPosts.isEmpty()) {
                KuniranEmptyState(
                    icon = Icons.Default.Campaign,
                    message = stringResource(R.string.home_empty_feed),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("feed_list"),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPosts, key = { it.id }) { post ->
                        PostItemCard(
                            post = post,
                            currentUserId = uiState.currentUser?.id,
                            currentUserRole = uiState.currentUser?.role,
                            onPinClick = { viewModel.pinPost(post.id) },
                            onUnpinClick = { viewModel.unpinPost(post.id) },
                            onDeleteClick = { viewModel.deletePost(post.id) }
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreatePostDialog(
                onDismiss = { showCreateDialog = false },
                onSubmit = { title, content, type, eventDate, eventLocation ->
                    showCreateDialog = false
                    viewModel.createPost(
                        title = title,
                        content = content,
                        type = type,
                        eventDate = eventDate,
                        eventLocation = eventLocation,
                        onSuccess = {
                            viewModel.refreshFeed()
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun QuickAccessButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
