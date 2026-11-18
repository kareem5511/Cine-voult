package com.example.player

import android.content.Context
import android.net.Uri
import android.util.TypedValue
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.core.graphics.toColorInt
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.FfmpegSubtitleExtractorsFactory
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.withFfmpegSubtitles
import com.example.CineVaultApplication
import com.example.data.model.PlaybackEngine
import com.example.data.model.StremioSubtitle
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
class ExoPlayerController(private val context: Context) {

    private val okHttpClient: OkHttpClient by lazy {
        (context.applicationContext as? CineVaultApplication)?.sharedOkHttpClient
            ?: OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
    }

    private var player: ExoPlayer? = null
    private var attachedView: PlayerView? = null
    private var bandwidthMeter: DefaultBandwidthMeter? = null
    private var currentStreamUrl: String = ""
    private var externalSubtitles: List<StremioSubtitle> = emptyList()
    private var selectedExternalSubUrl: String = ""
    private var subtitlesDisabledByPref: Boolean = false
    private var pendingSubBottomFraction: Float = 0.18f
    private var preferredAudioLanguage: String = ""

    private val _state = MutableStateFlow(ActivePlaybackState(engine = PlaybackEngine.NEXTPLAYER))
    val state: StateFlow<ActivePlaybackState> = _state.asStateFlow()

    fun initialize(
        streamUrl: String,
        initialPositionMs: Long,
        hardwareDecoding: Boolean,
        preferredAudioLang: String,
        preferredSubtitleLang: String,
        subtitles: List<StremioSubtitle>,
        localSubtitleUri: String
    ) {
        currentStreamUrl = streamUrl
        preferredAudioLanguage = preferredAudioLang
        subtitlesDisabledByPref = preferredSubtitleLang.equals("off", ignoreCase = true)
        externalSubtitles = buildList {
            if (localSubtitleUri.isNotBlank()) {
                add(StremioSubtitle(id = "local_sub", url = localSubtitleUri, lang = "Local", subFileName = "Local Subtitle"))
            }
            addAll(subtitles)
        }
        selectedExternalSubUrl = if (subtitlesDisabledByPref) {
            ""
        } else {
            externalSubtitles.firstOrNull { LanguageCodeMatcher.matches(it.lang, preferredSubtitleLang) }?.url
                ?: externalSubtitles.firstOrNull()?.url.orEmpty()
        }

        release()

        val httpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("CineVault-Android/1.0")
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

        val renderersFactory = NextRenderersFactory(context).apply {
            setExtensionRendererMode(
                if (hardwareDecoding) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            )
            setEnableDecoderFallback(true)
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 45_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 3_000
            )
            .build()

        val meter = DefaultBandwidthMeter.Builder(context).build()
        bandwidthMeter = meter

        val extractorsFactory = FfmpegSubtitleExtractorsFactory()

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)
            .withFfmpegSubtitles(dataSourceFactory)

        val exo = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setBandwidthMeter(meter)
            .build()

        val trackParamsBuilder = exo.trackSelectionParameters.buildUpon()
        val audioCodes = LanguageCodeMatcher.getCodes(preferredAudioLang)
        if (audioCodes.isNotEmpty()) {
            trackParamsBuilder.setPreferredAudioLanguages(*audioCodes.toTypedArray())
        }
        if (subtitlesDisabledByPref) {
            trackParamsBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            trackParamsBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            if (preferredSubtitleLang.isNotBlank() && !preferredSubtitleLang.equals("auto", ignoreCase = true)) {
                val subCodes = LanguageCodeMatcher.getCodes(preferredSubtitleLang)
                if (subCodes.isNotEmpty()) {
                    trackParamsBuilder.setPreferredTextLanguages(*subCodes.toTypedArray())
                } else {
                    trackParamsBuilder.setPreferredTextLanguage(preferredSubtitleLang)
                }
            }
        }
        exo.trackSelectionParameters = trackParamsBuilder.build()

        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying, isBuffering = false) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        isEnded = playbackState == Player.STATE_ENDED,
                        durationMs = exo.duration.coerceAtLeast(0L),
                        positionMs = exo.currentPosition.coerceAtLeast(0L),
                        bufferedPositionMs = exo.bufferedPosition.coerceAtLeast(0L)
                    )
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                extractTracks(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = error.localizedMessage ?: "Playback error"
                    )
                }
            }
        })

        player = exo
        _state.update {
            it.copy(
                engine = PlaybackEngine.NEXTPLAYER,
                hardwareDecodingActive = hardwareDecoding,
                isEnded = false,
                errorMessage = null
            )
        }

        setMediaItem(exo, streamUrl, initialPositionMs)
        attachedView?.player = exo
    }

    private fun setMediaItem(exo: ExoPlayer, streamUrl: String, seekMs: Long) {
        val subConfigs = externalSubtitles.mapNotNull { sub ->
            if (sub.url.isBlank()) null
            else {
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                    .setMimeType(guessSubtitleMime(sub.url, sub.subFileName))
                    .setLanguage(sub.lang)
                    .setLabel(sub.subFileName ?: sub.displayLanguage())
                    .setSelectionFlags(
                        if (!subtitlesDisabledByPref && sub.url == selectedExternalSubUrl) C.SELECTION_FLAG_DEFAULT else 0
                    )
                    .build()
            }
        }

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(streamUrl))
            .setSubtitleConfigurations(subConfigs)
            .build()

        exo.setMediaItem(mediaItem)
        if (seekMs > 0L) exo.seekTo(seekMs)
        exo.prepare()
        exo.playWhenReady = true
    }

    fun retry() {
        val exo = player ?: return
        _state.update { it.copy(errorMessage = null, isBuffering = true) }
        exo.prepare()
        exo.playWhenReady = true
    }

    fun attachPlayerView(view: PlayerView, preferences: UserPreferences) {
        attachedView = view
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        view.useController = false
        view.setKeepContentOnPlayerReset(true)
        view.player = player
        applySubtitleStyle(view, preferences)
        view.subtitleView?.setBottomPaddingFraction(pendingSubBottomFraction)
        applyAspectMode(view, _state.value.aspectMode)
    }

    fun setSubtitleBottomPadding(fraction: Float) {
        pendingSubBottomFraction = fraction.coerceIn(0.04f, 0.35f)
        attachedView?.subtitleView?.setBottomPaddingFraction(pendingSubBottomFraction)
    }

    fun applySubtitleStyle(preferences: UserPreferences) {
        applySubtitleStyle(attachedView, preferences)
    }

    fun applySubtitleStyle(view: PlayerView?, preferences: UserPreferences) {
        val subView = view?.subtitleView ?: return
        val fgColor = runCatching { preferences.subtitleColorHex.toColorInt() }
            .getOrElse { android.graphics.Color.WHITE }
        val bgAlpha = (preferences.subtitleBgOpacity * 255).roundToInt().coerceIn(0, 255)
        val bgColor = android.graphics.Color.argb(bgAlpha, 0, 0, 0)
        subView.setApplyEmbeddedStyles(false)
        subView.setStyle(
            CaptionStyleCompat(
                fgColor,
                bgColor,
                android.graphics.Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                android.graphics.Color.BLACK,
                null
            )
        )
        subView.setFixedTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            preferences.subtitleFontSizeSp.toFloat()
        )
        subView.setBottomPaddingFraction(pendingSubBottomFraction)
    }

    fun applyAspectMode(view: PlayerView?, mode: AspectMode) {
        view ?: return
        view.resizeMode = when (mode) {
            AspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            AspectMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        }
    }

    fun togglePlayPause() {
        val exo = player ?: return
        if (exo.isPlaying) exo.pause() else exo.play()
    }

    fun seekTo(positionMs: Long) {
        val exo = player ?: return
        val maxDur = exo.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
        val clamped = positionMs.coerceIn(0L, maxDur)
        exo.seekTo(clamped)
        _state.update { it.copy(positionMs = exo.currentPosition.coerceAtLeast(0L), isEnded = false) }
    }

    fun seekBy(deltaMs: Long) {
        val exo = player ?: return
        seekTo(exo.currentPosition + deltaMs)
    }

    fun setSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
        _state.update { it.copy(playbackSpeed = speed) }
    }

    fun setVolumeMultiplier(multiplier: Float) {
        val clamped = multiplier.coerceIn(0.5f, 2.0f)
        _state.update { it.copy(volumeBoost = clamped) }
        player?.volume = clamped
    }

    fun toggleNightMode(enabled: Boolean) {
        _state.update { it.copy(nightModeActive = enabled) }
        if (enabled) {
            player?.volume = (_state.value.volumeBoost * 0.85f).coerceIn(0.2f, 1.5f)
        } else {
            player?.volume = _state.value.volumeBoost
        }
    }

    fun cycleAspectMode() {
        val next = when (_state.value.aspectMode) {
            AspectMode.FIT -> AspectMode.ZOOM
            AspectMode.ZOOM -> AspectMode.STRETCH
            AspectMode.STRETCH -> AspectMode.FIT
        }
        _state.update { it.copy(aspectMode = next) }
        applyAspectMode(attachedView, next)
    }

    fun selectAudioTrack(trackId: String) {
        val exo = player ?: return
        val group = exo.currentTracks.groups.firstOrNull { g ->
            g.type == C.TRACK_TYPE_AUDIO && g.mediaTrackGroup.id == trackId
        } ?: return
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
            .build()
    }

    fun selectSubtitleTrack(track: PlayerTrackInfo?) {
        val exo = player ?: return
        if (track == null) {
            subtitlesDisabledByPref = true
            selectedExternalSubUrl = ""
            exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            extractTracks(exo.currentTracks)
            return
        }
        subtitlesDisabledByPref = false
        if (track.isExternal && track.externalUrl.isNotBlank()) {
            selectedExternalSubUrl = track.externalUrl
            val currentPos = exo.currentPosition.coerceAtLeast(0L)
            exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .build()
            setMediaItem(exo, currentStreamUrl, currentPos)
            return
        }
        val group = exo.currentTracks.groups.firstOrNull { g ->
            g.type == C.TRACK_TYPE_TEXT && g.mediaTrackGroup.id == track.id
        } ?: return
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
            .build()
    }

    fun addLocalSubtitle(subtitleUri: Uri) {
        subtitlesDisabledByPref = false
        val newSub = StremioSubtitle(
            id = "local_${System.currentTimeMillis()}",
            url = subtitleUri.toString(),
            lang = "Local",
            subFileName = subtitleUri.lastPathSegment ?: "Local Subtitle"
        )
        externalSubtitles = listOf(newSub) + externalSubtitles
        selectedExternalSubUrl = newSub.url
        val exo = player ?: return
        val pos = exo.currentPosition.coerceAtLeast(0L)
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        setMediaItem(exo, currentStreamUrl, pos)
    }

    fun pollPosition() {
        val exo = player ?: return
        val pos = exo.currentPosition.coerceAtLeast(0L)
        val buf = exo.bufferedPosition.coerceAtLeast(pos)
        val dur = exo.duration.coerceAtLeast(0L)
        val playing = exo.isPlaying
        val bitrateEst = bandwidthMeter?.bitrateEstimate?.takeIf { it > 0L } ?: 0L
        val mediaBitrate = ((exo.videoFormat?.bitrate?.takeIf { it > 0 } ?: 0) +
            (exo.audioFormat?.bitrate?.takeIf { it > 0 } ?: 0)).toLong()
        val effectiveBitrate = if (mediaBitrate > 0L) mediaBitrate else bitrateEst.coerceAtLeast(3_500_000L)
        val bufferedLeadSec = ((buf - pos).coerceAtLeast(0L)) / 1000.0
        val bufferedBytes = ((bufferedLeadSec * effectiveBitrate) / 8.0).toLong().coerceAtLeast(0L)
        val speedBytesPerSec = if (buf > pos || exo.isLoading) (bitrateEst / 8L).coerceAtLeast(0L) else 0L
        val prev = _state.value
        if (prev.positionMs != pos ||
            prev.bufferedPositionMs != buf ||
            prev.durationMs != dur ||
            prev.isPlaying != playing ||
            prev.bufferedBytesEstimate != bufferedBytes ||
            prev.networkSpeedBytesPerSec != speedBytesPerSec
        ) {
            _state.update {
                it.copy(
                    positionMs = pos,
                    bufferedPositionMs = buf,
                    durationMs = dur,
                    isPlaying = playing,
                    bufferedBytesEstimate = bufferedBytes,
                    networkSpeedBytesPerSec = speedBytesPerSec
                )
            }
        }
    }

    fun currentPositionMs(): Long = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
    fun currentDurationMs(): Long = player?.duration?.coerceAtLeast(0L) ?: 0L

    fun release() {
        attachedView?.player = null
        player?.release()
        player = null
        bandwidthMeter = null
    }

    private fun extractTracks(tracks: Tracks) {
        val audio = mutableListOf<PlayerTrackInfo>()
        val subs = mutableListOf<PlayerTrackInfo>()
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO && group.length > 0) {
                val fmt = group.getTrackFormat(0)
                val langLabel = listOfNotNull(
                    fmt.label?.takeIf { it.isNotBlank() },
                    fmt.language?.uppercase()?.takeIf { it.isNotBlank() }
                ).joinToString(" • ").ifBlank { "Audio ${audio.size + 1}" }
                audio.add(
                    PlayerTrackInfo(
                        id = group.mediaTrackGroup.id,
                        label = langLabel,
                        language = fmt.language ?: "und",
                        isSelected = group.isSelected
                    )
                )
            } else if (group.type == C.TRACK_TYPE_TEXT && group.length > 0) {
                val fmt = group.getTrackFormat(0)
                subs.add(
                    PlayerTrackInfo(
                        id = group.mediaTrackGroup.id,
                        label = fmt.label ?: fmt.language?.uppercase() ?: "Subtitle ${subs.size + 1}",
                        language = fmt.language ?: "und",
                        isSelected = !subtitlesDisabledByPref && group.isSelected
                    )
                )
            }
        }

        val exo = player
        if (exo != null && preferredAudioLanguage.isNotBlank() && !preferredAudioLanguage.equals("auto", ignoreCase = true)) {
            val hasMatchingSelectedAudio = audio.any { it.isSelected && LanguageCodeMatcher.matches(it.language, preferredAudioLanguage) }
            if (!hasMatchingSelectedAudio) {
                val matchingGroup = tracks.groups.firstOrNull { g ->
                    if (g.type == C.TRACK_TYPE_AUDIO && g.length > 0) {
                        val fmt = g.getTrackFormat(0)
                        LanguageCodeMatcher.matches(fmt.language, preferredAudioLanguage) ||
                            LanguageCodeMatcher.matches(fmt.label, preferredAudioLanguage)
                    } else false
                }
                if (matchingGroup != null && !matchingGroup.isSelected) {
                    exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                        .setOverrideForType(TrackSelectionOverride(matchingGroup.mediaTrackGroup, 0))
                        .build()
                }
            }
        }
        externalSubtitles.forEach { ext ->
            if (subs.none { it.externalUrl == ext.url }) {
                subs.add(
                    PlayerTrackInfo(
                        id = "ext_${ext.id}",
                        label = "${ext.displayLanguage()} (${ext.subFileName ?: "External"})",
                        language = ext.lang,
                        isSelected = !subtitlesDisabledByPref && ext.url == selectedExternalSubUrl,
                        isExternal = true,
                        externalUrl = ext.url
                    )
                )
            }
        }
        _state.update { it.copy(audioTracks = audio, subtitleTracks = subs) }
    }

    private fun guessSubtitleMime(url: String, fileName: String?): String {
        val target = (fileName ?: url).lowercase()
        return when {
            target.contains(".vtt") -> MimeTypes.TEXT_VTT
            target.contains(".ass") || target.contains(".ssa") -> MimeTypes.TEXT_SSA
            else -> MimeTypes.APPLICATION_SUBRIP
        }
    }
}
