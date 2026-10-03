package com.kuniran.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kuniran.R
import com.kuniran.core.common.DateTimeUtils
import com.kuniran.core.common.asText
import com.kuniran.core.model.Post
import com.kuniran.core.model.PostType
import com.kuniran.core.model.UserRole
import com.kuniran.core.ui.components.KuniranTopAppBar
import kotlinx.coroutines.delay

private const val POST_MAX_LENGTH = 5000
private const val FEED_POLL_INTERVAL_MS = 30_000L

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var draft by rememberSaveable { mutableStateOf("") }
    var asAnnouncement by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err.asText(context))
            viewModel.clearError()
        }
    }

    // Penyegaran otomatis selama layar terlihat (pengganti sementara sampai Realtime terpasang)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refreshFeed(silent = true)
                delay(FEED_POLL_INTERVAL_MS)
            }
        }
    }

    // Informasi Hari Ini: pengumuman yang disematkan admin (selama belum kedaluwarsa), kalau tidak
    // ada maka pengumuman terbaru yang dibuat HARI INI (WIB). Selain itu kartu kosong; pengumuman
    // lama tetap bisa dibaca di daftar di bawah.
    val featured = remember(uiState.posts) {
        val announcements = uiState.posts.filter { it.type == PostType.PENGUMUMAN }
        announcements.firstOrNull {
            it.isPinned && (it.pinnedUntil == null || DateTimeUtils.isFuture(it.pinnedUntil))
        } ?: announcements.firstOrNull { DateTimeUtils.isTodayWib(it.createdAt) }
    }
    val isAdmin = uiState.currentUser?.role == UserRole.ADMIN_RT
    val feed = remember(uiState.posts, featured) {
        uiState.posts.filter { it.id != featured?.id }
    }

    val rtLabel = uiState.rtGroup?.displayLabel ?: stringResource(R.string.nav_home)
    val fallbackAuthor = stringResource(R.string.home_author_fallback)

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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("feed_list"),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "info_today") {
                InfoTodayCard(
                    post = featured,
                    authorName = featured?.let { uiState.authorNames[it.authorId] ?: it.authorName.ifBlank { fallbackAuthor } },
                    actions = {
                        if (featured != null) {
                            PostActionsMenu(
                                canPin = isAdmin,
                                canDelete = isAdmin || uiState.currentUser?.id == featured.authorId,
                                isPinned = featured.isPinned,
                                onPinClick = { viewModel.pinPost(featured.id) },
                                onUnpinClick = { viewModel.unpinPost(featured.id) },
                                onDeleteClick = { viewModel.deletePost(featured.id) }
                            )
                        }
                    }
                )
            }

            item(key = "composer") {
                PostComposer(
                    draft = draft,
                    onDraftChange = { draft = it },
                    asAnnouncement = asAnnouncement,
                    onToggleAnnouncement = { asAnnouncement = !asAnnouncement },
                    isSending = uiState.isLoading,
                    onSend = {
                        viewModel.sendPost(draft, asAnnouncement) {
                            draft = ""
                            asAnnouncement = false
                        }
                    }
                )
            }

            if (feed.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.home_empty_chat),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            } else {
                items(feed, key = { it.id }) { post ->
                    PostItemCard(
                        post = post,
                        authorName = uiState.authorNames[post.authorId]
                            ?: post.authorName.ifBlank { fallbackAuthor },
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
}

@Composable
private fun InfoTodayCard(
    post: Post?,
    authorName: String?,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_info_today"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_info_today_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                actions()
            }

            if (post == null) {
                Text(
                    text = stringResource(R.string.home_info_today_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (post.showsSeparateTitle()) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (post.content.isNotBlank()) {
                    Text(
                        text = post.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = listOfNotNull(authorName, DateTimeUtils.formatWibDate(post.createdAt))
                        .joinToString(" \u2022 "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PostComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    asAnnouncement: Boolean,
    onToggleAnnouncement: () -> Unit,
    isSending: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= POST_MAX_LENGTH) onDraftChange(it) },
                placeholder = { Text(stringResource(R.string.home_composer_hint)) },
                minLines = 2,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_home_post")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilterChip(
                    selected = asAnnouncement,
                    onClick = onToggleAnnouncement,
                    label = {
                        Text(
                            text = stringResource(R.string.home_composer_announcement),
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("chip_post_announcement")
                )

                FilledIconButton(
                    onClick = onSend,
                    enabled = draft.isNotBlank() && !isSending,
                    modifier = Modifier.testTag("btn_send_post")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.home_btn_send)
                        )
                    }
                }
            }

            if (asAnnouncement) {
                Text(
                    text = stringResource(R.string.home_composer_announcement_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
