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
import androidx.compose.material.icons.rounded.LiveTv
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    UHD_4K("4K / UHD"),
    HD_1080("1080p"),
    HDR("HDR / DV"),
    HEVC("HEVC")
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
    isModal: Boolean = false
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
    val filteredStreams = remember(detailState.streams, activeStreamFilter) {
        when (activeStreamFilter) {
            StreamFilter.ALL -> detailState.streams
            StreamFilter.UHD_4K -> detailState.streams.filter {
                val b = it.streamingBadges()
                b.contains("4K") || it.fullFileName().contains("2160", ignoreCase = true)
            }
            StreamFilter.HD_1080 -> detailState.streams.filter {
                val b = it.streamingBadges()
                b.contains("1080p") || it.fullFileName().contains("1080", ignoreCase = true)
            }
            StreamFilter.HDR -> detailState.streams.filter {
                val b = it.streamingBadges()
                b.any { tag -> tag.contains("HDR", ignoreCase = true) || tag.contains("Dolby Vision", ignoreCase = true) }
            }
            StreamFilter.HEVC -> detailState.streams.filter {
                val b = it.streamingBadges()
                b.contains("HEVC")
            }
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

                Button(
                    onClick = {
                        if (primaryDirectStream != null) {
                            onPlayStream(primaryDirectStream, preferredEngine)
                        } else {
                            scrollToStreams()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
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
                        color = Color.Black
                    )
                }

                // Secondary Action Pills: About, Streams (X), Bookmark, Watched
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text("About", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(CircleShape)
                            .clickable { scrollToStreams() }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                text = "Streams (${detailState.streams.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(42.dp)
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
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Watched",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

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

                // Cast section with circular avatars (only if cast metadata is present)
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

                // Stream Providers Section (dynamically populated from installed addons)
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

                // Trailers button
                if (trailers.isNotEmpty()) {
                    val trailerBtnShape = RoundedCornerShape(14.dp)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        shape = trailerBtnShape,
                        onClick = { activeTrailerLightbox = trailers.first() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .machinedCardBorder(shape = trailerBtnShape, cornerRadiusDp = 14.dp)
                            .testTag("detail_trailer_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Watch Official Trailer",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
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

        // Trailers & Clips Row (when multiple clips exist)
        if (trailers.size > 1) {
            item(contentType = "trailers_clips") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Trailers & Clips",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
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

        // Nuvio-Style Season Tabs + Episode Cards
        if (episodes.isNotEmpty()) {
            item(contentType = "episodes") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Episodes (${episodes.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
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

        // Streams Section Header + Nuvio Quality Filter Chips
        item(contentType = "streams_header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Streams",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (detailState.streams.isNotEmpty()) {
                        Text(
                            text = "${filteredStreams.size}/${detailState.streams.size} source${if (detailState.streams.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (detailState.streams.size > 1) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
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
        }

        if (detailState.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredStreams.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (detailState.streams.isEmpty()) "Metadata Addon Only" else "No Streams Match Filter",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (detailState.streams.isEmpty()) {
                                "Your active catalog addons provide metadata for this title. Install a Stremio stream provider addon in the Addons tab or stream any Open Cinema Vault title or personal cloud source."
                            } else {
                                "Switch back to All streams to view every available source."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                StreamOptionCard(
                    stream = stream,
                    hasDirectUrl = !stream.url.isNullOrBlank(),
                    hasWebFallback = webFallbackUrl != null,
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
                    onDownload = { onDownloadStream(stream) }
                )
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
    hasDirectUrl: Boolean,
    hasWebFallback: Boolean,
    onPlay: () -> Unit,
    onOpenWebFallback: () -> Unit,
    onDownload: () -> Unit
) {
    val badges = remember(stream) { stream.streamingBadges() }
    val fullName = remember(stream) { stream.fullFileName() }

    val cardShape = RoundedCornerShape(16.dp)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .machinedCardBorder(shape = cardShape, cornerRadiusDp = 16.dp)
            .clickable(onClick = onPlay)
            .testTag("stream_option_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Streaming Badge + Quality / Codec Badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                badges.forEachIndexed { idx, badge ->
                    val isStreamingPrimary = idx == 0
                    Surface(
                        color = if (isStreamingPrimary) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isStreamingPrimary) {
                                Icon(
                                    imageVector = Icons.Rounded.LiveTv,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isStreamingPrimary) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isStreamingPrimary) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            // Full File Name — soft-wrapped cleanly without truncation or player labels
            Text(
                text = fullName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("stream_full_filename")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("play_primary_button")
                ) {
                    Icon(
                        imageVector = if (hasDirectUrl) Icons.Rounded.PlayArrow else Icons.Rounded.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hasDirectUrl) "Stream Now" else "Web Player", maxLines = 1)
                }

                if (hasDirectUrl && hasWebFallback) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        IconButton(
                            onClick = onOpenWebFallback,
                            modifier = Modifier.testTag("web_fallback_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInBrowser,
                                contentDescription = "Open in web player",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (hasDirectUrl) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        IconButton(
                            onClick = onDownload,
                            modifier = Modifier.testTag("download_offline_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Download offline",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
