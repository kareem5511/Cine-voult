package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.SurfaceView
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.UploadFile
import kotlin.math.abs
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.PlaybackEngine
import com.example.data.model.StremioSubtitle
import com.example.data.model.UserPreferences
import com.example.player.AspectMode
import com.example.player.ExoPlayerController
import com.example.player.PlaybackRequest
import com.example.player.PlayerEpisodeItem
import com.example.player.StreamAutoMatcher
import com.example.player.StreamFingerprint
import com.example.player.StreamSourceOption
import com.example.player.UpNextInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    request: PlaybackRequest,
    subtitles: List<StremioSubtitle>,
    preferences: UserPreferences,
    onUpdateProgress: (mediaKey: String, title: String, mediaType: String, posterUrl: String, streamUrl: String, positionMs: Long, durationMs: Long) -> Unit,
    onUpdateSubtitleDelay: (Int) -> Unit,
    onResolveSources: suspend (type: String, mediaId: String) -> List<StreamSourceOption> = { _, _ -> emptyList() },
    onResolveSubtitles: suspend (type: String, mediaId: String) -> List<StremioSubtitle> = { _, _ -> emptyList() },
    onResolveNextQueueItem: (currentMediaKey: String) -> UpNextInfo? = { null },
    onSaveStreamPreference: (StreamFingerprint) -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    var activeMediaKey by remember { mutableStateOf(request.mediaKey) }
    var activeTitle by remember { mutableStateOf(request.title) }
    val baseShowTitle = remember(request.title) {
        request.title.substringBefore(" • ").ifBlank { request.title }
    }
    var activeMediaType by remember { mutableStateOf(request.mediaType) }
    var activeStreamUrl by remember { mutableStateOf(request.streamUrl) }
    var activePosterUrl by remember { mutableStateOf(request.posterUrl) }
    var activeImdbId by remember { mutableStateOf(request.imdbId) }
    var activeLocalSubUri by remember { mutableStateOf(request.localSubtitleUri) }
    var activeInitialSeekMs by remember { mutableLongStateOf(request.initialPositionMs) }

    val seriesEpisodes = remember(request.mediaKey, request.episodes) { request.episodes }
    var activeSeason by remember(request.mediaKey) {
        mutableStateOf(
            request.currentSeason
                ?: seriesEpisodes.firstOrNull { it.id == request.mediaKey }?.season
        )
    }
    var activeEpisode by remember(request.mediaKey) {
        mutableStateOf(
            request.currentEpisode
                ?: seriesEpisodes.firstOrNull { it.id == request.mediaKey }?.episode
        )
    }

    var availableSources by remember(request.mediaKey) {
        val initial = if (request.availableSources.isNotEmpty()) request.availableSources
        else listOf(
            StreamSourceOption(
                id = "primary",
                fullFileName = request.title,
                badges = listOf("Streaming"),
                streamUrl = request.streamUrl
            )
        )
        mutableStateOf(initial)
    }
    var activeStreamPreference by remember(request.mediaKey) {
        val inferred = request.streamPreference ?: availableSources
            .firstOrNull { it.streamUrl == request.streamUrl }
            ?.let { StreamAutoMatcher.extractFingerprint(it) }
        mutableStateOf(inferred)
    }
    var activeSubtitles by remember(request.mediaKey) { mutableStateOf(subtitles) }
    var currentUpNext by remember(request.mediaKey) { mutableStateOf(request.upNext) }

    var controlsVisible by remember { mutableStateOf(true) }
    var showGestureHint by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var showUnlockButton by remember { mutableStateOf(false) }
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSourcesSheet by remember { mutableStateOf(false) }
    var showEpisodeDropdown by remember { mutableStateOf(false) }
    var selectedDropdownSeason by remember(activeSeason) {
        mutableStateOf(activeSeason ?: seriesEpisodes.firstOrNull()?.season ?: 1)
    }
    var introSkipped by remember(activeMediaKey) { mutableStateOf(false) }
    var upNextDismissed by remember(activeMediaKey) { mutableStateOf(false) }
    var isSwitchingUpNext by remember { mutableStateOf(false) }
    var isResolvingEpisodeStreams by remember { mutableStateOf(false) }
    var autoMatchToast by remember { mutableStateOf<String?>(null) }
    var autoMatchToastToken by remember { mutableIntStateOf(0) }
    var episodeEmptyError by remember { mutableStateOf<String?>(null) }

    // Brightness state (0.05f .. 1.0f) — touch-gesture controlled
    var brightnessLevel by remember {
        val currentAttr = activity?.window?.attributes?.screenBrightness ?: -1f
        val initVal = if (currentAttr in 0.01f..1.0f) {
            currentAttr
        } else {
            val sysVal = runCatching {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
            }.getOrElse { 0.6f }
            sysVal.coerceIn(0.05f, 1.0f)
        }
        mutableFloatStateOf(initVal)
    }

    // Volume state (0f .. 1f) — touch-gesture controlled
    val maxMusicVol = remember(audioManager) {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
    }
    var volumeLevel by remember(audioManager, maxMusicVol) {
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxMusicVol / 2)
        mutableFloatStateOf((current.toFloat() / maxMusicVol.toFloat()).coerceIn(0f, 1f))
    }
    var activeOverlayHud by remember { mutableStateOf<String?>(null) }
    var activeOverlayIcon by remember { mutableStateOf<ImageVector?>(null) }
    var activeOverlayProgress by remember { mutableStateOf<Float?>(null) }
    var hudToken by remember { mutableIntStateOf(0) }

    fun applyBrightness(fraction: Float) {
        val clamped = fraction.coerceIn(0.05f, 1.0f)
        brightnessLevel = clamped
        activity?.window?.let { win ->
            val attrs = win.attributes
            attrs.screenBrightness = clamped
            win.attributes = attrs
        }
        activeOverlayIcon = when {
            clamped < 0.35f -> Icons.Rounded.BrightnessLow
            clamped < 0.7f -> Icons.Rounded.BrightnessMedium
            else -> Icons.Rounded.BrightnessHigh
        }
        activeOverlayProgress = clamped
        activeOverlayHud = "Brightness ${(clamped * 100).roundToInt()}%"
        hudToken++
        lastInteractionMs = System.currentTimeMillis()
    }

    fun applyVolume(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        volumeLevel = clamped
        val targetVol = (clamped * maxMusicVol).roundToInt().coerceIn(0, maxMusicVol)
        runCatching {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
        }
        activeOverlayIcon = when {
            clamped <= 0.01f -> Icons.AutoMirrored.Rounded.VolumeOff
            clamped < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
            else -> Icons.AutoMirrored.Rounded.VolumeUp
        }
        activeOverlayProgress = clamped
        activeOverlayHud = "Volume ${(clamped * 100).roundToInt()}%"
        hudToken++
        lastInteractionMs = System.currentTimeMillis()
    }

    // Auto-clear transient gesture HUD badge & unlock button
    LaunchedEffect(hudToken) {
        if (hudToken > 0) {
            delay(1150L)
            activeOverlayHud = null
            activeOverlayIcon = null
            activeOverlayProgress = null
        }
    }
    LaunchedEffect(showUnlockButton) {
        if (showUnlockButton) {
            delay(3000L)
            showUnlockButton = false
        }
    }
    LaunchedEffect(Unit) {
        delay(3000L)
        showGestureHint = false
    }
    LaunchedEffect(autoMatchToastToken) {
        if (autoMatchToastToken > 0) {
            delay(3000L)
            autoMatchToast = null
        }
    }

    val exoController = remember { ExoPlayerController(context) }
    val uiState by exoController.state.collectAsStateWithLifecycle()

    // Resolve sources/subtitles dynamically if missing (e.g. launched from Vault or Up Next)
    LaunchedEffect(activeImdbId, activeMediaType) {
        if (activeSubtitles.isEmpty() && activeImdbId.isNotBlank()) {
            val fetchedSubs = onResolveSubtitles(
                if (activeMediaType == "series") "series" else "movie",
                activeImdbId
            )
            if (fetchedSubs.isNotEmpty()) activeSubtitles = fetchedSubs
        }
        if (availableSources.size <= 1 && activeImdbId.isNotBlank()) {
            val fetchedSources = onResolveSources(
                if (activeMediaType == "series") "series" else "movie",
                activeImdbId
            )
            if (fetchedSources.isNotEmpty()) availableSources = fetchedSources
        }
    }

    val subtitleFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { subUri: Uri? ->
        if (subUri != null) {
            exoController.addLocalSubtitle(subUri)
        }
    }

    // Initialize or switch stream source
    LaunchedEffect(activeStreamUrl) {
        if (activeStreamUrl.isBlank()) return@LaunchedEffect
        val resumePos = if (uiState.positionMs > 0L) uiState.positionMs else activeInitialSeekMs
        activeInitialSeekMs = 0L

        exoController.initialize(
            streamUrl = activeStreamUrl,
            initialPositionMs = resumePos,
            hardwareDecoding = preferences.hardwareDecoding,
            preferredAudioLang = preferences.preferredAudioLang,
            preferredSubtitleLang = preferences.preferredSubtitleLang,
            subtitles = activeSubtitles,
            localSubtitleUri = activeLocalSubUri
        )
    }

    // Only apply subtitle styles when preferences actually change
    LaunchedEffect(preferences) {
        exoController.applySubtitleStyle(preferences)
    }

    // Dynamic subtitle vertical offset so captions never overlap bottom player controls
    val subtitleBottomFraction by animateFloatAsState(
        targetValue = if (controlsVisible && !isScreenLocked) 0.19f else 0.06f,
        label = "subtitle_bottom_fraction"
    )
    LaunchedEffect(subtitleBottomFraction) {
        exoController.setSubtitleBottomPadding(subtitleBottomFraction)
    }

    // Auto-hide controls smoothly during active playback (pauses while dropdown or modal sheets are open)
    LaunchedEffect(
        controlsVisible,
        uiState.isPlaying,
        lastInteractionMs,
        showSubtitleSheet,
        showAudioSheet,
        showSpeedSheet,
        showSourcesSheet,
        showEpisodeDropdown,
        isResolvingEpisodeStreams
    ) {
        if (controlsVisible && uiState.isPlaying &&
            !showSubtitleSheet && !showAudioSheet && !showSpeedSheet && !showSourcesSheet &&
            !showEpisodeDropdown && !isResolvingEpisodeStreams
        ) {
            delay(4500L)
            controlsVisible = false
        }
    }

    // Poll player position & sync watch progress periodically
    LaunchedEffect(activeMediaKey) {
        var tick = 0
        while (true) {
            delay(500L)
            exoController.pollPosition()
            tick++
            if (tick % 10 == 0) {
                val pos = exoController.currentPositionMs()
                val dur = exoController.currentDurationMs()
                if (pos > 0L) {
                    onUpdateProgress(activeMediaKey, activeTitle, activeMediaType, activePosterUrl, activeStreamUrl, pos, dur)
                }
            }
        }
    }

    // Keep screen on, hide status/nav bars (true immersive mode), use cutout edges, & restore cleanly when leaving
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    var isLandscape by remember {
        mutableStateOf(configuration.orientation != Configuration.ORIENTATION_PORTRAIT)
    }

    DisposableEffect(Unit) {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        val originalBrightness = window?.attributes?.screenBrightness
            ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        val originalCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && window != null) {
            window.attributes.layoutInDisplayCutoutMode
        } else 0

        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && window != null) {
            val attrs = window.attributes
            attrs.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = attrs
        }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        isLandscape = true

        onDispose {
            val pos = exoController.currentPositionMs()
            val dur = exoController.currentDurationMs()
            if (pos > 0L) {
                onUpdateProgress(activeMediaKey, activeTitle, activeMediaType, activePosterUrl, activeStreamUrl, pos, dur)
            }
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && window != null) {
                val attrs = window.attributes
                attrs.layoutInDisplayCutoutMode = originalCutoutMode
                window.attributes = attrs
            }
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window?.let { win ->
                val attrs = win.attributes
                attrs.screenBrightness = originalBrightness
                win.attributes = attrs
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            exoController.release()
        }
    }

    fun switchToEpisode(
        epId: String,
        epTitle: String,
        season: Int?,
        episode: Int?,
        posterFallback: String = "",
        directStreamUrl: String = ""
    ) {
        if (isSwitchingUpNext || isResolvingEpisodeStreams) return
        isSwitchingUpNext = true
        isResolvingEpisodeStreams = true
        showEpisodeDropdown = false
        episodeEmptyError = null
        lastInteractionMs = System.currentTimeMillis()
        scope.launch {
            val resolvedSources = onResolveSources("series", epId).ifEmpty {
                if (directStreamUrl.isNotBlank()) {
                    listOf(
                        StreamSourceOption(
                            id = "up_next_direct",
                            fullFileName = epTitle,
                            badges = listOf("Streaming"),
                            streamUrl = directStreamUrl
                        )
                    )
                } else emptyList()
            }

            val bestMatch = StreamAutoMatcher.pickBestMatch(
                sources = resolvedSources,
                preference = activeStreamPreference,
                preferredAudioLang = preferences.preferredAudioLang
            )

            val epCode = if (season != null && episode != null) "S$season:E$episode" else ""
            val formattedTitle = if (epCode.isNotBlank()) {
                "$baseShowTitle • $epCode"
            } else {
                epTitle.ifBlank { activeTitle }
            }

            if (bestMatch != null && bestMatch.streamUrl.isNotBlank()) {
                val resolvedSubs = onResolveSubtitles("series", epId)
                activeMediaKey = epId
                activeTitle = formattedTitle
                activeMediaType = "series"
                activeSeason = season
                activeEpisode = episode
                if (posterFallback.isNotBlank()) {
                    activePosterUrl = posterFallback
                }
                activeImdbId = epId
                activeLocalSubUri = ""
                activeInitialSeekMs = 0L
                availableSources = resolvedSources
                activeSubtitles = resolvedSubs
                currentUpNext = onResolveNextQueueItem(epId)
                introSkipped = false
                upNextDismissed = false
                episodeEmptyError = null

                val matchedRes = bestMatch.resolution.ifBlank {
                    bestMatch.badges.firstOrNull { it.isNotBlank() } ?: "Best Match"
                }
                val matchedAddon = bestMatch.addonName.ifBlank {
                    activeStreamPreference?.addonName?.takeIf { it.isNotBlank() }
                        ?: bestMatch.badges.drop(1).firstOrNull()
                        ?: "Stream"
                }
                autoMatchToast = "Auto-matched: $matchedRes • $matchedAddon"
                autoMatchToastToken++
                activeStreamUrl = bestMatch.streamUrl
            } else {
                exoController.release()
                activeMediaKey = epId
                activeTitle = formattedTitle
                activeMediaType = "series"
                activeSeason = season
                activeEpisode = episode
                activeImdbId = epId
                availableSources = emptyList()
                currentUpNext = onResolveNextQueueItem(epId)
                introSkipped = false
                upNextDismissed = true
                activeStreamUrl = ""
                episodeEmptyError = "No streams available for this episode yet"
            }
            isResolvingEpisodeStreams = false
            isSwitchingUpNext = false
        }
    }

    fun playUpNext(next: UpNextInfo) {
        switchToEpisode(
            epId = next.imdbId.ifBlank { next.mediaKey },
            epTitle = next.title,
            season = next.season,
            episode = next.episode,
            posterFallback = next.posterUrl,
            directStreamUrl = next.streamUrl
        )
    }

    BackHandler {
        if (showEpisodeDropdown) {
            showEpisodeDropdown = false
        } else {
            onClose()
        }
    }

    val isSeries = activeMediaType.equals("series", ignoreCase = true)
    val showSkipIntro = !introSkipped &&
        !isResolvingEpisodeStreams &&
        episodeEmptyError == null &&
        uiState.durationMs >= 120_000L &&
        uiState.positionMs in 2_000L..120_000L

    val remainingMs = if (uiState.durationMs > 0L) (uiState.durationMs - uiState.positionMs).coerceAtLeast(0L) else Long.MAX_VALUE
    val showUpNextCard = isSeries &&
        currentUpNext != null &&
        !upNextDismissed &&
        !isResolvingEpisodeStreams &&
        episodeEmptyError == null &&
        (uiState.isEnded || (uiState.durationMs >= 30_000L && remainingMs <= 20_000L))

    var upNextCountdownSec by remember(activeMediaKey, uiState.isEnded, upNextDismissed) {
        mutableIntStateOf(10)
    }
    LaunchedEffect(showUpNextCard, uiState.isEnded, upNextDismissed, currentUpNext?.mediaKey) {
        if (showUpNextCard && uiState.isEnded && !upNextDismissed) {
            val targetNext = currentUpNext ?: return@LaunchedEffect
            upNextCountdownSec = 10
            for (sec in 10 downTo 1) {
                upNextCountdownSec = sec
                delay(1000L)
            }
            if (!upNextDismissed && !isResolvingEpisodeStreams) {
                playUpNext(targetNext)
            }
        }
    }

    var gestureMode by remember { mutableStateOf<String?>(null) } // "bright" | "vol" | "seek"
    var seekPreviewTargetMs by remember { mutableLongStateOf(0L) }
    var seekDragAccumX by remember { mutableFloatStateOf(0f) }
    val latestUiState by rememberUpdatedState(uiState)
    val latestBrightness by rememberUpdatedState(brightnessLevel)
    val latestVolume by rememberUpdatedState(volumeLevel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isScreenLocked) {
                detectTapGestures(
                    onTap = {
                        if (isScreenLocked) {
                            showUnlockButton = !showUnlockButton
                        } else {
                            controlsVisible = !controlsVisible
                            lastInteractionMs = System.currentTimeMillis()
                        }
                    },
                    onDoubleTap = { offset ->
                        if (isScreenLocked) {
                            showUnlockButton = true
                            return@detectTapGestures
                        }
                        val width = size.width.coerceAtLeast(1)
                        val seekDelta = if (offset.x < width / 2f) -10_000L else 10_000L
                        exoController.seekBy(seekDelta)
                        activeOverlayIcon = if (seekDelta < 0) Icons.Rounded.Replay10 else Icons.Rounded.Forward10
                        activeOverlayProgress = null
                        activeOverlayHud = if (seekDelta < 0) "-10s" else "+10s"
                        hudToken++
                        lastInteractionMs = System.currentTimeMillis()
                    }
                )
            }
            .pointerInput(isScreenLocked, maxMusicVol) {
                detectDragGestures(
                    onDragStart = { _ ->
                        if (isScreenLocked) return@detectDragGestures
                        gestureMode = null
                        seekDragAccumX = 0f
                        seekPreviewTargetMs = latestUiState.positionMs
                    },
                    onDragEnd = {
                        if (gestureMode == "seek" && !isScreenLocked && latestUiState.durationMs > 0L) {
                            exoController.seekTo(seekPreviewTargetMs)
                        }
                        gestureMode = null
                    },
                    onDragCancel = {
                        gestureMode = null
                    },
                    onDrag = { change, dragAmount ->
                        if (isScreenLocked) return@detectDragGestures
                        change.consume()
                        val width = size.width.coerceAtLeast(1)
                        val height = size.height.coerceAtLeast(1)
                        if (gestureMode == null) {
                            gestureMode = if (abs(dragAmount.x) > abs(dragAmount.y) * 1.15f) {
                                "seek"
                            } else if (change.position.x < width / 2f) {
                                "bright"
                            } else {
                                "vol"
                            }
                        }
                        when (gestureMode) {
                            "bright" -> applyBrightness(latestBrightness - dragAmount.y / (height * 0.65f))
                            "vol" -> applyVolume(latestVolume - dragAmount.y / (height * 0.65f))
                            "seek" -> {
                                val dur = latestUiState.durationMs
                                if (dur > 0L) {
                                    seekDragAccumX += dragAmount.x
                                    val deltaMs = ((seekDragAccumX / width.toFloat()) * 90_000f).toLong()
                                    val target = (latestUiState.positionMs + deltaMs).coerceIn(0L, dur)
                                    seekPreviewTargetMs = target
                                    val sign = if (deltaMs >= 0) "+" else "-"
                                    activeOverlayIcon = Icons.Rounded.FastForward
                                    activeOverlayProgress = target.toFloat() / dur.toFloat().coerceAtLeast(1f)
                                    activeOverlayHud = "Seek $sign${formatDuration(abs(deltaMs))} • ${formatDuration(target)}"
                                    hudToken++
                                }
                            }
                        }
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).also { pv ->
                    exoController.attachPlayerView(pv, preferences)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if ((uiState.isBuffering || isResolvingEpisodeStreams) && uiState.errorMessage == null && episodeEmptyError == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(
                        if (isResolvingEpisodeStreams) Modifier.testTag("player_episode_loading_overlay")
                        else Modifier
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                if (isResolvingEpisodeStreams) {
                    Text(
                        text = "Matching preferred stream...",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Auto-Matched Stream Confirmation Toast (fades out after 3 seconds)
        AnimatedVisibility(
            visible = !autoMatchToast.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 62.dp)
                .zIndex(25f)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                modifier = Modifier.testTag("player_automatch_toast")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = autoMatchToast.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        // Transient Touch Gesture Feedback Pill (Brightness / Volume / Seek) — sliders stay hidden
        AnimatedVisibility(
            visible = !activeOverlayHud.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 64.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.82f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeOverlayIcon?.let { iconVec ->
                            Icon(
                                imageVector = iconVec,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = activeOverlayHud.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    activeOverlayProgress?.let { prog ->
                        LinearProgressIndicator(
                            progress = { prog.coerceIn(0f, 1f) },
                            modifier = Modifier.width(150.dp).height(4.dp)
                        )
                    }
                }
            }
        }

        // Screen Unlock Floating Pill (when locked)
        AnimatedVisibility(
            visible = isScreenLocked && showUnlockButton,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    isScreenLocked = false
                    showUnlockButton = false
                    controlsVisible = true
                    lastInteractionMs = System.currentTimeMillis()
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("player_unlock_button")
            ) {
                Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Unlock")
            }
        }

        if (!episodeEmptyError.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .widthIn(max = 360.dp)
                    .zIndex(20f)
                    .testTag("player_no_streams_overlay")
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = episodeEmptyError!!,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isSeries && seriesEpisodes.isNotEmpty()) {
                            Button(
                                onClick = {
                                    controlsVisible = true
                                    showEpisodeDropdown = true
                                },
                                modifier = Modifier.testTag("player_no_streams_episode_list_button")
                            ) {
                                Text("Choose Episode")
                            }
                        }
                        OutlinedButton(onClick = onClose) {
                            Text("Exit Player")
                        }
                    }
                }
            }
        } else if (!uiState.errorMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                exoController.retry()
                            }
                        ) {
                            Text("Retry")
                        }
                        if (availableSources.size > 1) {
                            OutlinedButton(onClick = { showSourcesSheet = true }) {
                                Text("Switch Source")
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && !isScreenLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top Bar: Back (Top-Left Only), Lock, Screen Rotation, Engine Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Close player",
                            tint = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {

                        IconButton(
                            onClick = {
                                val nextLandscape = !isLandscape
                                isLandscape = nextLandscape
                                activity?.requestedOrientation = if (nextLandscape) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                }
                                lastInteractionMs = System.currentTimeMillis()
                            },
                            modifier = Modifier.testTag("player_rotate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ScreenRotation,
                                contentDescription = "Rotate screen",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                isScreenLocked = true
                                controlsVisible = false
                                showUnlockButton = true
                            },
                            modifier = Modifier.testTag("player_lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Lock screen",
                                tint = Color.White
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    lastInteractionMs = System.currentTimeMillis()
                                    exoController.cycleAspectMode()
                                }
                                .testTag("player_aspect_mode_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.AspectRatio,
                                    contentDescription = "Aspect ratio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (uiState.aspectMode) {
                                        AspectMode.FIT -> "Fit"
                                        AspectMode.ZOOM -> "Fill"
                                        AspectMode.STRETCH -> "Stretch"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Center Transport Controls (minimalist, borderless, spaced widely)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(80.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            lastInteractionMs = System.currentTimeMillis()
                            exoController.seekBy(-10_000L)
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Surface(
                        color = Color.Transparent,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable {
                                lastInteractionMs = System.currentTimeMillis()
                                exoController.togglePlayPause()
                            }
                            .testTag("player_play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            lastInteractionMs = System.currentTimeMillis()
                            exoController.seekBy(10_000L)
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Isolated Bottom Scrub Bar + Bottom-Left Title/Episode/Year Block + Telemetry Pill
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val bufferedMb = (uiState.bufferedBytesEstimate / (1024.0 * 1024.0)).coerceAtLeast(0.0)
                    val speedMb = (uiState.networkSpeedBytesPerSec / (1024.0 * 1024.0)).coerceAtLeast(0.0)
                    val bufferedLabel = if (bufferedMb >= 10.0) {
                        "+${bufferedMb.roundToInt()} MB buffered"
                    } else {
                        String.format("+%.1f MB buffered", bufferedMb)
                    }
                    val speedLabel = String.format("%.1f MB/s", speedMb)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Bottom-Left Title, Subtitle, and Year Block
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val showTitle = activeTitle.substringBefore(" • ").ifBlank { activeTitle }
                            Text(
                                text = showTitle,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isSeries && seriesEpisodes.isNotEmpty()) {
                                val sNum = activeSeason ?: 1
                                val eNum = activeEpisode ?: 1
                                val epName = activeTitle.substringAfter(" • ", "Episode $eNum")
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedDropdownSeason = sNum
                                            showEpisodeDropdown = !showEpisodeDropdown
                                            lastInteractionMs = System.currentTimeMillis()
                                        }
                                        .testTag("player_episode_dropdown_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "S$sNum E$eNum • $epName ▾",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Next Player",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "2026",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }

                        Surface(
                            color = Color.Black.copy(alpha = 0.56f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.testTag("player_buffer_telemetry")
                        ) {
                            Text(
                                text = "$bufferedLabel • $speedLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.86f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    PlayerTimelineBar(
                        positionMs = uiState.positionMs,
                        bufferedPositionMs = uiState.bufferedPositionMs,
                        durationMs = uiState.durationMs,
                        onSeekTo = { target ->
                            lastInteractionMs = System.currentTimeMillis()
                            exoController.seekTo(target)
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatDuration(uiState.positionMs)} / ${formatDuration(uiState.durationMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        lastInteractionMs = System.currentTimeMillis()
                                        showSourcesSheet = true
                                    }
                                    .testTag("player_sources_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Dns,
                                        contentDescription = "Switch video source",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (availableSources.size > 1) "Sources (${availableSources.size})" else "Source",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    lastInteractionMs = System.currentTimeMillis()
                                    showSubtitleSheet = true
                                },
                                modifier = Modifier.testTag("player_subtitles_button")
                            ) {
                                Icon(Icons.Rounded.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                            }
                            IconButton(
                                onClick = {
                                    lastInteractionMs = System.currentTimeMillis()
                                    showAudioSheet = true
                                },
                                modifier = Modifier.testTag("player_audio_button")
                            ) {
                                Icon(Icons.Rounded.AudioFile, contentDescription = "Audio tracks", tint = Color.White)
                            }
                            IconButton(
                                onClick = {
                                    lastInteractionMs = System.currentTimeMillis()
                                    showSpeedSheet = true
                                }
                            ) {
                                Icon(Icons.Rounded.Speed, contentDescription = "Playback speed", tint = Color.White)
                            }
                            IconButton(
                                onClick = {
                                    lastInteractionMs = System.currentTimeMillis()
                                    exoController.cycleAspectMode()
                                    val nextLabel = when (uiState.aspectMode) {
                                        AspectMode.FIT -> "Zoom (Fill)"
                                        AspectMode.ZOOM -> "Stretch"
                                        AspectMode.STRETCH -> "Fit"
                                    }
                                    activeOverlayIcon = Icons.Rounded.AspectRatio
                                    activeOverlayProgress = null
                                    activeOverlayHud = "Aspect: $nextLabel"
                                    hudToken++
                                }
                            ) {
                                Icon(Icons.Rounded.AspectRatio, contentDescription = "Aspect ratio", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Skip Intro Pill Button (visible independently during intro window)
        AnimatedVisibility(
            visible = showSkipIntro,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 18.dp, bottom = if (controlsVisible) 108.dp else 28.dp)
        ) {
            Button(
                onClick = {
                    introSkipped = true
                    val target = (uiState.positionMs + 85_000L).coerceAtMost(uiState.durationMs - 1_000L)
                    exoController.seekTo(target)
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("player_skip_intro_button")
            ) {
                Icon(Icons.Rounded.FastForward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Skip Intro")
            }
        }

        // Series Season & Episode Selector Dropdown Popover
        AnimatedVisibility(
            visible = showEpisodeDropdown && isSeries && seriesEpisodes.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(30f)
        ) {
            val seasons = remember(seriesEpisodes) {
                seriesEpisodes.mapNotNull { it.season }.distinct().sorted().ifEmpty { listOf(1) }
            }
            val seasonEpisodes = remember(seriesEpisodes, selectedDropdownSeason) {
                seriesEpisodes.filter { (it.season ?: 1) == selectedDropdownSeason }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showEpisodeDropdown = false }
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 16.dp, top = 58.dp, end = 16.dp, bottom = 24.dp)
            ) {
                Surface(
                    color = Color(0xFF0E1318).copy(alpha = 0.96f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    tonalElevation = 12.dp,
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .heightIn(max = 360.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                        .testTag("player_episode_dropdown_menu")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episodes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            IconButton(
                                onClick = { showEpisodeDropdown = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close episode list",
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons, key = { it }) { seasonNum ->
                                val selected = seasonNum == selectedDropdownSeason
                                Surface(
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.White.copy(alpha = 0.08f)
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { selectedDropdownSeason = seasonNum }
                                        .testTag("player_dropdown_season_$seasonNum")
                                ) {
                                    Text(
                                        text = "Season $seasonNum",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(seasonEpisodes, key = { it.id }) { ep ->
                                val isCurrent = ep.id == activeMediaKey
                                val epNum = ep.episode ?: (seasonEpisodes.indexOf(ep) + 1)
                                Surface(
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    } else {
                                        Color.White.copy(alpha = 0.05f)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (isCurrent) {
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (!isCurrent) {
                                                switchToEpisode(
                                                    epId = ep.id,
                                                    epTitle = ep.title,
                                                    season = ep.season ?: selectedDropdownSeason,
                                                    episode = epNum,
                                                    posterFallback = ep.thumbnail
                                                )
                                            } else {
                                                showEpisodeDropdown = false
                                            }
                                        }
                                        .testTag("player_dropdown_ep_${ep.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(76.dp)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.08f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (ep.thumbnail.isNotBlank()) {
                                                AsyncImage(
                                                    model = ep.thumbnail,
                                                    contentDescription = ep.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.72f),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp)
                                            ) {
                                                Text(
                                                    text = "E$epNum",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = ep.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (ep.runtime.isNotBlank()) {
                                                Text(
                                                    text = ep.runtime,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.62f),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = "Now playing",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
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

        // Up Next Floating Card (Series Only — final 20s or onEnded countdown)
        AnimatedVisibility(
            visible = showUpNextCard,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(end = 18.dp, bottom = if (controlsVisible) 108.dp else 28.dp)
        ) {
            currentUpNext?.let { next ->
                val eyebrow = if (next.season != null && next.episode != null) {
                    "UP NEXT • S${next.season}:E${next.episode}"
                } else {
                    "UP NEXT"
                }
                val thumbUrl = next.posterUrl.ifBlank { activePosterUrl }
                Surface(
                    color = Color(0xFF0E1318).copy(alpha = 0.96f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .widthIn(max = 330.dp)
                        .testTag("player_up_next_card")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(88.dp)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                if (thumbUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = thumbUrl,
                                        contentDescription = next.subtitle.ifBlank { next.title },
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = eyebrow,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = next.subtitle.ifBlank { next.title },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { upNextDismissed = true },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("player_up_next_dismiss")
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Dismiss up next",
                                    tint = Color.White.copy(alpha = 0.78f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { playUpNext(next) },
                            enabled = !isSwitchingUpNext && !isResolvingEpisodeStreams,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_up_next_play_button")
                        ) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                when {
                                    isSwitchingUpNext || isResolvingEpisodeStreams -> "Loading..."
                                    uiState.isEnded -> "Play Next (${upNextCountdownSec}s)"
                                    else -> "Play Next"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSourcesSheet) {
        VideoSourceSwitchSheet(
            sources = availableSources,
            activeStreamUrl = activeStreamUrl,
            onSelectSource = { source ->
                activeInitialSeekMs = exoController.currentPositionMs()
                val fp = StreamAutoMatcher.extractFingerprint(source)
                activeStreamPreference = fp
                onSaveStreamPreference(fp)
                activeStreamUrl = source.streamUrl
                showSourcesSheet = false
            },
            onDismiss = { showSourcesSheet = false }
        )
    }

    if (showSubtitleSheet) {
        SubtitleStudioSheet(
            tracks = uiState.subtitleTracks,
            delayMs = uiState.activeSubtitleDelayMs,
            onSelectTrack = { track ->
                exoController.selectSubtitleTrack(track)
                showSubtitleSheet = false
            },
            onImportFile = {
                subtitleFilePicker.launch(arrayOf("text/*", "application/x-subrip", "*/*"))
            },
            onChangeDelay = { newDelay ->
                onUpdateSubtitleDelay(newDelay)
            },
            onDismiss = { showSubtitleSheet = false }
        )
    }

    if (showAudioSheet) {
        AudioTrackSheet(
            tracks = uiState.audioTracks,
            onSelect = { id ->
                exoController.selectAudioTrack(id)
                showAudioSheet = false
            },
            onDismiss = { showAudioSheet = false }
        )
    }

    if (showSpeedSheet) {
        SpeedSheet(
            currentSpeed = uiState.playbackSpeed,
            onSelectSpeed = { speed ->
                exoController.setSpeed(speed)
                showSpeedSheet = false
            },
            onDismiss = { showSpeedSheet = false }
        )
    }
}

@Composable
private fun PlayerTimelineBar(
    positionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val accentColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var barWidthPx by remember { mutableIntStateOf(1) }

    val activeFraction = if (isDragging) {
        dragFraction.coerceIn(0f, 1f)
    } else {
        (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    }
    val bufferedFraction = (bufferedPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(activeFraction, 1f)
    val latestDuration by rememberUpdatedState(safeDuration)
    val latestOnSeekTo by rememberUpdatedState(onSeekTo)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .onSizeChanged { barWidthPx = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val w = size.width.coerceAtLeast(1).toFloat()
                    val f = (offset.x / w).coerceIn(0f, 1f)
                    latestOnSeekTo((f * latestDuration).toLong())
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val w = size.width.coerceAtLeast(1).toFloat()
                        dragFraction = (offset.x / w).coerceIn(0f, 1f)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val w = size.width.coerceAtLeast(1).toFloat()
                        dragFraction = (change.position.x / w).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        latestOnSeekTo((dragFraction * latestDuration).toLong())
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
            .testTag("player_multi_layer_seekbar"),
        contentAlignment = Alignment.Center
    ) {
        if (isDragging) {
            val targetMs = (dragFraction * safeDuration).toLong()
            val deltaMs = targetMs - positionMs
            val sign = if (deltaMs >= 0L) "+" else "-"
            val isCached = targetMs <= bufferedPositionMs
            val tooltipWidthPx = with(density) { 156.dp.roundToPx() }
            val thumbCenterPx = (activeFraction * barWidthPx).roundToInt()
            val tooltipX = (thumbCenterPx - tooltipWidthPx / 2)
                .coerceIn(0, (barWidthPx - tooltipWidthPx).coerceAtLeast(0))
            val tooltipY = -with(density) { 38.dp.roundToPx() }
            Surface(
                color = Color.Black.copy(alpha = 0.92f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(tooltipX, tooltipY) }
                    .widthIn(min = 120.dp)
                    .testTag("player_scrub_tooltip")
            ) {
                Text(
                    text = "${formatDuration(targetMs)} ($sign${formatDuration(abs(deltaMs))})${if (isCached) " • Cached" else ""}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            val trackHeight = 6.dp.toPx()
            val centerY = size.height / 2f
            val topY = centerY - trackHeight / 2f
            val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)

            // 1. Dark background pill track
            drawRoundRect(
                color = Color.White.copy(alpha = 0.20f),
                topLeft = Offset(0f, topY),
                size = Size(size.width, trackHeight),
                cornerRadius = corner
            )

            // 2. Translucent white buffer track
            val bufWidth = (size.width * bufferedFraction).coerceIn(0f, size.width)
            if (bufWidth > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(0f, topY),
                    size = Size(bufWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // 3. Active solid crisp white playback progress bar
            val progWidth = (size.width * activeFraction).coerceIn(0f, size.width)
            if (progWidth > 0f) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(0f, topY),
                    size = Size(progWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // 4. Draggable thumb (hidden by default, smoothly drawn only while scrubbing)
            if (isDragging) {
                val thumbRadius = 9.dp.toPx()
                val thumbX = progWidth.coerceIn(thumbRadius, (size.width - thumbRadius).coerceAtLeast(thumbRadius))
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = thumbRadius * 1.6f,
                    center = Offset(thumbX, centerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = thumbRadius,
                    center = Offset(thumbX, centerY)
                )
                drawCircle(
                    color = accentColor,
                    radius = thumbRadius * 0.5f,
                    center = Offset(thumbX, centerY)
                )
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun VideoSourceSwitchSheet(
    sources: List<StreamSourceOption>,
    activeStreamUrl: String,
    onSelectSource: (StreamSourceOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Switch Video Source", style = MaterialTheme.typography.titleLarge)
            if (sources.isEmpty()) {
                Text(
                    text = "Only the current direct source is available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(280.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sources, key = { it.id }) { source ->
                        val isActive = source.streamUrl == activeStreamUrl
                        Surface(
                            color = if (isActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectSource(source) }
                                .testTag("player_source_option")
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
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        source.badges.forEachIndexed { idx, badge ->
                                            Surface(
                                                color = if (idx == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = badge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (idx == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (isActive) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = "Active source",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = source.fullFileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubtitleStudioSheet(
    tracks: List<com.example.player.PlayerTrackInfo>,
    delayMs: Int,
    onSelectTrack: (com.example.player.PlayerTrackInfo?) -> Unit,
    onImportFile: () -> Unit,
    onChangeDelay: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subtitles & Sync", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = onImportFile) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Load .srt/.vtt")
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Sync Offset: ${if (delayMs > 0) "+${delayMs}ms" else "${delayMs}ms"}",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { onChangeDelay(delayMs - 100) },
                        label = { Text("-0.1s") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { onChangeDelay(delayMs + 100) },
                        label = { Text("+0.1s") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { onChangeDelay(delayMs - 250) },
                        label = { Text("-0.25s") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { onChangeDelay(delayMs + 250) },
                        label = { Text("+0.25s") }
                    )
                    FilterChip(
                        selected = delayMs == 0,
                        onClick = { onChangeDelay(0) },
                        label = { Text("0s") }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.height(260.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTrack(null) }
                    ) {
                        Text("Off / Disable Subtitles", modifier = Modifier.padding(14.dp))
                    }
                }
                items(tracks) { track ->
                    val formatBadge = when {
                        track.label.contains("ass", ignoreCase = true) -> "ASS"
                        track.label.contains("vtt", ignoreCase = true) -> "VTT"
                        track.label.contains("srt", ignoreCase = true) -> "SRT"
                        else -> null
                    }
                    val sourceBadge = when {
                        track.label.contains("opensubtitle", ignoreCase = true) -> "OpenSubtitles"
                        track.label.contains("embedded", ignoreCase = true) || track.label.contains("internal", ignoreCase = true) -> "Embedded"
                        track.label.contains("external", ignoreCase = true) -> "External"
                        else -> if (formatBadge != null) "Sub" else null
                    }
                    val tagText = listOfNotNull(sourceBadge, formatBadge).joinToString(" • ")

                    Surface(
                        color = if (track.isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = if (track.isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTrack(track) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = track.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (track.isSelected) FontWeight.SemiBold else FontWeight.Normal),
                                    color = if (track.isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                if (tagText.isNotBlank()) {
                                    Text(
                                        text = tagText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                    )
                                }
                            }
                            if (track.isSelected) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrackSheet(
    tracks: List<com.example.player.PlayerTrackInfo>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val activeGreen = Color(0xFF22C55E)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Audio Tracks", style = MaterialTheme.typography.titleLarge)
            if (tracks.isEmpty()) {
                Text(
                    text = "Default stream audio active.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .testTag("audio_tracks_list"),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tracks, key = { it.id }) { track ->
                        Surface(
                            color = if (track.isSelected) {
                                activeGreen.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = if (track.isSelected) {
                                BorderStroke(2.dp, activeGreen)
                            } else {
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(track.id) }
                                .testTag("audio_track_row_${track.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.label,
                                    color = if (track.isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (track.isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (track.isSelected) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = activeGreen
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

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSheet(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Playback Speed", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                    FilterChip(
                        selected = currentSpeed == spd,
                        onClick = { onSelectSpeed(spd) },
                        label = { Text("${spd}x") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0L) String.format("%d:%02d:%02d", hrs, mins, secs)
    else String.format("%02d:%02d", mins, secs)
}
