package com.example

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UiTextScale
import androidx.core.view.WindowCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.auth.GoogleAuthManager
import com.example.data.auth.authStateFlow
import com.example.data.local.DownloadStatus
import com.example.data.model.PlaybackEngine
import com.example.data.model.StremioMetaPreview
import com.example.data.model.StremioStream
import com.example.data.model.StremioSubtitle
import com.example.data.preferences.LocalPreferencesStore
import com.example.data.repository.CineVaultSyncPort
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.LocalRoomSyncRepository
import com.example.data.stremio.StremioUrlHelper
import com.example.player.LocalPipMode
import com.example.player.PlaybackRequest
import com.example.player.StreamSourceOption
import com.example.ui.screens.AddonsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MediaDetailScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.AmoledBg
import com.example.ui.theme.CineVaultTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.viewmodel.CineVaultViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object DiscoverRoute
@Serializable object LibraryRoute
@Serializable object AddonsRoute
@Serializable object SettingsRoute
@Serializable object DetailRoute
@Serializable data class PlayerRoute(
    val mediaKey: String,
    val title: String,
    val mediaType: String,
    val streamUrl: String,
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val imdbId: String = "",
    val initialPositionMs: Long = 0L,
    val preferredEngine: String = PlaybackEngine.NEXTPLAYER.key,
    val localSubtitleUri: String = "",
    val seriesId: String = "",
    val currentSeason: Int? = null,
    val currentEpisode: Int? = null
)

private data class TopNavItem<T : Any>(
    val route: T,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    private val isPipModeState = mutableStateOf(false)
    private var isPlayerActive = false

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPlayerActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPipModeState.value = isInPictureInPictureMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalPipMode provides isPipModeState.value) {
                CineVaultAppRoot(
                    onPlayerStateChanged = { active -> isPlayerActive = active }
                )
            }
        }
    }
}

@Composable
fun CineVaultAppRoot(
    auth: FirebaseAuth = Firebase.auth,
    onPlayerStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localPrefs = remember(context) { LocalPreferencesStore(context.applicationContext) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val guestSkipped by remember(localPrefs) {
        localPrefs.guestModeSkippedFlow.map<Boolean, Boolean?> { it }
    }.collectAsStateWithLifecycle(initialValue = null)

    val user = currentUser
    when {
        user != null -> {
            val uid = user.uid
            key(uid) {
                CineVaultShell(
                    currentUserId = uid,
                    userEmail = user.email,
                    isGuestMode = false,
                    syncRepoFactory = { appCtx ->
                        val databaseId = appCtx.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        FirestoreSyncRepository(db)
                    },
                    onClearGuestMode = {
                        scope.launch { localPrefs.setGuestModeSkipped(false) }
                    },
                    onPlayerStateChanged = onPlayerStateChanged
                )
            }
        }
        guestSkipped == true -> {
            key(LocalRoomSyncRepository.GUEST_USER_ID) {
                CineVaultShell(
                    currentUserId = LocalRoomSyncRepository.GUEST_USER_ID,
                    userEmail = null,
                    isGuestMode = true,
                    syncRepoFactory = { appCtx -> LocalRoomSyncRepository(appCtx) },
                    onClearGuestMode = {
                        scope.launch { localPrefs.setGuestModeSkipped(false) }
                    },
                    onPlayerStateChanged = onPlayerStateChanged
                )
            }
        }
        guestSkipped == false -> {
            CineVaultTheme {
                AuthScreen(
                    onAuthSuccess = {
                        scope.launch { localPrefs.setGuestModeSkipped(false) }
                    },
                    onSkipClick = {
                        scope.launch { localPrefs.setGuestModeSkipped(true) }
                    },
                    attemptSilentSignIn = true
                )
            }
        }
        else -> {
            CineVaultTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}

@Composable
private fun CineVaultShell(
    currentUserId: String,
    userEmail: String?,
    isGuestMode: Boolean,
    syncRepoFactory: (Context) -> CineVaultSyncPort,
    onClearGuestMode: () -> Unit,
    onPlayerStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: CineVaultViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                CineVaultViewModel(
                    appContext = app.applicationContext,
                    syncRepository = syncRepoFactory(app.applicationContext),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val preferences by viewModel.preferencesState.collectAsStateWithLifecycle()
    val discoverState by viewModel.discoverState.collectAsStateWithLifecycle()
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val spotlightMetaMap by viewModel.spotlightMetaMap.collectAsStateWithLifecycle()
    val addonsState by viewModel.addonsState.collectAsStateWithLifecycle()
    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val downloads by viewModel.downloadsState.collectAsStateWithLifecycle()
    val localMedia by viewModel.localMediaState.collectAsStateWithLifecycle()
    val addonCatalogNames by viewModel.addonCatalogNames.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val lastStreamPreference by viewModel.lastStreamPreference.collectAsStateWithLifecycle()

    val accentPreset = AccentPreset.fromKey(preferences.accentPreset)
    val preferredEngine = PlaybackEngine.fromKey(preferences.preferredEngine)
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val barColorArgb = if (preferences.amoledBlack) AmoledBg.toArgb() else ObsidianBg.toArgb()
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            @Suppress("DEPRECATION")
            activity.window.statusBarColor = if (preferences.amoledBlack) barColorArgb else android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            activity.window.navigationBarColor = barColorArgb
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearStatusBanner()
    }

    val hasActiveDownloads = remember(downloads) {
        downloads.any { it.status == DownloadStatus.DOWNLOADING.name || it.status == DownloadStatus.QUEUED.name }
    }

    val navItems = remember {
        listOf(
            TopNavItem(DiscoverRoute, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home, "nav_discover"),
            TopNavItem(LibraryRoute, R.string.nav_library, Icons.Rounded.CollectionsBookmark, Icons.Outlined.CollectionsBookmark, "nav_library"),
            TopNavItem(AddonsRoute, R.string.nav_addons, Icons.Rounded.Extension, Icons.Outlined.Extension, "nav_addons"),
            TopNavItem(SettingsRoute, R.string.nav_settings, Icons.Rounded.Tune, Icons.Outlined.Tune, "nav_settings")
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination
    val isPlayerActive = currentDest?.hasRoute<PlayerRoute>() == true
    val isFullScreenRoute = isPlayerActive || currentDest?.hasRoute<DetailRoute>() == true
    val isPipMode = LocalPipMode.current
    var showTitleDetailModal by remember { mutableStateOf(false) }

    LaunchedEffect(isPlayerActive) {
        onPlayerStateChanged(isPlayerActive)
    }

    androidx.activity.compose.BackHandler(enabled = showTitleDetailModal) {
        showTitleDetailModal = false
    }
    androidx.activity.compose.BackHandler(enabled = !showTitleDetailModal && !isPlayerActive && currentDest?.hasRoute<DiscoverRoute>() != true) {
        navController.navigate(DiscoverRoute) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val activeDetailLibItems = (libraryState as? UiState.Success)?.data.orEmpty()
    val activeDetailSavedItem = detailState.preview?.let { p ->
        activeDetailLibItems.firstOrNull { it.mediaKey == StremioUrlHelper.sanitizeDocId(p.id) }
    }

    val launchStreamPlayer: (StremioStream, PlaybackEngine) -> Unit = { stream, engine ->
        val p = detailState.preview
        val url = stream.url
        if (p != null && !url.isNullOrBlank()) {
            val activeId = detailState.selectedEpisodeId ?: p.id
            val selectedEp = detailState.detail?.videos?.firstOrNull { it.id == activeId }
            viewModel.saveStreamPreferenceForStream(stream)
            navController.navigate(
                PlayerRoute(
                    mediaKey = activeId,
                    title = detailState.detail?.name ?: p.name,
                    mediaType = p.type,
                    streamUrl = url,
                    posterUrl = p.poster.orEmpty(),
                    backdropUrl = p.background.orEmpty(),
                    imdbId = activeId,
                    initialPositionMs = activeDetailSavedItem?.watchProgressMs ?: 0L,
                    preferredEngine = engine.key,
                    seriesId = if (p.type.equals("series", ignoreCase = true)) p.id else "",
                    currentSeason = selectedEp?.season,
                    currentEpisode = selectedEp?.episode
                )
            )
        }
    }

    val enqueueStreamDownload: (StremioStream) -> Unit = { stream ->
        val p = detailState.preview
        val url = stream.url
        if (p != null && !url.isNullOrBlank()) {
            val bestSub = detailState.subtitles
                .firstOrNull { it.lang.equals(preferences.preferredSubtitleLang, ignoreCase = true) }
                ?: detailState.subtitles.firstOrNull()
            viewModel.startOfflineDownload(
                mediaKey = detailState.selectedEpisodeId ?: p.id,
                title = detailState.detail?.name ?: p.name,
                mediaType = p.type,
                posterUrl = p.poster.orEmpty(),
                streamUrl = url,
                subtitleUrl = bestSub?.url.orEmpty(),
                qualityLabel = stream.fullFileName(),
                initialTotalBytes = stream.parseSizeBytes()
            )
        }
    }

    val fontScaleMultiplier = UiTextScale.fromKey(preferences.uiTextScale).scale
    val currentDensity = LocalDensity.current
    val scaledDensity = remember(currentDensity.density, currentDensity.fontScale, fontScaleMultiplier) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * fontScaleMultiplier
        )
    }

    CineVaultTheme(
        accentPreset = accentPreset,
        amoledBlack = preferences.amoledBlack
    ) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 640.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = if (!isFullScreenRoute && !useRail) {
                            Modifier.padding(bottom = 86.dp)
                        } else {
                            Modifier
                        }
                    )
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (!isFullScreenRoute && useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                        ) {
                            navItems.forEach { item ->
                                val selected = currentDest?.hasRoute(item.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val contentModifier = if (isFullScreenRoute) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier.widthIn(max = 980.dp).fillMaxSize()
                        }

                        NavHost(
                            navController = navController,
                            startDestination = DiscoverRoute,
                            modifier = contentModifier
                        ) {
                            composable<DiscoverRoute> {
                                val libraryItems = (libraryState as? UiState.Success)?.data.orEmpty()
                                val continueWatching = remember(libraryItems) {
                                    libraryItems.filter { it.watchProgressMs > 0L }
                                        .sortedByDescending { it.lastWatchedAtMs }
                                        .take(15)
                                }
                                DiscoverScreen(
                                    discoverState = discoverState,
                                    continueWatching = continueWatching,
                                    compactGrid = preferences.compactGrid,
                                    posterLayout = preferences.posterLayout,
                                    preferredEngine = preferredEngine,
                                    autoPlayTrailers = preferences.autoPlayTrailers,
                                    spotlightMetaMap = spotlightMetaMap,
                                    onResolveSpotlightMeta = viewModel::resolveSpotlightMeta,
                                    onSearchChange = viewModel::updateSearchQuery,
                                    onTypeSelected = viewModel::selectMediaTypeFilter,
                                    onRefresh = viewModel::refreshDiscover,
                                    onSelectMedia = { preview ->
                                        viewModel.loadMediaDetail(preview)
                                        showTitleDetailModal = true
                                    },
                                    onResumeItem = { libItem ->
                                        if (libItem.streamUrl.isNotBlank()) {
                                            navController.navigate(
                                                PlayerRoute(
                                                    mediaKey = libItem.mediaKey,
                                                    title = libItem.title,
                                                    mediaType = libItem.mediaType,
                                                    streamUrl = libItem.streamUrl,
                                                    posterUrl = libItem.posterUrl,
                                                    backdropUrl = libItem.backdropUrl,
                                                    imdbId = libItem.mediaKey,
                                                    initialPositionMs = libItem.watchProgressMs,
                                                    preferredEngine = preferredEngine.key
                                                )
                                            )
                                        } else {
                                            viewModel.loadMediaDetail(
                                                StremioMetaPreview(
                                                    id = libItem.mediaKey,
                                                    type = if (libItem.mediaType == "series") "series" else "movie",
                                                    name = libItem.title,
                                                    poster = libItem.posterUrl.ifBlank { null },
                                                    background = libItem.backdropUrl.ifBlank { null },
                                                    description = libItem.overview.ifBlank { null },
                                                    releaseInfo = libItem.year.ifBlank { null },
                                                    imdbRating = libItem.imdbRating.ifBlank { null }
                                                )
                                            )
                                            showTitleDetailModal = true
                                        }
                                    },
                                    onQuickFavorite = viewModel::toggleFavorite,
                                    onSelectCatalogGenre = viewModel::selectCatalogGenre,
                                    onLoadMoreCatalog = viewModel::loadMoreCatalogItems,
                                    onToggleCatalogVisibility = viewModel::toggleCatalogVisibility,
                                    onMoveCatalogOrder = viewModel::moveCatalogOrder,
                                    onResetHiddenCatalogs = viewModel::resetHiddenCatalogs
                                )
                            }

                            composable<LibraryRoute> {
                                LibraryScreen(
                                    libraryState = libraryState,
                                    downloads = downloads,
                                    localMedia = localMedia,
                                    preferredEngine = preferredEngine,
                                    posterLayout = preferences.posterLayout,
                                    onAddPersonalCloudMedia = viewModel::addPersonalCloudMedia,
                                    onRemoveLibraryItem = viewModel::removeLibraryItem,
                                    onImportLocalVideo = viewModel::importLocalVideo,
                                    onAttachLocalSubtitle = viewModel::attachSubtitleToLocalVideo,
                                    onRefreshLocalMetadata = viewModel::refreshLocalMetadata,
                                    onDeleteLocalMedia = viewModel::deleteLocalMedia,
                                    onDeleteDownload = viewModel::deleteDownload,
                                    onPlayRequest = { req ->
                                        navController.navigate(
                                            PlayerRoute(
                                                mediaKey = req.mediaKey,
                                                title = req.title,
                                                mediaType = req.mediaType,
                                                streamUrl = req.streamUrl,
                                                posterUrl = req.posterUrl,
                                                backdropUrl = req.backdropUrl,
                                                imdbId = req.imdbId,
                                                initialPositionMs = req.initialPositionMs,
                                                preferredEngine = req.preferredEngine,
                                                localSubtitleUri = req.localSubtitleUri
                                            )
                                        )
                                    },
                                    onOpenPreview = { preview ->
                                        viewModel.loadMediaDetail(preview)
                                        showTitleDetailModal = true
                                    }
                                )
                            }

                            composable<AddonsRoute> {
                                AddonsScreen(
                                    addonsState = addonsState,
                                    addonCatalogNames = addonCatalogNames,
                                    hiddenCatalogKeys = discoverState.hiddenSectionKeys,
                                    onInstallAddon = viewModel::installAddon,
                                    onToggleAddon = viewModel::toggleAddon,
                                    onRemoveAddon = viewModel::removeAddon,
                                    onToggleCatalogHidden = viewModel::toggleCatalogVisibility
                                )
                            }

                            composable<SettingsRoute> {
                                SettingsScreen(
                                    preferences = preferences,
                                    userEmail = userEmail,
                                    isGuestMode = isGuestMode,
                                    onUpdatePreferences = viewModel::updatePreferences,
                                    onSignInClick = {
                                        GoogleAuthManager.launchInteractiveGoogleSignIn(
                                            context = context,
                                            credentialManager = credentialManager,
                                            scope = scope,
                                            onAuthSuccess = {
                                                onClearGuestMode()
                                            },
                                            onAuthError = { msg ->
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            },
                                            onAuthCancelled = {}
                                        )
                                    },
                                    onSignOut = {
                                        GoogleAuthManager.signOut(credentialManager, scope) {
                                            onClearGuestMode()
                                        }
                                    }
                                )
                            }

                            composable<DetailRoute> {
                                MediaDetailScreen(
                                    detailState = detailState,
                                    isFavorited = activeDetailSavedItem?.isFavorite == true,
                                    savedPositionMs = activeDetailSavedItem?.watchProgressMs ?: 0L,
                                    preferredEngine = preferredEngine,
                                    onBack = { navController.popBackStack() },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onSelectEpisode = viewModel::selectEpisode,
                                    onPlayStream = launchStreamPlayer,
                                    onDownloadStream = enqueueStreamDownload,
                                    downloads = downloads,
                                    onNavigateToOffline = {
                                        navController.navigate(LibraryRoute) {
                                            popUpTo<DiscoverRoute> { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onSelectRelatedMedia = { rel -> viewModel.loadMediaDetail(rel) }
                                )
                            }

                            composable<PlayerRoute> { backStackEntry ->
                                val route = backStackEntry.toRoute<PlayerRoute>()
                                val detailSources = remember(route.mediaKey, detailState.streams, detailState.streamAddonMap) {
                                    viewModel.buildDetailSourceOptions()
                                }
                                val seriesEpisodes = remember(route.seriesId, detailState.detail) {
                                    if (route.mediaType.equals("series", ignoreCase = true)) {
                                        viewModel.currentSeriesEpisodes()
                                    } else emptyList()
                                }
                                val initialUpNext = remember(route.mediaKey, detailState.selectedEpisodeId) {
                                    viewModel.computeUpNext(route.mediaKey, detailState.selectedEpisodeId)
                                }
                                val req = PlaybackRequest(
                                    mediaKey = route.mediaKey,
                                    title = route.title,
                                    mediaType = route.mediaType,
                                    streamUrl = route.streamUrl,
                                    posterUrl = route.posterUrl,
                                    backdropUrl = route.backdropUrl,
                                    imdbId = route.imdbId,
                                    initialPositionMs = route.initialPositionMs,
                                    preferredEngine = route.preferredEngine,
                                    localSubtitleUri = route.localSubtitleUri,
                                    availableSources = detailSources,
                                    upNext = initialUpNext,
                                    seriesId = route.seriesId,
                                    currentSeason = route.currentSeason,
                                    currentEpisode = route.currentEpisode,
                                    episodes = seriesEpisodes,
                                    streamPreference = lastStreamPreference
                                )
                                val initialSubs = remember(route.mediaKey) {
                                    if (detailState.preview?.id == route.mediaKey || detailState.selectedEpisodeId == route.mediaKey) {
                                        detailState.subtitles
                                    } else {
                                        emptyList()
                                    }
                                }
                                PlayerScreen(
                                    request = req,
                                    subtitles = initialSubs,
                                    preferences = preferences,
                                    onUpdateProgress = { mKey, mTitle, mType, mPoster, mUrl, posMs, durMs ->
                                        viewModel.saveWatchProgress(
                                            mediaKey = mKey,
                                            title = mTitle,
                                            mediaType = mType,
                                            posterUrl = mPoster,
                                            streamUrl = mUrl,
                                            positionMs = posMs,
                                            durationMs = durMs
                                        )
                                    },
                                    onUpdateSubtitleDelay = { newDelay ->
                                        viewModel.updatePreferences { it.copy(subtitleDelayMs = newDelay) }
                                    },
                                    onResolveSources = viewModel::fetchStreamsForPlayer,
                                    onResolveSubtitles = viewModel::fetchSubtitlesForMedia,
                                    onResolveNextQueueItem = { curKey ->
                                        viewModel.computeUpNext(curKey, curKey)
                                    },
                                    onSaveStreamPreference = viewModel::saveStreamPreference,
                                    onClose = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }

            // Fixed top status-bar gradient scrim (hidden in full-screen player)
            if (!isFullScreenRoute) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black,
                                    Color.Black.copy(alpha = 0.95f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Centered Floating Frosted-Glass Pill Dock & bottom edge scrim
            if (!isFullScreenRoute && !useRail && !isPipMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Black
                                )
                            )
                        )
                )

                FloatingBottomDock(
                    items = navItems,
                    isSelected = { item -> currentDest?.hasRoute(item.route::class) == true },
                    amoledBlack = preferences.amoledBlack,
                    hasActiveDownloads = hasActiveDownloads,
                    onNavigate = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            // Slide-Up Title Details Modal Sheet
            AnimatedVisibility(
                visible = showTitleDetailModal && !isPlayerActive && detailState.preview != null,
                enter = fadeIn(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(180))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                )
            }

            AnimatedVisibility(
                visible = showTitleDetailModal && !isPlayerActive && detailState.preview != null,
                enter = slideInVertically(
                    animationSpec = tween(280),
                    initialOffsetY = { it }
                ) + fadeIn(animationSpec = tween(220)),
                exit = slideOutVertically(
                    animationSpec = tween(240),
                    targetOffsetY = { it }
                ) + fadeOut(animationSpec = tween(180)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                        .padding(top = 12.dp)
                        .widthIn(max = 980.dp)
                        .testTag("title_details_modal")
                ) {
                    MediaDetailScreen(
                        detailState = detailState,
                        isFavorited = activeDetailSavedItem?.isFavorite == true,
                        savedPositionMs = activeDetailSavedItem?.watchProgressMs ?: 0L,
                        preferredEngine = preferredEngine,
                        onBack = { showTitleDetailModal = false },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onSelectEpisode = viewModel::selectEpisode,
                        onPlayStream = launchStreamPlayer,
                        onDownloadStream = enqueueStreamDownload,
                        isModal = true,
                        downloads = downloads,
                        onNavigateToOffline = {
                            showTitleDetailModal = false
                            navController.navigate(LibraryRoute) {
                                popUpTo<DiscoverRoute> { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onSelectRelatedMedia = { rel -> viewModel.loadMediaDetail(rel) }
                    )
                }
            }
        }
    }
}
}

@Composable
private fun FloatingBottomDock(
    items: List<TopNavItem<out Any>>,
    isSelected: (TopNavItem<out Any>) -> Boolean,
    amoledBlack: Boolean,
    hasActiveDownloads: Boolean,
    onNavigate: (TopNavItem<out Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    val dockShape = RoundedCornerShape(32.dp)
    val dockColor = Color(0xFF0A0A0A).copy(alpha = 0.95f)
    val borderAlpha = 0.10f
    val accentPrimary = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    val infinitePulse = rememberInfiniteTransition(label = "vault_pulse")
    val pulseAlpha by infinitePulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val pulseScale by infinitePulse.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        shape = dockShape,
        color = dockColor,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 24.dp, shape = dockShape, clip = false)
            .border(width = 1.dp, color = Color.White.copy(alpha = borderAlpha), shape = dockShape)
            .clip(dockShape)
            .testTag("floating_bottom_dock")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = isSelected(item)
                val pillShape = RoundedCornerShape(24.dp)
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.96f else 1f,
                    animationSpec = tween(150),
                    label = "tab_scale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(pillShape)
                        .background(
                            if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                            shape = pillShape
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigate(item)
                        }
                        .padding(vertical = 6.dp)
                        .testTag(item.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = stringResource(item.labelRes),
                                tint = if (selected) Color.White else Color(0xFFA8B3C4),
                                modifier = Modifier.size(22.dp)
                            )
                            if (item.testTag == "nav_library" && hasActiveDownloads) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .graphicsLayer {
                                            scaleX = pulseScale
                                            scaleY = pulseScale
                                            alpha = pulseAlpha
                                        }
                                        .clip(CircleShape)
                                        .background(accentPrimary)
                                )
                            }
                        }
                        Text(
                            text = stringResource(item.labelRes),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) Color.White else Color(0xFFA8B3C4),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
