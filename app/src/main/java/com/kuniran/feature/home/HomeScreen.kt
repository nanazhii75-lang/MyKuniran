package com.kuniran.feature.home

import com.kuniran.core.common.asText

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
            // Quick Access Features (Calendar & RSVP, Forum Warga, QR Scanner)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateCalendarRsvp,
                    modifier = Modifier.weight(1f).testTag("btn_home_quick_calendar")
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.home_tab_agenda), style = MaterialTheme.typography.bodySmall)
                }

                OutlinedButton(
                    onClick = onNavigateForum,
                    modifier = Modifier.weight(1f).testTag("btn_home_quick_forum")
                ) {
                    Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Forum", style = MaterialTheme.typography.bodySmall)
                }

                OutlinedButton(
                    onClick = onNavigateQrScanner,
                    modifier = Modifier.weight(1f).testTag("btn_home_quick_qr")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Presensi", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedFilter == "ALL",
                    onClick = { viewModel.setFilter("ALL") },
                    label = { Text(stringResource(R.string.home_tab_all)) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == "PENGUMUMAN",
                    onClick = { viewModel.setFilter("PENGUMUMAN") },
                    label = { Text(stringResource(R.string.home_tab_pengumuman)) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == "AGENDA",
                    onClick = { viewModel.setFilter("AGENDA") },
                    label = { Text(stringResource(R.string.home_tab_agenda)) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == "FINANCE_REPORT",
                    onClick = { viewModel.setFilter("FINANCE_REPORT") },
                    label = { Text(stringResource(R.string.home_tab_finance)) }
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
                    contentPadding = PaddingValues(16.dp),
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
