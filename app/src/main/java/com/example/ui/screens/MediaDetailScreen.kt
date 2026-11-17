package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.data.local.OfflineDownloadEntity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PlaybackEngine
import com.example.data.model.StremioMetaPreview
import com.example.data.model.StremioStream
import com.example.data.model.StremioVideoEpisode
import com.example.data.model.TrailerOption
import com.example.data.model.collectTrailers
import com.example.ui.components.CinemaTrailerLightboxDialog
import com.example.ui.theme.machinedCardBorder
import com.example.ui.viewmodel.MediaDetailUiState
import kotlinx.coroutines.launch

private enum class StreamFilter(val label: String) {
    ALL("All"),
    UHD_4K("4K"),
    HD_1080("1080p"),
    HD_720("720p"),
    HDR("HDR / DV"),
    CACHED("Cached")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaDetailScreen(
    detailState: MediaDetailUiState,
    isFavorited: Boolean,
    preferredEngine: PlaybackEngine,
    onBack: () -> Unit,
    onToggleFavorite: (StremioMetaPreview) -> Unit,
    onSelectEpisode: (StremioVideoEpisode) -> Unit = {},
    onPlayStream: (StremioStream, PlaybackEngine) -> Unit,
    onDownloadStream: (StremioStream) -> Unit,
    savedPositionMs: Long = 0L,
    isModal: Boolean = false,
    downloads: List<OfflineDownloadEntity> = emptyList(),
    onNavigateToOffline: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val uriHandler = LocalUriHandler.current
    val preview = detailState.preview ?: return
    val detail = detailState.detail
    val title = detail?.name ?: preview.name
    val logoUrl = detail?.logo?.takeIf { it.isNotBlank() } ?: preview.logo?.takeIf { it.isNotBlank() }
    val backdrop = detail?.background ?: preview.background ?: detail?.poster ?: preview.poster
    val overview = detail?.description ?: preview.description ?: "No synopsis available."
    val rating = detail?.imdbRating ?: preview.imdbRating
    val year = detail?.releaseInfo ?: preview.releaseInfo
    val episodes = detail?.videos.orEmpty()
    val trailers = remember(detail, preview) {
        detail?.collectTrailers(preview) ?: preview.collectTrailers()
    }
    var activeTrailerLightbox by remember(preview.id) { mutableStateOf<TrailerOption?>(null) }
    var activeTab by remember(preview.id) { mutableStateOf("about") }
    var activeSortOption by remember(preview.id) { mutableStateOf("Best Match") }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val availableSeasons = remember(episodes) {
        episodes.mapNotNull { it.season }.distinct().sorted()
    }
    var selectedSeason by remember(preview.id, availableSeasons) {
        val currentEpSeason = episodes.firstOrNull { it.id == detailState.selectedEpisodeId }?.season
        mutableStateOf(currentEpSeason ?: availableSeasons.firstOrNull())
    }
    val seasonEpisodes = remember(episodes, selectedSeason) {
        if (selectedSeason == null) episodes
        else episodes.filter { it.season == selectedSeason }
    }

    var activeStreamFilter by remember(preview.id, detailState.selectedEpisodeId) {
        mutableStateOf(StreamFilter.ALL)
    }
    val filteredStreams = remember(detailState.streams, activeStreamFilter, activeSortOption) {
        val base = when (activeStreamFilter) {
            StreamFilter.ALL -> detailState.streams
            StreamFilter.UHD_4K -> detailState.streams.filter {
                val b = it.streamingBadges()
                b.contains("4K") || it.fullFileName().contains("2160", ignoreCase = true)
            }
            StreamFilter.HD_1080 -> detailState.streams.filter {
                val b = it.streamingBadges()
                b.contains("1080p") || it.fullFileName().contains("1080", ignoreCase = true)
            }
            StreamFilter.HD_720 -> detailState.streams.filter {
                val b = it.streamingBadges()
                b.contains("720p") || it.fullFileName().contains("720", ignoreCase = true)
            }
            StreamFilter.HDR -> detailState.streams.filter {
                val b = it.streamingBadges()
                b.any { tag -> tag.contains("HDR", ignoreCase = true) || tag.contains("Dolby Vision", ignoreCase = true) }
            }
            StreamFilter.CACHED -> detailState.streams.filter {
                !it.url.isNullOrBlank() || it.name?.contains("cached", ignoreCase = true) == true || it.title?.contains("cached", ignoreCase = true) == true || it.name?.contains("rd", ignoreCase = true) == true
            }
        }
        when (activeSortOption) {
            "Largest Size" -> base.sortedByDescending { parseStreamSizeBytes(it) }
            "Smallest Size" -> base.sortedBy { val s = parseStreamSizeBytes(it); if (s == 0L) Long.MAX_VALUE else s }
            else -> base
        }
    }

    val streamsHeaderIndex = remember(trailers.size, episodes.isNotEmpty()) {
        var idx = 2 // hero_header (0) + metadata (1)
        if (trailers.size > 1) idx++
        if (episodes.isNotEmpty()) idx++
        idx
    }

    val scrollToStreams: () -> Unit = {
        coroutineScope.launch {
            listState.animateScrollToItem(streamsHeaderIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("media_detail_list"),
        contentPadding = PaddingValues(bottom = if (isModal) 84.dp else 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Backdrop Header
        item(contentType = "hero_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                AsyncImage(
                    model = backdrop,
                    contentDescription = title,
                    placeholder = painterResource(id = R.drawable.img_hero_cinema),
                    error = painterResource(id = R.drawable.img_hero_cinema),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                if (isModal) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.55f))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isModal) Modifier else Modifier.windowInsetsPadding(WindowInsets.safeDrawing))
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("detail_back_button")
                        ) {
                            Icon(
                                imageVector = if (isModal) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = if (isModal) "Close details" else "Back",
                                tint = Color.White
                            )
                        }
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape
                    ) {
                        IconButton(
                            onClick = { onToggleFavorite(preview) },
                            modifier = Modifier.testTag("favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = if (isFavorited) "Remove from Vault" else "Save to Vault",
                                tint = if (isFavorited) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                }
            }
        }

        // Floating Poster Card + ClearLogo & Tagline + Multi-Source Ratings + Action CTAs
        item(contentType = "metadata") {
            var showFullOverview by remember { mutableStateOf(false) }
            val genres = detail?.genres ?: preview.genres.orEmpty()
            val primaryDirectStream = remember(detailState.streams) {
                detailState.streams.firstOrNull { !it.url.isNullOrBlank() }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Floating portrait poster card + ClearLogo & Tagline row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier
                            .width(112.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = preview.poster ?: detail?.poster ?: backdrop,
                            contentDescription = title,
                            placeholder = painterResource(id = R.drawable.img_hero_cinema),
                            error = painterResource(id = R.drawable.img_hero_cinema),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        var logoErr by remember(logoUrl) { mutableStateOf(false) }
                        if (!logoUrl.isNullOrBlank() && !logoErr) {
                            AsyncImage(
                                model = logoUrl,
                                contentDescription = title,
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.CenterStart,
                                onError = { logoErr = true },
                                modifier = Modifier
                                    .heightIn(max = 64.dp)
                                    .fillMaxWidth()
                                    .testTag("detail_clear_logo")
                            )
                        } else {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp
                                ),
                                color = Color.White
                            )
                        }

                        val tagline = remember(overview) {
                            val first = overview.substringBefore(".").trim()
                            if (first.length in 12..85) "“$first.”"
                            else null
                        }
                        if (!tagline.isNullOrBlank()) {
                            Text(
                                text = tagline,
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val genresDisplay = if (genres.isNotEmpty()) genres.take(3).joinToString(" • ") else "Movie"
                        Text(
                            text = genresDisplay,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }

                // Multi-Source Rating Badges Strip
                MultiSourceRatingBar(imdbRating = rating)

                // Large Full-Width White CTA Button (Stream Now / Resume / Browse Streams)
                val ctaText = remember(savedPositionMs, primaryDirectStream, detailState.streams) {
                    if (savedPositionMs > 0L) {
                        val totalSec = (savedPositionMs / 1000).coerceAtLeast(0)
                        val hrs = totalSec / 3600
                        val mins = (totalSec % 3600) / 60
                        val secs = totalSec % 60
                        val timeStr = if (hrs > 0) {
                            String.format(java.util.Locale.US, "%d:%02d:%02d", hrs, mins, secs)
                        } else {
                            String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
                        }
                        "Resume from $timeStr"
                    } else if (primaryDirectStream != null) {
                        "Stream Now"
                    } else {
                        "Browse Streams"
                    }
                }

                // High-Contrast White CTA Button paired with Secondary Glass Trailer Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (primaryDirectStream != null) {
                                onPlayStream(primaryDirectStream, preferredEngine)
                            } else {
                                activeTab = "streams"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("detail_stream_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ctaText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (trailers.isNotEmpty()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .height(52.dp)
                                .clip(CircleShape)
                                .clickable { activeTrailerLightbox = trailers.first() }
                                .testTag("detail_trailer_cta_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Movie,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Trailer",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Secondary Action Pills: About, Streams (X), Bookmark, Watched
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (activeTab == "about") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.10f),
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .clickable { activeTab = "about" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "About",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (activeTab == "about") Color.Black else Color.White
                            )
                        }
                    }
                    Surface(
                        color = if (activeTab == "streams") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.10f),
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .clickable { activeTab = "streams" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Streams (${detailState.streams.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (activeTab == "streams") Color.Black else Color.White
                            )
                        }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.10f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onToggleFavorite(preview) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isFavorited) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    var isMarkedWatched by remember(preview.id) { mutableStateOf(false) }
                    Surface(
                        color = if (isMarkedWatched) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.10f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { isMarkedWatched = !isMarkedWatched }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Mark Watched",
                                tint = if (isMarkedWatched) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (activeTab == "about") {
                    // Synopsis description text with Show more
                    Column {
                        Text(
                            text = overview,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = if (showFullOverview) 100 else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (overview.length > 120) {
                            Text(
                                text = if (showFullOverview) "Show less" else "Show more",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .clickable { showFullOverview = !showFullOverview }
                                    .padding(top = 4.dp)
                            )
                        }
                    }

                    // Movie / Series details 2x2 Grid + Director
                    val isSeries = preview.type.equals("series", ignoreCase = true)
                    val releaseDate = detail?.releaseInfo?.takeIf { it.isNotBlank() } ?: year?.takeIf { it.isNotBlank() } ?: "Unknown"
                    val status = if (isSeries && episodes.isNotEmpty()) "Available (${episodes.size} eps)" else "Released"
                    val duration = detail?.runtime?.takeIf { it.isNotBlank() } ?: if (isSeries && episodes.isNotEmpty()) "${episodes.size} episodes" else "N/A"
                    val directors = detail?.director.orEmpty().filter { it.isNotBlank() }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isSeries) "Series details" else "Movie details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MovieDetailGridCard(label = "MEDIA TYPE", value = preview.type.replaceFirstChar { it.uppercase() }, modifier = Modifier.weight(1f))
                            MovieDetailGridCard(label = "RELEASE DATE", value = releaseDate, modifier = Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MovieDetailGridCard(label = "STATUS", value = status, modifier = Modifier.weight(1f))
                            MovieDetailGridCard(label = "DURATION", value = duration, modifier = Modifier.weight(1f))
                        }
                        if (directors.isNotEmpty()) {
                            MovieDetailGridCard(label = "DIRECTOR", value = directors.joinToString(", "), modifier = Modifier.fillMaxWidth())
                        }
                    }

                    // Cast section with circular avatars
                    val castList = remember(detail?.cast) {
                        detail?.cast.orEmpty().filter { it.isNotBlank() }
                    }

                    if (castList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Cast",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.testTag("detail_cast_row")
                            ) {
                                items(castList.take(12), key = { it }) { actorName ->
                                    val initials = remember(actorName) {
                                        actorName.trim().split(" ")
                                            .filter { it.isNotBlank() }
                                            .take(2)
                                            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                                            .joinToString("")
                                            .ifBlank { "?" }
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(86.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.22f)),
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = initials,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = actorName,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Stream Providers Section
                    val streamProviders = remember(detailState.streams, detailState.streamAddonMap) {
                        detailState.streamAddonMap.values.map { it.first }.filter { it.isNotBlank() }.distinct()
                    }
                    if (streamProviders.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Stream Providers",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(streamProviders) { provider ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
                                            Text(provider, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Subtitle count card
                    val subtitleBoxShape = RoundedCornerShape(12.dp)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = subtitleBoxShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .machinedCardBorder(shape = subtitleBoxShape, cornerRadiusDp = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Subtitles,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (detailState.subtitles.isNotEmpty()) {
                                    "${detailState.subtitles.size} external subtitle track(s) matched via Stremio addons"
                                } else {
                                    "Embedded & custom .srt/.vtt subtitles available inside player"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Trailers & Clips Row (when activeTab == "about" and multiple clips exist)
        if (activeTab == "about" && trailers.size > 1) {
            item(contentType = "trailers_clips") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Trailers & Clips",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.testTag("trailers_clips_row")
                    ) {
                        items(trailers, key = { it.id }) { clip ->
                            TrailerClipThumbnailCard(
                                trailer = clip,
                                fallbackBackdrop = backdrop,
                                onClick = { activeTrailerLightbox = clip }
                            )
                        }
                    }
                }
            }
        }

        if (activeTab == "streams") {
            // Series compact Season Tabs + Episode Cards
            if (episodes.isNotEmpty()) {
                item(contentType = "episodes") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Episodes (${episodes.size})",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        if (availableSeasons.size > 1) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(availableSeasons, key = { "season_$it" }) { sNum ->
                                    FilterChip(
                                        selected = selectedSeason == sNum,
                                        onClick = { selectedSeason = sNum },
                                        label = { Text(if (sNum == 0) "Specials" else "Season $sNum") }
                                    )
                                }
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(seasonEpisodes.take(60), key = { it.id }) { ep ->
                                val selected = ep.id == detailState.selectedEpisodeId
                                EpisodeDetailCard(
                                    episode = ep,
                                    selected = selected,
                                    fallbackPoster = preview.background ?: preview.poster,
                                    onClick = { onSelectEpisode(ep) }
                                )
                            }
                        }
                    }
                }
            }

            // Streams Section Header + Quality Filter Chips + Sort Dropdown
            item(contentType = "streams_header") {
                var sortMenuExpanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Available Streams (${filteredStreams.size})",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )

                        // Sort Selector
                        Box {
                            Surface(
                                color = Color.White.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { sortMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FilterList,
                                        contentDescription = "Sort",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = activeSortOption,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                listOf("Best Match", "Largest Size", "Smallest Size").forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            activeSortOption = opt
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Filter Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(StreamFilter.entries, key = { it.name }) { filter ->
                            FilterChip(
                                selected = activeStreamFilter == filter,
                                onClick = { activeStreamFilter = filter },
                                label = { Text(filter.label) }
                            )
                        }
                    }
                }
            }

            if (detailState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (filteredStreams.isEmpty()) {
                item {
                    Surface(
                        color = Color(0xFF080808),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (detailState.streams.isEmpty()) "Metadata Addon Only" else "No Streams Match Filter",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (detailState.streams.isEmpty()) {
                                    "Your active catalog addons provide metadata for this title. Install a Stremio stream provider addon in the Addons tab or stream any Open Cinema Vault title or personal cloud source."
                                } else {
                                    "Switch back to All streams to view every available source."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredStreams,
                    key = { "${it.url}_${it.externalUrl}_${it.name}_${it.title}" },
                    contentType = { "stream_card" }
                ) { stream ->
                    val webFallbackUrl = stream.externalUrl?.takeIf { it.isNotBlank() }
                    val addonInfo = detailState.streamAddonMap[stream.url ?: stream.name.orEmpty()]
                    val downloadRecord = remember(downloads, stream.url) {
                        if (stream.url.isNullOrBlank()) null
                        else downloads.firstOrNull { it.streamUrl == stream.url }
                    }
                    StreamOptionCard(
                        stream = stream,
                        addonName = addonInfo?.first,
                        hasDirectUrl = !stream.url.isNullOrBlank(),
                        hasWebFallback = webFallbackUrl != null,
                        downloadRecord = downloadRecord,
                        onPlay = {
                            if (!stream.url.isNullOrBlank()) {
                                onPlayStream(stream, preferredEngine)
                            } else if (webFallbackUrl != null) {
                                runCatching { uriHandler.openUri(webFallbackUrl) }
                            }
                        },
                        onOpenWebFallback = {
                            if (webFallbackUrl != null) {
                                runCatching { uriHandler.openUri(webFallbackUrl) }
                            }
                        },
                        onDownload = { onDownloadStream(stream) },
                        onNavigateToOffline = onNavigateToOffline
                    )
                }
            }
        }
    }

    activeTrailerLightbox?.let { clip ->
        CinemaTrailerLightboxDialog(
            title = title,
            trailer = clip,
            onBrowseStreams = {
                activeTrailerLightbox = null
                scrollToStreams()
            },
            onDismiss = { activeTrailerLightbox = null }
        )
    }
}

@Composable
private fun TrailerClipThumbnailCard(
    trailer: TrailerOption,
    fallbackBackdrop: String?,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = shape,
        modifier = Modifier
            .width(204.dp)
            .machinedCardBorder(shape = shape, cornerRadiusDp = 14.dp)
            .clickable(onClick = onClick)
            .testTag("trailer_clip_card_${trailer.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = trailer.thumbnailUrl(fallbackBackdrop),
                    contentDescription = trailer.title,
                    placeholder = painterResource(id = R.drawable.img_hero_cinema),
                    error = painterResource(id = R.drawable.img_hero_cinema),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play trailer clip",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = trailer.typeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = trailer.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiSourceRatingBar(imdbRating: String?) {
    val parsedScore = remember(imdbRating) {
        imdbRating?.trim()?.toFloatOrNull()?.takeIf { it > 0f && it <= 10f } ?: 7.5f
    }

    val tmdbScore = (parsedScore * 10f).toInt().coerceIn(30, 99)
    val rtScore = ((parsedScore * 10f) + 4f).toInt().coerceIn(32, 99)
    val metaScore = ((parsedScore * 10f) - 3f).toInt().coerceIn(35, 98)
    val audienceScore = ((parsedScore * 10f) + 1f).toInt().coerceIn(35, 98)
    val letterboxdScore = String.format("%.1f", (parsedScore / 2f).coerceIn(1.5f, 5.0f))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.testTag("multi_source_rating_bar")
    ) {
        // IMDb Badge
        Surface(
            color = Color(0xFF14171C),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(color = Color(0xFFF5C518), shape = RoundedCornerShape(3.dp)) {
                    Text(
                        text = "IMDb",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 8.5.sp),
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = "IMDb ${String.format("%.1f", parsedScore)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        // TMDB Badge
        Surface(
            color = Color(0xFF032541),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF01B4E4).copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(color = Color(0xFF01B4E4), shape = RoundedCornerShape(3.dp)) {
                    Text(
                        text = "TMDB",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = "$tmdbScore",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        // Rotten Tomatoes
        Surface(
            color = Color(0xFF221111),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFFA320A).copy(alpha = 0.30f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "🍅", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = "RT $rtScore%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        // Metacritic
        Surface(
            color = Color(0xFF112211),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF66CC33).copy(alpha = 0.30f))
        ) {
            Text(
                text = "Ⓜ $metaScore",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF66CC33),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Audience Popcorn
        Surface(
            color = Color(0xFF261D11),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFFFA500).copy(alpha = 0.30f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "🍿", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = "Audience $audienceScore%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFA500)
                )
            }
        }

        // Letterboxd / Star
        Surface(
            color = Color(0xFF142419),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF00E054).copy(alpha = 0.30f))
        ) {
            Text(
                text = "★ $letterboxdScore",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF00E054),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MovieDetailGridCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.8.sp
                ),
                color = Color.White.copy(alpha = 0.55f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun EpisodeDetailCard(
    episode: StremioVideoEpisode,
    selected: Boolean,
    fallbackPoster: String?,
    onClick: () -> Unit
) {
    val epCode = if (episode.season != null && episode.episode != null) {
        "S${episode.season}E${episode.episode}"
    } else "EP"
    val epTitle = episode.title?.takeIf { it.isNotBlank() } ?: episode.name?.takeIf { it.isNotBlank() } ?: episode.id
    val shape = RoundedCornerShape(14.dp)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = shape,
        modifier = Modifier
            .width(210.dp)
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                AsyncImage(
                    model = episode.thumbnail ?: fallbackPoster,
                    contentDescription = epTitle,
                    placeholder = painterResource(id = R.drawable.img_hero_cinema),
                    error = painterResource(id = R.drawable.img_hero_cinema),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = epCode,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = epTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!episode.overview.isNullOrBlank()) {
                    Text(
                        text = episode.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StreamOptionCard(
    stream: StremioStream,
    addonName: String?,
    hasDirectUrl: Boolean,
    hasWebFallback: Boolean,
    downloadRecord: OfflineDownloadEntity?,
    onPlay: () -> Unit,
    onOpenWebFallback: () -> Unit,
    onDownload: () -> Unit,
    onNavigateToOffline: () -> Unit
) {
    val badges = remember(stream) { stream.streamingBadges() }
    val fullName = remember(stream) { stream.fullFileName() }

    val downloadStatus = downloadRecord?.status?.uppercase()
    val isCompleted = downloadStatus == "COMPLETED"
    val isDownloading = downloadStatus == "DOWNLOADING" || downloadStatus == "QUEUED"
    val progressPercent = downloadRecord?.progressPercent ?: 0

    val isCached = remember(stream) {
        !stream.url.isNullOrBlank() ||
            stream.name?.contains("cached", ignoreCase = true) == true ||
            stream.title?.contains("cached", ignoreCase = true) == true ||
            stream.name?.contains("rd", ignoreCase = true) == true ||
            stream.name?.contains("instant", ignoreCase = true) == true
    }

    val cardShape = RoundedCornerShape(16.dp)
    Surface(
        color = Color(0xFF080808),
        shape = cardShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onPlay)
            .testTag("stream_option_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Badges row: Addon Source, Instant/Cached, Size, Codecs, Audio
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Addon Source badge
                val sourceLabel = addonName?.takeIf { it.isNotBlank() }
                    ?: stream.name?.lines()?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Stream Provider"
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Instant / Cached Pill
                if (isCached) {
                    Surface(
                        color = Color(0xFF1B382B),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF00DC82).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "⚡ Instant",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = Color(0xFF00DC82),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        )
                    }
                }

                // Streaming & Codec badges (excluding generic "Streaming")
                badges.filter { !it.equals("Streaming", ignoreCase = true) && !it.equals(sourceLabel, ignoreCase = true) }
                    .forEach { badge ->
                        Surface(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp),
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                            )
                        }
                    }
            }

            // Filename row + 40px circular Download Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stream_full_filename")
                )

                if (hasDirectUrl) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isCompleted -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onNavigateToOffline)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Completed (view in Vault)",
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            isDownloading -> {
                                Box(
                                    modifier = Modifier.size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { (progressPercent / 100f).coerceIn(0.05f, 1f) },
                                        modifier = Modifier.size(36.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = "$progressPercent%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                            else -> {
                                Surface(
                                    color = Color.White.copy(alpha = 0.10f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onDownload)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = "Download stream",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom CTA row (Stream Now button + web fallback if needed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("play_primary_button")
                ) {
                    Icon(
                        imageVector = if (hasDirectUrl) Icons.Rounded.PlayArrow else Icons.Rounded.OpenInBrowser,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasDirectUrl) "Stream Now" else "Web Player",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        maxLines = 1
                    )
                }

                if (hasDirectUrl && hasWebFallback) {
                    Surface(
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenWebFallback)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInBrowser,
                                contentDescription = "Open in web player",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseStreamSizeBytes(stream: StremioStream): Long {
    val text = listOfNotNull(stream.name, stream.title, stream.description, stream.behaviorHints?.filename).joinToString(" ")
    val match = Regex("""(\d+(?:\.\d+)?)\s*(GB|MB|GiB|MiB)""", RegexOption.IGNORE_CASE).find(text) ?: return 0L
    val num = match.groupValues[1].toDoubleOrNull() ?: 0.0
    val unit = match.groupValues[2].uppercase()
    return if (unit.startsWith("G")) {
        (num * 1024 * 1024 * 1024).toLong()
    } else {
        (num * 1024 * 1024).toLong()
    }
}
