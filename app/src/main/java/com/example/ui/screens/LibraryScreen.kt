package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.DownloadStatus
import com.example.data.local.LocalMediaEntity
import com.example.data.local.MetadataMatchStatus
import com.example.data.local.OfflineDownloadEntity
import com.example.data.model.LibraryMediaItem
import com.example.data.model.PlaybackEngine
import com.example.player.PlaybackRequest
import com.example.ui.theme.machinedCardBorder
import com.example.ui.viewmodel.UiState
import java.io.File

@Composable
fun LibraryScreen(
    libraryState: UiState<List<LibraryMediaItem>>,
    downloads: List<OfflineDownloadEntity>,
    localMedia: List<LocalMediaEntity>,
    preferredEngine: PlaybackEngine,
    onAddPersonalCloudMedia: (title: String, url: String, provider: String, poster: String) -> Unit,
    onRemoveLibraryItem: (String) -> Unit,
    onImportLocalVideo: (Uri) -> Unit,
    onAttachLocalSubtitle: (uriString: String, subUri: Uri) -> Unit,
    onRefreshLocalMetadata: (String) -> Unit,
    onDeleteLocalMedia: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onPlayRequest: (PlaybackRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCloudDialog by remember { mutableStateOf(false) }
    var pendingSubTargetUri by remember { mutableStateOf<String?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportLocalVideo(uri)
    }

    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { subUri ->
        val target = pendingSubTargetUri
        if (subUri != null && target != null) {
            onAttachLocalSubtitle(target, subUri)
        }
        pendingSubTargetUri = null
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 10.dp)
        ) {
            Text(
                text = "Media Vault",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Cloud Library, Offline Downloads & Local Auto-Metadata",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Cloud Vault") },
                modifier = Modifier.testTag("vault_tab_cloud")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Offline (${downloads.size})") },
                modifier = Modifier.testTag("vault_tab_offline")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Local (${localMedia.size})") },
                modifier = Modifier.testTag("vault_tab_local")
            )
        }

        when (selectedTab) {
            0 -> CloudVaultTab(
                libraryState = libraryState,
                downloads = downloads,
                localMediaCount = localMedia.size,
                preferredEngine = preferredEngine,
                onOpenAddCloud = { showCloudDialog = true },
                onDeleteItem = onRemoveLibraryItem,
                onPlayRequest = onPlayRequest
            )
            1 -> OfflineDownloadsTab(
                downloads = downloads,
                preferredEngine = preferredEngine,
                onPlayRequest = onPlayRequest,
                onDeleteDownload = onDeleteDownload
            )
            2 -> LocalMediaTab(
                localMedia = localMedia,
                preferredEngine = preferredEngine,
                onPickVideo = { videoPickerLauncher.launch(arrayOf("video/*")) },
                onRefreshMetadata = onRefreshLocalMetadata,
                onAttachSubtitle = { uriStr ->
                    pendingSubTargetUri = uriStr
                    subtitlePickerLauncher.launch(arrayOf("text/*", "application/x-subrip", "*/*"))
                },
                onDeleteLocal = onDeleteLocalMedia,
                onPlayRequest = onPlayRequest
            )
        }
    }

    if (showCloudDialog) {
        AddCloudMediaDialog(
            onDismiss = { showCloudDialog = false },
            onConfirm = { title, url, provider, poster ->
                onAddPersonalCloudMedia(title, url, provider, poster)
                showCloudDialog = false
            }
        )
    }
}

@Composable
private fun CloudVaultTab(
    libraryState: UiState<List<LibraryMediaItem>>,
    downloads: List<OfflineDownloadEntity>,
    localMediaCount: Int,
    preferredEngine: PlaybackEngine,
    onOpenAddCloud: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onPlayRequest: (PlaybackRequest) -> Unit
) {
    val itemsList = (libraryState as? UiState.Success)?.data.orEmpty()
    val totalCachedBytes = remember(downloads) { downloads.sumOf { it.bytesDownloaded } }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            val hubShape = RoundedCornerShape(20.dp)
            Card(
                shape = hubShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .machinedCardBorder(shape = hubShape, cornerRadiusDp = 20.dp)
                    .testTag("storage_cloud_hub_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Storage & Cloud Hub",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${itemsList.size} Cloud • ${downloads.size} Offline (${formatBytes(totalCachedBytes)}) • $localMediaCount Local",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = onOpenAddCloud,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("add_cloud_source_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Link")
                            }
                        }

                        // Segmented Offline / Cached / Cloud Bar
                        val cloudUnits = itemsList.size.coerceAtLeast(1).toFloat()
                        val offlineUnits = downloads.size.coerceAtLeast(1).toFloat()
                        val localUnits = localMediaCount.coerceAtLeast(1).toFloat()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.08f)),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(cloudUnits)
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(offlineUnits)
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(localUnits)
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                        }
                    }
                }
            }
        }

        if (itemsList.isEmpty()) {
            item {
                EmptyVaultPlaceholder(
                    title = stringResource(R.string.empty_library_title),
                    subtitle = stringResource(R.string.empty_library_subtitle)
                )
            }
        } else {
            items(itemsList, key = { it.mediaKey }) { item ->
                val itemShape = RoundedCornerShape(16.dp)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = itemShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .machinedCardBorder(shape = itemShape, cornerRadiusDp = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 68.dp, height = 96.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.posterUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.posterUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.CloudQueue,
                                    contentDescription = item.title,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.sourceLabel} • ${item.mediaType.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (item.durationMs > 0L && item.watchProgressMs > 0L) {
                                val pct = (item.watchProgressMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                )
                            }
                        }

                        if (item.streamUrl.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onPlayRequest(
                                        PlaybackRequest(
                                            mediaKey = item.mediaKey,
                                            title = item.title,
                                            mediaType = item.mediaType,
                                            streamUrl = item.streamUrl,
                                            posterUrl = item.posterUrl,
                                            backdropUrl = item.backdropUrl,
                                            initialPositionMs = item.watchProgressMs,
                                            preferredEngine = preferredEngine.key
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Play ${item.title}",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteItem(item.mediaKey) }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Remove ${item.title}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineDownloadsTab(
    downloads: List<OfflineDownloadEntity>,
    preferredEngine: PlaybackEngine,
    onPlayRequest: (PlaybackRequest) -> Unit,
    onDeleteDownload: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (downloads.isEmpty()) {
            item {
                EmptyVaultPlaceholder(
                    title = "No Offline Downloads Yet",
                    subtitle = "Tap the download icon on any stream in Discover to watch offline on the go."
                )
            }
        } else {
            items(downloads, key = { it.downloadKey }) { dl ->
                val isCompleted = dl.status == DownloadStatus.COMPLETED.name && dl.localFilePath.isNotBlank()
                val dlShape = RoundedCornerShape(16.dp)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = dlShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .machinedCardBorder(shape = dlShape, cornerRadiusDp = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dl.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${dl.status} • ${dl.qualityLabel} • ${formatBytes(dl.bytesDownloaded)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Row {
                                if (isCompleted) {
                                    IconButton(
                                        onClick = {
                                            val fileUri = Uri.fromFile(File(dl.localFilePath)).toString()
                                            val subUri = if (dl.localSubtitlePath.isNotBlank()) {
                                                Uri.fromFile(File(dl.localSubtitlePath)).toString()
                                            } else ""
                                            onPlayRequest(
                                                PlaybackRequest(
                                                    mediaKey = dl.mediaKey,
                                                    title = dl.title,
                                                    mediaType = dl.mediaType,
                                                    streamUrl = fileUri,
                                                    posterUrl = dl.posterUrl,
                                                    preferredEngine = preferredEngine.key,
                                                    localSubtitleUri = subUri
                                                )
                                            )
                                        }
                                    ) {
                                        Icon(
                                            Icons.Rounded.DownloadDone,
                                            contentDescription = "Play offline",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteDownload(dl.downloadKey) }) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete download",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        if (!isCompleted && dl.status != DownloadStatus.FAILED.name) {
                            LinearProgressIndicator(
                                progress = { dl.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                        }
                        if (dl.errorMessage.isNotBlank()) {
                            Text(
                                text = dl.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalMediaTab(
    localMedia: List<LocalMediaEntity>,
    preferredEngine: PlaybackEngine,
    onPickVideo: () -> Unit,
    onRefreshMetadata: (String) -> Unit,
    onAttachSubtitle: (String) -> Unit,
    onDeleteLocal: (String) -> Unit,
    onPlayRequest: (PlaybackRequest) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            val localBannerShape = RoundedCornerShape(18.dp)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = localBannerShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .machinedCardBorder(shape = localBannerShape, cornerRadiusDp = 18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Local Media + Auto Metadata",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select any MKV/MP4 from your device. CineVault parses filename & resolution and retrieves IMDb posters & synopsis automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onPickVideo,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("import_local_video_button")
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import")
                    }
                }
            }
        }

        if (localMedia.isEmpty()) {
            item {
                EmptyVaultPlaceholder(
                    title = "No Local Videos Imported",
                    subtitle = "Tap Import to add local device videos with automatic poster & metadata matching."
                )
            }
        } else {
            items(localMedia, key = { it.uriString }) { media ->
                val localShape = RoundedCornerShape(16.dp)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = localShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .machinedCardBorder(shape = localShape, cornerRadiusDp = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(width = 70.dp, height = 100.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                if (media.posterUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = media.posterUrl,
                                        contentDescription = media.parsedTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.FolderOpen,
                                        contentDescription = media.parsedTitle,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = media.parsedTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val metaLine = listOfNotNull(
                                    media.parsedYear.takeIf { it.isNotBlank() },
                                    media.resolution.takeIf { it.isNotBlank() },
                                    media.imdbRating.takeIf { it.isNotBlank() }?.let { "IMDb $it" }
                                ).joinToString(" • ")
                                if (metaLine.isNotBlank()) {
                                    Text(
                                        text = metaLine,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = when (media.metadataStatus) {
                                        MetadataMatchStatus.MATCHED.name -> "Metadata matched (${media.imdbId})"
                                        MetadataMatchStatus.PENDING.name -> "Retrieving metadata..."
                                        else -> "Local file • ${media.qualityTags.ifBlank { media.mimeType }}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (media.overview.isNotBlank()) {
                            Text(
                                text = media.overview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onPlayRequest(
                                        PlaybackRequest(
                                            mediaKey = media.imdbId.ifBlank { "local_${media.uriString.hashCode()}" },
                                            title = media.parsedTitle,
                                            mediaType = "local_video",
                                            streamUrl = media.uriString,
                                            posterUrl = media.posterUrl,
                                            backdropUrl = media.backdropUrl,
                                            imdbId = media.imdbId,
                                            preferredEngine = preferredEngine.key,
                                            localSubtitleUri = media.localSubtitleUri
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play")
                            }

                            OutlinedButton(
                                onClick = { onAttachSubtitle(media.uriString) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Subtitles, contentDescription = "Attach subtitle", modifier = Modifier.size(18.dp))
                            }

                            IconButton(onClick = { onRefreshMetadata(media.uriString) }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Re-fetch metadata")
                            }

                            IconButton(onClick = { onDeleteLocal(media.uriString) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete local entry",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyVaultPlaceholder(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_vault),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddCloudMediaDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, url: String, provider: String, poster: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("WebDAV / Cloud") }
    var poster by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Personal Cloud Media") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Stream URL (WebDAV, S3, HTTPS MP4/HLS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("Storage Label (e.g. Nextcloud, R2, NAS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = poster,
                    onValueChange = { poster = it },
                    label = { Text("Optional Poster URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, url, provider, poster) },
                enabled = title.isNotBlank() && url.isNotBlank()
            ) {
                Text("Save & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return String.format("%.1f MB", mb)
}
