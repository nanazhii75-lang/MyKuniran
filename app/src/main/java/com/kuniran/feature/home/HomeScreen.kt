package com.kuniran.feature.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil.ImageLoader
import com.kuniran.R
import com.kuniran.core.common.asText
import com.kuniran.core.ui.components.KuniranTopAppBar
import kotlinx.coroutines.delay

private const val POST_MAX_LENGTH = 5000
private const val FEED_POLL_INTERVAL_MS = 30_000L

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var draft by rememberSaveable { mutableStateOf("") }
    var asAnnouncement by rememberSaveable { mutableStateOf(false) }
    var pickedImage by rememberSaveable { mutableStateOf<Uri?>(null) }

    // Pemilih foto bawaan Android: tidak butuh izin tambahan
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedImage = uri
    }

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

    val feed = uiState.posts

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
            item(key = "composer") {
                PostComposer(
                    draft = draft,
                    onDraftChange = { draft = it },
                    asAnnouncement = asAnnouncement,
                    onToggleAnnouncement = { asAnnouncement = !asAnnouncement },
                    isSending = uiState.isLoading,
                    pickedImage = pickedImage,
                    imageLoader = imageLoader,
                    onPickImage = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemoveImage = { pickedImage = null },
                    onSend = {
                        viewModel.sendPost(draft, asAnnouncement, pickedImage) {
                            draft = ""
                            asAnnouncement = false
                            pickedImage = null
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
                        imageLoader = imageLoader,
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
private fun PostComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    asAnnouncement: Boolean,
    onToggleAnnouncement: () -> Unit,
    isSending: Boolean,
    pickedImage: Uri?,
    imageLoader: ImageLoader,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
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
            Text(
                text = stringResource(R.string.home_info_today_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

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

            if (pickedImage != null) {
                ComposerImagePreview(
                    uri = pickedImage,
                    imageLoader = imageLoader,
                    onRemove = onRemoveImage
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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

                IconButton(
                    onClick = onPickImage,
                    enabled = !isSending,
                    modifier = Modifier.testTag("btn_pick_photo")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = stringResource(R.string.home_btn_add_photo),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                }

                FilledIconButton(
                    onClick = onSend,
                    enabled = (draft.isNotBlank() || pickedImage != null) && !isSending,
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
