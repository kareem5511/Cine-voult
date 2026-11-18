package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.example.data.model.PosterLayout
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.LocalMovies
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AmoledCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import android.content.Context
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.LibraryMediaItem
import com.example.data.model.PlaybackEngine
import com.example.data.model.StremioMetaDetail
import com.example.data.model.StremioMetaPreview
import com.example.data.model.collectTrailers
import com.example.ui.components.InlineSpotlightTrailerPlayer
import com.example.ui.components.isTrailerAutoPlayAllowed
import com.example.ui.theme.LocalIsAmoledBlack
import com.example.ui.theme.machinedCardBorder
import com.example.ui.viewmodel.CatalogSection
import com.example.ui.viewmodel.DiscoverUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    discoverState: DiscoverUiState,
    continueWatching: List<LibraryMediaItem> = emptyList(),
    compactGrid: Boolean,
    posterLayout: String = "STANDARD_2",
    preferredEngine: PlaybackEngine,
    autoPlayTrailers: String = "WIFI_ONLY",
    spotlightMetaMap: Map<String, StremioMetaDetail> = emptyMap(),
    onResolveSpotlightMeta: (StremioMetaPreview) -> Unit = {},
    onSearchChange: (String) -> Unit,
    onTypeSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectMedia: (StremioMetaPreview) -> Unit,
    onResumeItem: (LibraryMediaItem) -> Unit = {},
    onRestartItem: (LibraryMediaItem) -> Unit = {},
    onMarkAsWatched: (LibraryMediaItem) -> Unit = {},
    onRemoveContinueWatching: (LibraryMediaItem) -> Unit = {},
    onQuickFavorite: (StremioMetaPreview) -> Unit,
    onSelectCatalogGenre: (sectionKey: String, genre: String?) -> Unit = { _, _ -> },
    onLoadMoreCatalog: (sectionKey: String) -> Unit = {},
    onToggleCatalogVisibility: (sectionKey: String) -> Unit = {},
    onMoveCatalogOrder: (sectionKey: String, direction: Int) -> Unit = { _, _ -> },
    onResetHiddenCatalogs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCustomizeSheet by remember { mutableStateOf(false) }
    var activeBrowseSectionKey by remember { mutableStateOf<String?>(null) }
    var searchExpanded by remember { mutableStateOf(false) }
    var selectedCwItemForOptions by remember { mutableStateOf<LibraryMediaItem?>(null) }
    val listState = rememberLazyListState()
    val isHeroInView by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    val availableTypes = remember(discoverState.sections) {
        val extras = discoverState.sections.map { it.type.lowercase() }.distinct()
            .filter { it !in listOf("movie", "series") }
        listOf("all" to "All", "movie" to "Movies", "series" to "Series") +
            extras.map { it to it.replaceFirstChar { c -> c.uppercase() } }
    }
    val visibleSections = remember(
        discoverState.sections,
        discoverState.selectedType,
        discoverState.hiddenSectionKeys
    ) {
        discoverState.sections.filter { section ->
            !discoverState.hiddenSectionKeys.contains(section.sectionKey) &&
                (discoverState.selectedType == "all" ||
                    section.type.equals(discoverState.selectedType, ignoreCase = true))
        }
    }
    val heroItems = remember(visibleSections) {
        visibleSections.flatMap { it.items }
            .distinctBy { it.id }
            .filter { !it.poster.isNullOrBlank() || !it.background.isNullOrBlank() }
            .take(5)
    }
    val showFullBleedHero = heroItems.isNotEmpty() && discoverState.searchQuery.isBlank()
    val isAmoled = LocalIsAmoledBlack.current
    val scrimBaseColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.background

    androidx.activity.compose.BackHandler(enabled = searchExpanded) {
        searchExpanded = false
    }
    androidx.activity.compose.BackHandler(enabled = activeBrowseSectionKey != null) {
        activeBrowseSectionKey = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Minimal Floating Top Controls (Frosted-Glass Search & Spin Refresh)
        val infiniteRefresh = rememberInfiniteTransition(label = "refresh_spin")
        val spinRotation by infiniteRefresh.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin_angle"
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 10.dp, end = 16.dp)
                .zIndex(45f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = CircleShape,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { searchExpanded = true }
                    .testTag("discover_floating_search_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = CircleShape,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !discoverState.isLoading) { onRefresh() }
                    .testTag("discover_floating_refresh_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh catalogs",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                if (discoverState.isLoading) {
                                    rotationZ = spinRotation
                                }
                            }
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("discover_feed_list"),
            contentPadding = PaddingValues(top = 0.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (showFullBleedHero) {
                item(contentType = "hero_carousel") {
                    FeaturedHeroCarousel(
                        items = heroItems,
                        spotlightMetaMap = spotlightMetaMap,
                        autoPlayTrailers = autoPlayTrailers,
                        isHeroInView = isHeroInView,
                        onResolveSpotlightMeta = onResolveSpotlightMeta,
                        onWatchClick = onSelectMedia,
                        onSaveClick = onQuickFavorite
                    )
                }

                // Media Type Filter Row right below full-bleed Spotlight
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableTypes, key = { it.first }) { (key, label) ->
                            val isSelected = discoverState.selectedType == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { onTypeSelected(key) },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF161616),
                                    labelColor = Color.White.copy(alpha = 0.80f),
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("filter_chip_$key")
                            )
                        }
                    }
                }
            } else {
                // Top spacing below status bar scrim + Type Filters
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableTypes, key = { it.first }) { (key, label) ->
                                FilterChip(
                                    selected = discoverState.selectedType == key,
                                    onClick = { onTypeSelected(key) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("filter_chip_$key")
                                )
                            }
                        }
                    }
                }
            }

        // Nuvio-Style Continue Watching Rail
        if (continueWatching.isNotEmpty() && discoverState.searchQuery.isBlank()) {
            item(contentType = "continue_watching") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Continue Watching",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = Color.White
                            )
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "See All Continue Watching",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(3.5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.55f))
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(continueWatching, key = { "cw_${it.mediaKey}" }) { item ->
                            ContinueWatchingCard(
                                item = item,
                                onClick = { onResumeItem(item) },
                                onLongClick = { selectedCwItemForOptions = item }
                            )
                        }
                    }
                }
            }
        }

        if (discoverState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (visibleSections.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_vault),
                        contentDescription = "No catalog results",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = discoverState.errorMessage ?: "No titles available for this filter.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (discoverState.hiddenSectionKeys.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(onClick = onResetHiddenCatalogs) {
                            Text("Show Hidden Catalogs (${discoverState.hiddenSectionKeys.size})")
                        }
                    }
                }
            }
        } else {
            items(
                items = visibleSections,
                key = { it.sectionKey },
                contentType = { "catalog_section" }
            ) { section ->
                val typeLabel = if (section.type.equals("series", ignoreCase = true)) "Series" else "Movies"
                val sectionTitle = remember(section.catalogTitle, section.addonName, typeLabel, visibleSections) {
                    val rawTitle = if (section.catalogTitle.contains("•") || section.catalogTitle.contains("(")) {
                        section.catalogTitle
                    } else {
                        "${section.catalogTitle} • $typeLabel"
                    }
                    val duplicates = visibleSections.count { it.catalogTitle.equals(section.catalogTitle, ignoreCase = true) }
                    if (duplicates > 1 && section.addonName.isNotBlank() && !rawTitle.contains(section.addonName, ignoreCase = true)) {
                        "$rawTitle • ${section.addonName}"
                    } else {
                        rawTitle
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sectionTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { activeBrowseSectionKey = section.sectionKey }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "See All ${section.catalogTitle}",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(3.5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.55f))
                        )
                    }

                    val parsedPosterLayout = remember(posterLayout, compactGrid) {
                        if (posterLayout.isNotBlank()) com.example.data.model.PosterLayout.fromKey(posterLayout)
                        else if (compactGrid) com.example.data.model.PosterLayout.COMPACT_3
                        else com.example.data.model.PosterLayout.STANDARD_2
                    }
                    val cardWidth = when (parsedPosterLayout) {
                        com.example.data.model.PosterLayout.BENTO_4 -> 105.dp
                        com.example.data.model.PosterLayout.COMPACT_3 -> 118.dp
                        com.example.data.model.PosterLayout.STANDARD_2 -> 142.dp
                        com.example.data.model.PosterLayout.CINEMATIC_1 -> 180.dp
                        com.example.data.model.PosterLayout.LIST_DETAILED -> 150.dp
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = section.items,
                            key = { "${section.sectionKey}_${it.id}" },
                            contentType = { "poster_card" }
                        ) { meta ->
                            PosterMediaCard(
                                item = meta,
                                widthDp = cardWidth.value,
                                onClick = { onSelectMedia(meta) }
                            )
                        }
                    }
                }
            }
        }
        }

        // Fixed Top Status Bar Gradient Scrim so scrolled titles never clash with system icons
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(64.dp)
                .zIndex(30f)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to scrimBaseColor,
                            0.55f to scrimBaseColor.copy(alpha = 0.85f),
                            1.0f to Color.Transparent
                        )
                    )
                )
                .testTag("discover_top_scrim")
        )
    }

    if (showCustomizeSheet) {
        CustomizeCatalogsSheet(
            sections = discoverState.sections,
            hiddenKeys = discoverState.hiddenSectionKeys,
            onToggleHidden = onToggleCatalogVisibility,
            onMoveOrder = onMoveCatalogOrder,
            onResetHidden = onResetHiddenCatalogs,
            onDismiss = { showCustomizeSheet = false }
        )
    }

    if (selectedCwItemForOptions != null) {
        ContinueWatchingOptionsDialog(
            item = selectedCwItemForOptions!!,
            onDismiss = { selectedCwItemForOptions = null },
            onGoToDetails = { item ->
                onSelectMedia(
                    StremioMetaPreview(
                        id = item.mediaKey,
                        type = if (item.mediaType.contains("series", ignoreCase = true)) "series" else "movie",
                        name = item.title,
                        poster = item.posterUrl.ifBlank { null },
                        background = item.backdropUrl.ifBlank { null },
                        description = item.overview.ifBlank { null },
                        releaseInfo = item.year.ifBlank { null },
                        imdbRating = item.imdbRating.ifBlank { null }
                    )
                )
            },
            onStartFromBeginning = onRestartItem,
            onMarkAsWatched = onMarkAsWatched,
            onRemove = onRemoveContinueWatching
        )
    }

    val activeBrowseSection = activeBrowseSectionKey?.let { key ->
        discoverState.sections.firstOrNull { it.sectionKey == key }
    }
    if (activeBrowseSection != null) {
        CatalogFullBrowserScreen(
            section = activeBrowseSection,
            compactGrid = compactGrid,
            posterLayout = posterLayout,
            onSelectMedia = { meta ->
                activeBrowseSectionKey = null
                onSelectMedia(meta)
            },
            onSelectGenre = { genre ->
                onSelectCatalogGenre(activeBrowseSection.sectionKey, genre)
            },
            onLoadMore = {
                onLoadMoreCatalog(activeBrowseSection.sectionKey)
            },
            onDismiss = { activeBrowseSectionKey = null }
        )
    }

    if (searchExpanded) {
        GlobalSearchOverlay(
            discoverState = discoverState,
            compactGrid = compactGrid,
            posterLayout = posterLayout,
            onSearchChange = onSearchChange,
            onSelectMedia = { meta ->
                searchExpanded = false
                onSelectMedia(meta)
            },
            onDismiss = {
                searchExpanded = false
                onSearchChange("")
            }
        )
    }
}

@Composable
private fun FeaturedHeroCarousel(
    items: List<StremioMetaPreview>,
    spotlightMetaMap: Map<String, StremioMetaDetail>,
    autoPlayTrailers: String,
    isHeroInView: Boolean,
    onResolveSpotlightMeta: (StremioMetaPreview) -> Unit,
    onWatchClick: (StremioMetaPreview) -> Unit,
    onSaveClick: (StremioMetaPreview) -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { items.size })
    val activeItem = items[pagerState.currentPage]
    val activeDetail = spotlightMetaMap["${activeItem.type}:${activeItem.id}"]
    val activeTrailers = remember(activeItem, activeDetail) {
        activeDetail?.collectTrailers(activeItem) ?: activeItem.collectTrailers()
    }
    val primaryTrailer = activeTrailers.firstOrNull()
    val allowAutoPlay = remember(autoPlayTrailers) {
        isTrailerAutoPlayAllowed(context, autoPlayTrailers)
    }

    var isTrailerPlaying by remember(activeItem.id) { mutableStateOf(false) }
    var isVideoReady by remember(activeItem.id) { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }

    LaunchedEffect(activeItem.id) {
        onResolveSpotlightMeta(activeItem)
    }

    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(items.size, isDragged, isTrailerPlaying) {
        if (items.size > 1 && !isDragged && !isTrailerPlaying) {
            while (true) {
                delay(6000L)
                val nextPage = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    LaunchedEffect(activeItem.id, primaryTrailer?.id, isHeroInView, allowAutoPlay) {
        isTrailerPlaying = false
        isVideoReady = false
        if (allowAutoPlay && isHeroInView && primaryTrailer != null) {
            delay(2_000L)
            isTrailerPlaying = true
        }
    }

    val bgColor = MaterialTheme.colorScheme.background
    val accentGlow = MaterialTheme.colorScheme.primary
    val configuration = LocalConfiguration.current
    val heroHeight = (configuration.screenHeightDp * 0.74f).dp.coerceIn(540.dp, 680.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accentGlow.copy(alpha = 0.24f),
                        accentGlow.copy(alpha = 0.08f),
                        bgColor
                    ),
                    radius = 900f
                )
            )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(0.dp),
                    pageSpacing = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("featured_hero_banner")
                ) { page ->
                    val item = items[page]
                    val meta = spotlightMetaMap["${item.type}:${item.id}"]
                    val clearLogoUrl = meta?.logo?.takeIf { it.isNotBlank() }
                        ?: item.logo?.takeIf { it.isNotBlank() }
                    val isCurrentPage = page == pagerState.currentPage
                    val showInlineVideo = isCurrentPage && isTrailerPlaying && primaryTrailer != null

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroHeight)
                            .clickable { onWatchClick(item) }
                    ) {
                        AsyncImage(
                            model = item.background ?: item.poster,
                            contentDescription = item.name,
                            placeholder = painterResource(id = R.drawable.img_hero_cinema),
                            error = painterResource(id = R.drawable.img_hero_cinema),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (showInlineVideo) {
                            val trailerAlpha by animateFloatAsState(
                                targetValue = if (isVideoReady) 1f else 0f,
                                animationSpec = tween(durationMillis = 550),
                                label = "spotlight_trailer_alpha"
                            )
                            InlineSpotlightTrailerPlayer(
                                trailer = primaryTrailer!!,
                                isMuted = isMuted,
                                isPaused = !isHeroInView,
                                onVideoReady = { isVideoReady = true },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = trailerAlpha }
                            )
                        }

                        // Multi-stop bottom gradient fading into true background (#000000 in AMOLED mode)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black.copy(alpha = 0.65f),
                                            0.18f to Color.Transparent,
                                            0.52f to Color.Transparent,
                                            0.72f to bgColor.copy(alpha = 0.50f),
                                            0.88f to bgColor.copy(alpha = 0.90f),
                                            1.0f to bgColor
                                        )
                                    )
                                )
                        )

                        // Ambient side/radial accent wash
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            accentGlow.copy(alpha = 0.16f),
                                            Color.Transparent
                                        ),
                                        radius = 750f
                                    )
                                )
                        )

                        // Mute / Unmute Floating Frosted-Glass Button when trailer preview is active
                        if (isCurrentPage && isTrailerPlaying && primaryTrailer != null) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = 18.dp)
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                                    .clip(CircleShape)
                                    .clickable { isMuted = !isMuted }
                                    .testTag("spotlight_mute_button")
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute trailer" else "Mute trailer",
                                    tint = Color.White,
                                    modifier = Modifier.padding(10.dp).size(20.dp)
                                )
                            }
                        }

                        // Bottom Spotlight Meta + ClearLogo + CTA
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var logoError by remember(clearLogoUrl) { mutableStateOf(false) }
                            if (!clearLogoUrl.isNullOrBlank() && !logoError) {
                                AsyncImage(
                                    model = clearLogoUrl,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.Center,
                                    onError = { logoError = true },
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .heightIn(max = 76.dp)
                                        .testTag("spotlight_clear_logo")
                                )
                            } else {
                                Text(
                                    text = item.name.uppercase(),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 30.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            val genresText = remember(item, meta) {
                                val g = meta?.genres?.takeIf { it.isNotEmpty() } ?: item.genres.orEmpty()
                                val typeLabel = if (item.type.equals("series", ignoreCase = true)) "Series" else "Movie"
                                if (g.isNotEmpty()) "$typeLabel • ${g.take(3).joinToString(" • ")}" else typeLabel
                            }
                            Text(
                                text = genresText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    letterSpacing = 0.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Button(
                                onClick = { onWatchClick(item) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 38.dp, vertical = 12.dp),
                                modifier = Modifier.testTag("hero_stream_button")
                            ) {
                                Text(
                                    text = "More details",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black
                                )
                            }

                            if (items.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(items.size) { index ->
                                        val active = pagerState.currentPage == index
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .size(width = if (active) 24.dp else 5.dp, height = 5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (active) Color.White
                                                    else Color.White.copy(alpha = 0.38f)
                                                )
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueWatchingCard(
    item: LibraryMediaItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val fraction = if (item.durationMs > 0L) {
        (item.watchProgressMs.toFloat() / item.durationMs.toFloat()).coerceIn(0.04f, 1f)
    } else 0.39f

    val percent = (fraction * 100).roundToInt()
    val cardShape = RoundedCornerShape(16.dp)
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .width(236.dp)
            .clip(cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .machinedCardBorder(shape = cardShape, cornerRadiusDp = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            AsyncImage(
                model = item.backdropUrl.ifBlank { item.posterUrl },
                contentDescription = item.title,
                placeholder = painterResource(id = R.drawable.img_hero_cinema),
                error = painterResource(id = R.drawable.img_hero_cinema),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Dark bottom gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
            )

            // Center subtle play icon
            Surface(
                color = Color.Black.copy(alpha = 0.50f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.Center)
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Resume ${item.title}",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp).size(22.dp)
                )
            }

            // Title + Resume info + progress inside bottom of card
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Resume • ${item.year.ifBlank { "2025" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                val remainingLabel = remember(item.watchProgressMs, item.durationMs) {
                    if (item.durationMs > item.watchProgressMs && item.durationMs > 0L) {
                        val remainingSec = (item.durationMs - item.watchProgressMs) / 1000L
                        val hours = remainingSec / 3600L
                        val mins = (remainingSec % 3600L) / 60L
                        if (hours > 0) "${hours}h ${mins}m left" else "${mins}m left"
                    } else "$percent% watched"
                }
                Text(
                    text = remainingLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingOptionsDialog(
    item: LibraryMediaItem,
    onDismiss: () -> Unit,
    onGoToDetails: (LibraryMediaItem) -> Unit,
    onStartFromBeginning: (LibraryMediaItem) -> Unit,
    onMarkAsWatched: (LibraryMediaItem) -> Unit,
    onRemove: (LibraryMediaItem) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable(enabled = false) {}, // prevent click-through
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Floating Preview Card matching Image 1
                val cardShape = RoundedCornerShape(18.dp)
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = AmoledCard),
                    modifier = Modifier
                        .width(280.dp)
                        .aspectRatio(16f / 9.5f)
                        .shadow(elevation = 20.dp, shape = cardShape, spotColor = Color.Black)
                        .machinedCardBorder(shape = cardShape, cornerRadiusDp = 18.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = item.backdropUrl.ifBlank { item.posterUrl },
                            contentDescription = item.title,
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
                                        colorStops = arrayOf(
                                            0.0f to Color.Transparent,
                                            0.40f to Color.Black.copy(alpha = 0.25f),
                                            1.0f to Color.Black.copy(alpha = 0.90f)
                                        )
                                    )
                                )
                        )

                        // Top-Right Remaining Time Badge (e.g. "2h 17m left")
                        val remainingLabel = remember(item.watchProgressMs, item.durationMs) {
                            if (item.durationMs > item.watchProgressMs && item.durationMs > 0L) {
                                val remainingSec = (item.durationMs - item.watchProgressMs) / 1000L
                                val hours = remainingSec / 3600L
                                val mins = (remainingSec % 3600L) / 60L
                                if (hours > 0) "${hours}h ${mins}m left" else "${mins}m left"
                            } else {
                                val pct = if (item.durationMs > 0L) {
                                    ((item.watchProgressMs * 100) / item.durationMs).toInt().coerceIn(0, 100)
                                } else 0
                                "$pct% watched"
                            }
                        }
                        Surface(
                            color = Color.Black.copy(alpha = 0.70f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = remainingLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Bottom-Left Title & Progress Track
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val fraction = if (item.durationMs > 0L) {
                                (item.watchProgressMs.toFloat() / item.durationMs.toFloat()).coerceIn(0.04f, 1f)
                            } else 0.35f
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(3.5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.30f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .height(3.5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title & Subtitle Below Card
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val typeLabel = if (item.mediaType.contains("series", ignoreCase = true)) "Series" else "Movie"
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Rounded Options Card matching Image 1
                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.width(310.dp)
                ) {
                    Column {
                        // 1. Go to details
                        OptionItemRow(
                            label = "Go to details",
                            icon = Icons.Rounded.Info,
                            onClick = {
                                onDismiss()
                                onGoToDetails(item)
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)

                        // 2. Start from beginning
                        OptionItemRow(
                            label = "Start from beginning",
                            icon = Icons.Rounded.RestartAlt,
                            onClick = {
                                onDismiss()
                                onStartFromBeginning(item)
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)

                        // 3. Mark as watched
                        OptionItemRow(
                            label = "Mark as watched",
                            icon = Icons.Rounded.CheckCircle,
                            onClick = {
                                onDismiss()
                                onMarkAsWatched(item)
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)

                        // 4. Remove
                        OptionItemRow(
                            label = "Remove",
                            icon = Icons.Rounded.DeleteOutline,
                            tint = Color(0xFFFF5252),
                            onClick = {
                                onDismiss()
                                onRemove(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionItemRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = tint
        )
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint.copy(alpha = if (tint == Color.White) 0.80f else 1.0f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun RankedPosterMediaCard(
    rank: Int,
    item: StremioMetaPreview,
    widthDp: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .clickable(onClick = onClick)
            .testTag("trending_rank_$rank")
    ) {
        PosterMediaCard(
            item = item,
            widthDp = widthDp,
            onClick = onClick
        )
    }
}

@Composable
private fun PosterMediaCard(
    item: StremioMetaPreview,
    widthDp: Float,
    onClick: () -> Unit
) {
    val isAmoled = LocalIsAmoledBlack.current
    val posterShape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .width(widthDp.dp)
            .clip(posterShape)
            .clickable(onClick = onClick)
            .testTag("media_card_${item.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(posterShape)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (isAmoled) 0.10f else 0.08f),
                    shape = posterShape
                )
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            if (!item.poster.isNullOrBlank()) {
                AsyncImage(
                    model = item.poster,
                    contentDescription = item.name,
                    error = painterResource(id = R.drawable.img_hero_cinema),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.LocalMovies,
                        contentDescription = item.name,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // Top-Right Single Yellow IMDb Rating Badge
            if (!item.imdbRating.isNullOrBlank()) {
                val formattedRating = remember(item.imdbRating) {
                    item.imdbRating.toDoubleOrNull()?.let {
                        String.format(java.util.Locale.US, "%.1f", it)
                    } ?: item.imdbRating
                }
                Surface(
                    color = Color(0xFF14171C).copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = Color(0xFFF5C518),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = "IMDb",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                ),
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = formattedRating,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!item.releaseInfo.isNullOrBlank()) {
            Text(
                text = item.releaseInfo,
                style = MaterialTheme.typography.labelMedium.copy(
                    lineHeight = 18.sp,
                    fontSize = 13.sp
                ),
                color = Color(0xFFA0AAB8),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizeCatalogsSheet(
    sections: List<CatalogSection>,
    hiddenKeys: Set<String>,
    onToggleHidden: (String) -> Unit,
    onMoveOrder: (String, Int) -> Unit,
    onResetHidden: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Customize Home Catalogs", style = MaterialTheme.typography.titleLarge)
                }
                if (hiddenKeys.isNotEmpty()) {
                    TextButton(onClick = onResetHidden) {
                        Text("Show All")
                    }
                }
            }
            Text(
                text = "Reorder or toggle any addon catalog row on your home feed (${sections.size} total).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(
                modifier = Modifier.height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sections, key = { "cust_${it.sectionKey}" }) { sec ->
                    val visible = !hiddenKeys.contains(sec.sectionKey)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sec.catalogTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${sec.addonName} • ${sec.items.size} items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onMoveOrder(sec.sectionKey, -1) }) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
                            }
                            IconButton(onClick = { onMoveOrder(sec.sectionKey, 1) }) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
                            }
                            Switch(
                                checked = visible,
                                onCheckedChange = { onToggleHidden(sec.sectionKey) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CatalogFullBrowserScreen(
    section: CatalogSection,
    compactGrid: Boolean,
    posterLayout: String = "STANDARD_2",
    onSelectMedia: (StremioMetaPreview) -> Unit,
    onSelectGenre: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    var sortOption by remember { mutableStateOf("Catalog Order") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val typeLabel = if (section.type.equals("series", ignoreCase = true)) "Series" else "Movies"
    val displayTitle = remember(section.catalogTitle, typeLabel) {
        if (section.catalogTitle.contains("•") || section.catalogTitle.contains("(")) {
            section.catalogTitle
        } else {
            "${section.catalogTitle} • $typeLabel"
        }
    }

    val sortedItems = remember(section.items, sortOption) {
        when (sortOption) {
            "Highest IMDb" -> section.items.sortedByDescending { it.imdbRating?.toDoubleOrNull() ?: 0.0 }
            "Newest First" -> section.items.sortedByDescending { it.releaseInfo?.filter { c -> c.isDigit() }?.take(4)?.toIntOrNull() ?: 0 }
            else -> section.items
        }
    }

    val showBackToTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > 4 }
    }

    LaunchedEffect(gridState, sortedItems.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= sortedItems.size - 4 && section.canLoadMore && !section.isLoadingMore) {
                    onLoadMore()
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
            .zIndex(90f)
            .testTag("catalog_full_browser")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Sticky Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${section.addonName} • ${section.items.size} titles",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }

                // Sort Dropdown Button
                Box {
                    Surface(
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { sortMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
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
                                text = sortOption,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        listOf("Catalog Order", "Highest IMDb", "Newest First").forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    sortOption = opt
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Genre strip
            if (section.availableGenres.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = section.selectedGenre == null,
                            onClick = { onSelectGenre(null) },
                            label = { Text("All") }
                        )
                    }
                    items(section.availableGenres, key = { "browser_g_$it" }) { genre ->
                        FilterChip(
                            selected = section.selectedGenre.equals(genre, ignoreCase = true),
                            onClick = { onSelectGenre(genre) },
                            label = { Text(genre) }
                        )
                    }
                }
            }

            // Grid content
            val parsedLayout = remember(posterLayout, compactGrid) {
                if (posterLayout.isNotBlank()) com.example.data.model.PosterLayout.fromKey(posterLayout)
                else if (compactGrid) com.example.data.model.PosterLayout.COMPACT_3
                else com.example.data.model.PosterLayout.STANDARD_2
            }
            val columns = when (parsedLayout) {
                com.example.data.model.PosterLayout.BENTO_4 -> 4
                com.example.data.model.PosterLayout.COMPACT_3 -> 3
                com.example.data.model.PosterLayout.STANDARD_2 -> 2
                com.example.data.model.PosterLayout.CINEMATIC_1 -> 1
                com.example.data.model.PosterLayout.LIST_DETAILED -> 1
            }
            val cardWidth = when (parsedLayout) {
                com.example.data.model.PosterLayout.BENTO_4 -> 85f
                com.example.data.model.PosterLayout.COMPACT_3 -> 114f
                com.example.data.model.PosterLayout.STANDARD_2 -> 165f
                com.example.data.model.PosterLayout.CINEMATIC_1 -> 320f
                com.example.data.model.PosterLayout.LIST_DETAILED -> 320f
            }
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
            ) {
                items(sortedItems, key = { "browser_${section.sectionKey}_${it.id}" }) { meta ->
                    PosterMediaCard(
                        item = meta,
                        widthDp = cardWidth,
                        onClick = { onSelectMedia(meta) }
                    )
                }

                if (section.canLoadMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (section.isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                OutlinedButton(onClick = onLoadMore) {
                                    Text("Load More")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Back-to-Top Button
        AnimatedVisibility(
            visible = showBackToTop,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 20.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable {
                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = "Back to top",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GlobalSearchOverlay(
    discoverState: DiscoverUiState,
    compactGrid: Boolean,
    posterLayout: String = "STANDARD_2",
    onSearchChange: (String) -> Unit,
    onSelectMedia: (StremioMetaPreview) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var selectedTab by remember { mutableStateOf("all") }
    var recentSearches by remember { mutableStateOf(getRecentSearches(context)) }

    val popularSearches = remember {
        listOf("Interstellar", "Severance", "The Batman", "Dune", "Breaking Bad", "Arcane", "Shogun", "Inception")
    }

    LaunchedEffect(Unit) {
        delay(120)
        focusRequester.requestFocus()
    }

    val allResults = remember(discoverState.sections) {
        discoverState.sections.flatMap { it.items }.distinctBy { it.id }
    }
    val movieResults = remember(discoverState.sections) {
        discoverState.sections.filter { it.type.equals("movie", ignoreCase = true) }
            .flatMap { it.items }.distinctBy { it.id }
    }
    val seriesResults = remember(discoverState.sections) {
        discoverState.sections.filter { it.type.equals("series", ignoreCase = true) }
            .flatMap { it.items }.distinctBy { it.id }
    }

    val displayedSections = remember(discoverState.sections, selectedTab) {
        if (selectedTab == "all") {
            discoverState.sections.filter { it.items.isNotEmpty() }
        } else {
            discoverState.sections.filter { it.type.equals(selectedTab, ignoreCase = true) && it.items.isNotEmpty() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
            .zIndex(100f)
            .testTag("global_search_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Top Bar: Back, Input, Clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = discoverState.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = "Search movies, series, anime...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0D0D0D),
                        unfocusedContainerColor = Color(0xFF0D0D0D),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        if (discoverState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )
            }

            // Sticky Filter Tabs with Live Counts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == "all",
                    onClick = { selectedTab = "all" },
                    label = { Text("All (${allResults.size})") }
                )
                FilterChip(
                    selected = selectedTab == "movie",
                    onClick = { selectedTab = "movie" },
                    label = { Text("Movies (${movieResults.size})") }
                )
                FilterChip(
                    selected = selectedTab == "series",
                    onClick = { selectedTab = "series" },
                    label = { Text("Series (${seriesResults.size})") }
                )
            }

            // Body: Empty State (Recent / Popular) OR Results Grid
            if (discoverState.searchQuery.isBlank()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent Searches",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = Color.White
                                    )
                                    TextButton(onClick = {
                                        clearRecentSearches(context)
                                        recentSearches = emptyList()
                                    }) {
                                        Text("Clear All", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                    }
                                }

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(recentSearches, key = { "recent_$it" }) { query ->
                                        Surface(
                                            color = Color(0xFF161616),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                            modifier = Modifier.clickable { onSearchChange(query) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = query,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White
                                                )
                                                IconButton(
                                                    onClick = {
                                                        removeRecentSearch(context, query)
                                                        recentSearches = getRecentSearches(context)
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Clear,
                                                        contentDescription = "Remove",
                                                        tint = Color.White.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Popular Searches",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = Color.White
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(popularSearches, key = { "pop_$it" }) { term ->
                                    Surface(
                                        color = Color(0xFF161616),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                        modifier = Modifier.clickable {
                                            saveRecentSearch(context, term)
                                            recentSearches = getRecentSearches(context)
                                            onSearchChange(term)
                                        }
                                    ) {
                                        Text(
                                            text = term,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (discoverState.isLoading) {
                // #080808 Shimmer Skeletons while loading
                val parsedLayout = remember(posterLayout, compactGrid) {
                    if (posterLayout.isNotBlank()) com.example.data.model.PosterLayout.fromKey(posterLayout)
                    else if (compactGrid) com.example.data.model.PosterLayout.COMPACT_3
                    else com.example.data.model.PosterLayout.STANDARD_2
                }
                val columns = when (parsedLayout) {
                    com.example.data.model.PosterLayout.BENTO_4 -> 4
                    com.example.data.model.PosterLayout.COMPACT_3 -> 3
                    com.example.data.model.PosterLayout.STANDARD_2 -> 2
                    com.example.data.model.PosterLayout.CINEMATIC_1 -> 1
                    com.example.data.model.PosterLayout.LIST_DETAILED -> 1
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(6) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF080808))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        )
                    }
                }
            } else if (displayedSections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results found for \"${discoverState.searchQuery}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Progressive Results grouped under Addon • Type
                val parsedLayout = remember(posterLayout, compactGrid) {
                    if (posterLayout.isNotBlank()) com.example.data.model.PosterLayout.fromKey(posterLayout)
                    else if (compactGrid) com.example.data.model.PosterLayout.COMPACT_3
                    else com.example.data.model.PosterLayout.STANDARD_2
                }
                val cardWidth = when (parsedLayout) {
                    com.example.data.model.PosterLayout.BENTO_4 -> 105.dp
                    com.example.data.model.PosterLayout.COMPACT_3 -> 118.dp
                    com.example.data.model.PosterLayout.STANDARD_2 -> 142.dp
                    com.example.data.model.PosterLayout.CINEMATIC_1 -> 180.dp
                    com.example.data.model.PosterLayout.LIST_DETAILED -> 150.dp
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(displayedSections, key = { "search_sec_${it.sectionKey}" }) { sec ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val secTypeLabel = if (sec.type.equals("series", ignoreCase = true)) "Series" else "Movies"
                            Text(
                                text = "${sec.addonName} • $secTypeLabel (${sec.items.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color.White
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(sec.items, key = { "res_${sec.sectionKey}_${it.id}" }) { meta ->
                                    PosterMediaCard(
                                        item = meta,
                                        widthDp = cardWidth.value,
                                        onClick = {
                                            saveRecentSearch(context, discoverState.searchQuery.trim())
                                            recentSearches = getRecentSearches(context)
                                            onSelectMedia(meta)
                                        }
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

// ---- Search History Persistence Helpers ----
private fun getRecentSearches(context: Context): List<String> {
    val prefs = context.getSharedPreferences("streamflow_search_history", Context.MODE_PRIVATE)
    val raw = prefs.getString("recent_queries", null) ?: return emptyList()
    return raw.split("|||").filter { it.isNotBlank() }
}

private fun saveRecentSearch(context: Context, query: String) {
    if (query.isBlank()) return
    val prefs = context.getSharedPreferences("streamflow_search_history", Context.MODE_PRIVATE)
    val current = getRecentSearches(context).toMutableList()
    current.remove(query)
    current.add(0, query)
    val trimmed = current.take(10).joinToString("|||")
    prefs.edit().putString("recent_queries", trimmed).apply()
}

private fun removeRecentSearch(context: Context, query: String) {
    val prefs = context.getSharedPreferences("streamflow_search_history", Context.MODE_PRIVATE)
    val current = getRecentSearches(context).toMutableList()
    current.remove(query)
    val trimmed = current.joinToString("|||")
    prefs.edit().putString("recent_queries", trimmed).apply()
}

private fun clearRecentSearches(context: Context) {
    val prefs = context.getSharedPreferences("streamflow_search_history", Context.MODE_PRIVATE)
    prefs.edit().remove("recent_queries").apply()
}

