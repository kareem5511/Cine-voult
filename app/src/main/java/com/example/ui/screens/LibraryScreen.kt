package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.DownloadStatus
import com.example.data.local.LocalMediaEntity
import com.example.data.local.MetadataMatchStatus
import com.example.data.local.OfflineDownloadEntity
import com.example.data.model.LibraryMediaItem
import com.example.data.model.PlaybackEngine
import com.example.data.model.StremioMetaPreview
import com.example.player.PlaybackRequest
import com.example.ui.theme.AmoledCard
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
    onOpenPreview: ((StremioMetaPreview) -> Unit)? = null,
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

    val allLibItems = (libraryState as? UiState.Success)?.data.orEmpty()
    val watchlistItems = remember(allLibItems) {
        allLibItems.filter { it.streamUrl.isBlank() || it.isFavorite }
    }
    val cloudItems = remember(allLibItems) {
        allLibItems.filter { it.streamUrl.isNotBlank() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Safe Area Top Header
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = 8.dp)
        ) {
            Text(
                text = "Media Vault",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Watchlist, Cloud, Offline Downloads & Local Media",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.60f)
            )
        }

        // 4 Sub-Tabs with Counter Pills
        val tabTitles = listOf(
            "Watchlist" to watchlistItems.size,
            "Cloud" to cloudItems.size,
            "Offline" to downloads.size,
            "Local" to localMedia.size
        )

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Black,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 2.5.dp
                    )
                }
            }
        ) {
            tabTitles.forEachIndexed { index, (label, count) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("vault_tab_$label"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.55f)
                            )
                            Surface(
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.60f),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> WatchlistTab(
                items = watchlistItems,
                preferredEngine = preferredEngine,
                onRemoveItem = onRemoveLibraryItem,
                onOpenPreview = onOpenPreview,
                onPlayRequest = onPlayRequest
            )
            1 -> CloudVaultTab(
                cloudItems = cloudItems,
                downloads = downloads,
                localMediaCount = localMedia.size,
                preferredEngine = preferredEngine,
                onOpenAddCloud = { showCloudDialog = true },
                onDeleteItem = onRemoveLibraryItem,
                onPlayRequest = onPlayRequest
            )
            2 -> OfflineDownloadsTab(
                downloads = downloads,
                preferredEngine = preferredEngine,
                onPlayRequest = onPlayRequest,
                onDeleteDownload = onDeleteDownload
            )
            3 -> LocalMediaTab(
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

// -------------------------------------------------------------
// Sub-Tab 1: Watchlist (Grid with IMDb rating & Remove button)
// -------------------------------------------------------------
@Composable
private fun WatchlistTab(
    items: List<LibraryMediaItem>,
    preferredEngine: PlaybackEngine,
    onRemoveItem: (String) -> Unit,
    onOpenPreview: ((StremioMetaPreview) -> Unit)?,
    onPlayRequest: (PlaybackRequest) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 112.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyVaultPlaceholder(
                title = "Your Watchlist is Empty",
                subtitle = "Tap the bookmark icon on any movie or series to save it for later."
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items, key = { it.mediaKey }) { item ->
                WatchlistPosterCard(
                    item = item,
                    preferredEngine = preferredEngine,
                    onRemove = { onRemoveItem(item.mediaKey) },
                    onClick = {
                        if (onOpenPreview != null) {
                            onOpenPreview(
                                StremioMetaPreview(
                                    id = item.mediaKey,
                                    type = item.mediaType,
                                    name = item.title,
                                    poster = item.posterUrl.ifBlank { null },
                                    background = item.backdropUrl.ifBlank { null },
                                    description = item.overview.ifBlank { null },
                                    releaseInfo = item.year.ifBlank { null },
                                    imdbRating = item.imdbRating.ifBlank { null }
                                )
                            )
                        } else if (item.streamUrl.isNotBlank()) {
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
                    }
                )
            }
        }
    }
}

@Composable
private fun WatchlistPosterCard(
    item: LibraryMediaItem,
    preferredEngine: PlaybackEngine,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(AmoledCard)
                .machinedCardBorder(cornerRadiusDp = 12.dp)
        ) {
            if (item.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.CloudQueue,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Remove Button (Top-Left)
            Surface(
                color = Color.Black.copy(alpha = 0.70f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onRemove() }
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove from Watchlist",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxSize()
                )
            }

            // IMDb Rating Badge (Top-Right)
            if (item.imdbRating.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFF5A623).copy(alpha = 0.40f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFF5A623),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = item.imdbRating,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Watch progress bar if partially watched
            if (item.durationMs > 0L && item.watchProgressMs > 0L) {
                val pct = (item.watchProgressMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.20f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pct)
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = listOfNotNull(item.year.takeIf { it.isNotBlank() }, item.mediaType.uppercase()).joinToString(" • "),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.50f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -------------------------------------------------------------
// Sub-Tab 2: Cloud Vault (Personal Cloud Streams & Hub)
// -------------------------------------------------------------
@Composable
private fun CloudVaultTab(
    cloudItems: List<LibraryMediaItem>,
    downloads: List<OfflineDownloadEntity>,
    localMediaCount: Int,
    preferredEngine: PlaybackEngine,
    onOpenAddCloud: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onPlayRequest: (PlaybackRequest) -> Unit
) {
    val totalCachedBytes = remember(downloads) { downloads.sumOf { it.bytesDownloaded } }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            val hubShape = RoundedCornerShape(18.dp)
            Card(
                shape = hubShape,
                colors = CardDefaults.cardColors(containerColor = AmoledCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .machinedCardBorder(shape = hubShape, cornerRadiusDp = 18.dp)
                    .testTag("storage_cloud_hub_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
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
                                    color = Color.White
                                )
                                Text(
                                    text = "${cloudItems.size} Cloud • ${downloads.size} Offline (${formatBytes(totalCachedBytes)}) • $localMediaCount Local",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.60f)
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

                        val cloudUnits = cloudItems.size.coerceAtLeast(1).toFloat()
                        val offlineUnits = downloads.size.coerceAtLeast(1).toFloat()
                        val localUnits = localMediaCount.coerceAtLeast(1).toFloat()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
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

        if (cloudItems.isEmpty()) {
            item {
                EmptyVaultPlaceholder(
                    title = "No Cloud Streams Linked",
                    subtitle = "Tap 'Link' above to connect WebDAV, Telegram channels, or personal cloud video streams."
                )
            }
        } else {
            items(cloudItems, key = { it.mediaKey }) { item ->
                val itemShape = RoundedCornerShape(14.dp)
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmoledCard),
                    shape = itemShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .machinedCardBorder(shape = itemShape, cornerRadiusDp = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 90.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f)),
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
                                    tint = MaterialTheme.colorScheme.primary
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.sourceLabel} • ${item.mediaType.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                            if (item.durationMs > 0L && item.watchProgressMs > 0L) {
                                val pct = (item.watchProgressMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
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
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
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

// -------------------------------------------------------------
// Sub-Tab 3: Offline Downloads (Active & Completed sections)
// -------------------------------------------------------------
@Composable
private fun OfflineDownloadsTab(
    downloads: List<OfflineDownloadEntity>,
    preferredEngine: PlaybackEngine,
    onPlayRequest: (PlaybackRequest) -> Unit,
    onDeleteDownload: (String) -> Unit
) {
    val activeDownloads = remember(downloads) {
        downloads.filter { it.status != DownloadStatus.COMPLETED.name }
    }
    val completedDownloads = remember(downloads) {
        downloads.filter { it.status == DownloadStatus.COMPLETED.name && it.localFilePath.isNotBlank() }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (downloads.isEmpty()) {
            item {
                EmptyVaultPlaceholder(
                    title = "No Offline Downloads Yet",
                    subtitle = "Tap the download button on any stream to watch completely offline without internet."
                )
            }
        } else {
            // Section 1: Active Downloads
            if (activeDownloads.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Downloads (${activeDownloads.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                items(activeDownloads, key = { it.downloadKey }) { dl ->
                    val dlShape = RoundedCornerShape(14.dp)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AmoledCard),
                        shape = dlShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .machinedCardBorder(shape = dlShape, cornerRadiusDp = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { dl.progressPercent / 100f },
                                    modifier = Modifier.size(46.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.White.copy(alpha = 0.10f),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "${dl.progressPercent}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dl.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${dl.qualityLabel} • ${dl.status.lowercase().replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (dl.totalBytes > 0L) {
                                    Text(
                                        text = "${formatBytes(dl.bytesDownloaded)} of ${formatBytes(dl.totalBytes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.50f)
                                    )
                                }
                            }

                            IconButton(onClick = { onDeleteDownload(dl.downloadKey) }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Cancel download",
                                    tint = Color.White.copy(alpha = 0.70f)
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Completed Downloads
            if (completedDownloads.isNotEmpty()) {
                item {
                    Text(
                        text = "Downloaded (${completedDownloads.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                items(completedDownloads, key = { it.downloadKey }) { dl ->
                    val dlShape = RoundedCornerShape(14.dp)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AmoledCard),
                        shape = dlShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .machinedCardBorder(shape = dlShape, cornerRadiusDp = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 56.dp, height = 80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dl.posterUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = dl.posterUrl,
                                        contentDescription = dl.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.DownloadDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dl.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${dl.qualityLabel} • ${formatBytes(dl.bytesDownloaded)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Ready for offline playback",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.50f)
                                )
                            }

                            Row {
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
                                        Icons.Rounded.PlayArrow,
                                        contentDescription = "Play offline",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
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
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Sub-Tab 4: Local Media (Scanner, Metadata Matching, Filter/Sort)
// -------------------------------------------------------------
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
    var sortOrder by remember { mutableStateOf("Newest Added") }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedMedia = remember(localMedia, sortOrder) {
        when (sortOrder) {
            "File Size (Large-Small)" -> localMedia.sortedByDescending { it.fileSizeBytes }
            "Title (A-Z)" -> localMedia.sortedBy { it.parsedTitle.lowercase() }
            else -> localMedia.sortedByDescending { it.addedAtMs }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            val localBannerShape = RoundedCornerShape(18.dp)
            Card(
                colors = CardDefaults.cardColors(containerColor = AmoledCard),
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Import MKV/MP4 files. CineVault parses filenames and fetches high-res Cinemeta posters automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.60f)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onPickVideo,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("import_local_video_button")
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import")
                    }
                }
            }
        }

        if (localMedia.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Imported Videos (${localMedia.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Box {
                        Surface(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showSortMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Sort, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(
                                    text = sortOrder,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            listOf("Newest Added", "File Size (Large-Small)", "Title (A-Z)").forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        sortOrder = opt
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (sortedMedia.isEmpty()) {
            item {
                EmptyVaultPlaceholder(
                    title = "No Local Videos Imported",
                    subtitle = "Tap 'Import' to add videos from your device storage with automatic IMDb poster and metadata matching."
                )
            }
        } else {
            items(sortedMedia, key = { it.uriString }) { media ->
                val localShape = RoundedCornerShape(14.dp)
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmoledCard),
                    shape = localShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .machinedCardBorder(shape = localShape, cornerRadiusDp = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(width = 68.dp, height = 96.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.05f)),
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
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                val metaLine = listOfNotNull(
                                    media.parsedYear.takeIf { it.isNotBlank() },
                                    media.resolution.takeIf { it.isNotBlank() },
                                    media.imdbRating.takeIf { it.isNotBlank() }?.let { "★ $it" }
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
                                        MetadataMatchStatus.MATCHED.name -> "Metadata matched"
                                        MetadataMatchStatus.PENDING.name -> "Retrieving metadata..."
                                        else -> "Local file • ${media.qualityTags.ifBlank { media.mimeType }}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.50f)
                                )
                            }
                        }

                        if (media.overview.isNotBlank()) {
                            Text(
                                text = media.overview,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
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
                                Icon(Icons.Rounded.Refresh, contentDescription = "Re-fetch metadata", tint = Color.White)
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
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_vault),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
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
    return if (mb >= 1024.0) {
        String.format("%.2f GB", mb / 1024.0)
    } else {
        String.format("%.1f MB", mb)
    }
}
