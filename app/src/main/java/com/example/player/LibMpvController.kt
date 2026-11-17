package com.example.player

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.data.model.PlaybackEngine
import com.example.data.model.StremioSubtitle
import com.example.data.model.UserPreferences
import dev.jdtech.mpv.MPVLib
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

class LibMpvController(private val context: Context) : MPVLib.EventObserver {

    private val _state = MutableStateFlow(ActivePlaybackState(engine = PlaybackEngine.LIBMPV))
    val state: StateFlow<ActivePlaybackState> = _state.asStateFlow()

    private var initialized = false
    private var surfaceAttached = false
    private var pendingUrl: String = ""
    private var pendingSeekMs: Long = 0L
    private var externalSubtitles: List<StremioSubtitle> = emptyList()
    private var attachedSubUrl: String = ""
    private var subtitlesDisabledByPref: Boolean = false
    private var preferredSubtitleLang: String = "en"
    private var currentPreferredAudioLang: String = ""

    fun initialize(
        streamUrl: String,
        initialPositionMs: Long,
        hardwareDecoding: Boolean,
        preferences: UserPreferences,
        subtitles: List<StremioSubtitle>,
        localSubtitleUri: String
    ) {
        pendingUrl = streamUrl
        pendingSeekMs = initialPositionMs
        preferredSubtitleLang = preferences.preferredSubtitleLang
        currentPreferredAudioLang = preferences.preferredAudioLang
        subtitlesDisabledByPref = preferredSubtitleLang.equals("off", ignoreCase = true)
        externalSubtitles = buildList {
            if (localSubtitleUri.isNotBlank()) {
                add(StremioSubtitle(id = "local_sub", url = localSubtitleUri, lang = "Local", subFileName = "Local Subtitle"))
            }
            addAll(subtitles)
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = "libmpv requires Android 8.0 (API 26)+. Switch to ExoPlayer on this device."
                )
            }
            return
        }

        val alang = LanguageCodeMatcher.expandForMpv(preferences.preferredAudioLang)
        val slang = LanguageCodeMatcher.expandForMpv(preferences.preferredSubtitleLang)

        val initResult = runCatching {
            if (!initialized) {
                val cpuThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 6).toString()
                MPVLib.create(context.applicationContext)
                MPVLib.setOptionString("config", "no")
                MPVLib.setOptionString("vo", "gpu")
                MPVLib.setOptionString("gpu-context", "android")
                MPVLib.setOptionString("opengl-es", "yes")
                MPVLib.setOptionString("hwdec", if (hardwareDecoding) "mediacodec,mediacodec-copy" else "no")
                MPVLib.setOptionString("vd-lavc-threads", cpuThreads)
                MPVLib.setOptionString("vd-lavc-dr", "yes")
                MPVLib.setOptionString("demuxer-max-bytes", "67108864")
                MPVLib.setOptionString("demuxer-readahead-secs", "20")
                MPVLib.setOptionString("audio-pitch-correction", "yes")
                MPVLib.setOptionString("ao", "audiotrack,opensles")
                MPVLib.setOptionString("keep-open", "yes")
                MPVLib.setOptionString("ytdl", "no")
                val shaderCache = java.io.File(context.cacheDir, "mpv_shaders").apply { mkdirs() }
                MPVLib.setOptionString("gpu-shader-cache-dir", shaderCache.absolutePath)
                if (alang.isNotBlank()) MPVLib.setOptionString("alang", alang)
                if (subtitlesDisabledByPref) {
                    MPVLib.setOptionString("sid", "no")
                } else if (slang.isNotBlank()) {
                    MPVLib.setOptionString("slang", slang)
                }
                applySubtitleOptions(preferences)
                MPVLib.init()
                MPVLib.addObserver(this)
                MPVLib.observeProperty("time-pos", MPV_FORMAT_INT64)
                MPVLib.observeProperty("demuxer-cache-time", MPV_FORMAT_INT64)
                MPVLib.observeProperty("duration", MPV_FORMAT_INT64)
                MPVLib.observeProperty("pause", MPV_FORMAT_FLAG)
                MPVLib.observeProperty("paused-for-cache", MPV_FORMAT_FLAG)
                MPVLib.observeProperty("eof-reached", MPV_FORMAT_FLAG)
                MPVLib.observeProperty("track-list/count", MPV_FORMAT_INT64)
                MPVLib.observeProperty("cache-speed", MPV_FORMAT_INT64)
                initialized = true
            } else {
                MPVLib.setPropertyString("hwdec", if (hardwareDecoding) "mediacodec,mediacodec-copy" else "no")
                if (alang.isNotBlank()) MPVLib.setPropertyString("alang", alang)
                if (subtitlesDisabledByPref) {
                    MPVLib.setPropertyString("sid", "no")
                } else if (slang.isNotBlank()) {
                    MPVLib.setPropertyString("slang", slang)
                }
                applySubtitleOptions(preferences)
            }
        }

        if (initResult.isFailure) {
            val err = initResult.exceptionOrNull()
            Log.e(TAG, "libmpv initialization failed", err)
            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = "libmpv native engine unavailable on this ABI (${err?.message ?: "load error"}). Switch to ExoPlayer."
                )
            }
            return
        }

        val matchingSub = if (subtitlesDisabledByPref) {
            null
        } else {
            externalSubtitles.firstOrNull { LanguageCodeMatcher.matches(it.lang, preferredSubtitleLang) }
                ?: externalSubtitles.firstOrNull()
        }

        _state.update {
            it.copy(
                engine = PlaybackEngine.LIBMPV,
                hardwareDecodingActive = hardwareDecoding,
                activeSubtitleDelayMs = preferences.subtitleDelayMs,
                isEnded = false,
                subtitleTracks = externalSubtitles.mapIndexed { idx, sub ->
                    PlayerTrackInfo(
                        id = "mpv_ext_$idx",
                        label = "${sub.displayLanguage()} (${sub.subFileName ?: "External"})",
                        language = sub.lang,
                        isSelected = matchingSub?.url == sub.url,
                        isExternal = true,
                        externalUrl = sub.url
                    )
                },
                errorMessage = null
            )
        }

        if (surfaceAttached) {
            loadStream()
        }
    }

    fun createSurfaceCallback(): SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            runCatching {
                if (initialized) {
                    MPVLib.attachSurface(holder.surface)
                    surfaceAttached = true
                    if (pendingUrl.isNotBlank()) {
                        loadStream()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "Failed attaching surface to libmpv", err)
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            if (initialized && surfaceAttached) {
                runCatching {
                    MPVLib.setPropertyString("android-surface-size", "${width}x${height}")
                }
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            surfaceAttached = false
            if (initialized) {
                runCatching { MPVLib.detachSurface() }
            }
        }
    }

    fun attachSurfaceView(surfaceView: SurfaceView) {
        surfaceView.holder.addCallback(createSurfaceCallback())
    }

    private fun loadStream() {
        if (!initialized || pendingUrl.isBlank()) return
        val urlToLoad = pendingUrl
        pendingUrl = ""
        runCatching {
            MPVLib.command(arrayOf("loadfile", urlToLoad, "replace"))
            if (pendingSeekMs > 0L) {
                val sec = pendingSeekMs / 1000.0
                MPVLib.command(arrayOf("seek", sec.toString(), "absolute"))
                pendingSeekMs = 0L
            }
            MPVLib.setPropertyBoolean("pause", false)
            if (!subtitlesDisabledByPref) {
                val chosen = externalSubtitles.firstOrNull {
                    LanguageCodeMatcher.matches(it.lang, preferredSubtitleLang)
                } ?: externalSubtitles.firstOrNull()
                chosen?.let { firstSub ->
                    if (firstSub.url.isNotBlank()) {
                        attachedSubUrl = firstSub.url
                        MPVLib.command(arrayOf("sub-add", firstSub.url, "select", firstSub.displayLanguage(), firstSub.lang))
                    }
                }
            }
        }.onFailure { err ->
            Log.e(TAG, "libmpv loadfile error", err)
            _state.update { it.copy(errorMessage = err.localizedMessage ?: "libmpv failed to open stream", isBuffering = false) }
        }
    }

    fun applySubtitleOptions(preferences: UserPreferences) {
        if (!initialized) return
        runCatching {
            MPVLib.setOptionString("sub-font-size", (preferences.subtitleFontSizeSp * 2).toString())
            MPVLib.setOptionString("sub-color", preferences.subtitleColorHex)
            val bgAlphaHex = (preferences.subtitleBgOpacity * 255).roundToInt().coerceIn(0, 255)
            val alphaStr = bgAlphaHex.toString(16).padStart(2, '0')
            MPVLib.setOptionString("sub-back-color", "#${alphaStr}000000")
            MPVLib.setPropertyDouble("sub-delay", preferences.subtitleDelayMs / 1000.0)
        }
    }

    fun setSubtitleBottomPadding(controlsVisible: Boolean) {
        if (!initialized) return
        runCatching {
            MPVLib.setPropertyInt("sub-pos", if (controlsVisible) 82 else 95)
        }
    }

    fun setSubtitleDelayMs(delayMs: Int) {
        if (!initialized) return
        runCatching {
            MPVLib.setPropertyDouble("sub-delay", delayMs / 1000.0)
            _state.update { it.copy(activeSubtitleDelayMs = delayMs) }
        }
    }

    fun selectAudioTrack(trackId: String) {
        if (!initialized) return
        runCatching {
            MPVLib.setPropertyString("aid", trackId)
            _state.update { st ->
                st.copy(
                    audioTracks = st.audioTracks.map {
                        it.copy(isSelected = it.id == trackId)
                    }
                )
            }
        }
    }

    fun selectSubtitleTrack(track: PlayerTrackInfo?) {
        if (!initialized) return
        runCatching {
            if (track == null) {
                MPVLib.setPropertyString("sid", "no")
                _state.update { st ->
                    st.copy(subtitleTracks = st.subtitleTracks.map { it.copy(isSelected = false) })
                }
                return
            }
            if (track.isExternal && track.externalUrl.isNotBlank()) {
                attachedSubUrl = track.externalUrl
                MPVLib.command(arrayOf("sub-add", track.externalUrl, "select", track.label, track.language))
            } else {
                MPVLib.setPropertyString("sid", track.id)
            }
            _state.update { st ->
                st.copy(
                    subtitleTracks = st.subtitleTracks.map {
                        it.copy(isSelected = it.id == track.id)
                    }
                )
            }
        }
    }

    fun addLocalSubtitle(subtitleUri: Uri) {
        val sub = StremioSubtitle(
            id = "local_${System.currentTimeMillis()}",
            url = subtitleUri.toString(),
            lang = "Local",
            subFileName = subtitleUri.lastPathSegment ?: "Local Subtitle"
        )
        externalSubtitles = listOf(sub) + externalSubtitles
        if (initialized) {
            runCatching {
                MPVLib.command(arrayOf("sub-add", sub.url, "select", "Local", "und"))
            }
        }
    }

    fun togglePlayPause() {
        if (!initialized) return
        runCatching {
            val currentlyPlaying = _state.value.isPlaying
            MPVLib.setPropertyBoolean("pause", currentlyPlaying)
        }
    }

    fun seekTo(positionMs: Long) {
        if (!initialized) return
        runCatching {
            val seconds = (positionMs.coerceAtLeast(0L)) / 1000.0
            MPVLib.command(arrayOf("seek", seconds.toString(), "absolute"))
            _state.update { it.copy(isEnded = false) }
        }
    }

    fun seekBy(deltaMs: Long) {
        if (!initialized) return
        runCatching {
            val seconds = deltaMs / 1000.0
            MPVLib.command(arrayOf("seek", seconds.toString(), "relative"))
            _state.update { it.copy(isEnded = false) }
        }
    }

    fun setSpeed(speed: Float) {
        if (!initialized) return
        runCatching {
            MPVLib.setPropertyDouble("speed", speed.toDouble())
            _state.update { it.copy(playbackSpeed = speed) }
        }
    }

    fun cycleAspectMode() {
        if (!initialized) return
        val next = when (_state.value.aspectMode) {
            AspectMode.FIT -> AspectMode.ZOOM
            AspectMode.ZOOM -> AspectMode.STRETCH
            AspectMode.STRETCH -> AspectMode.FIT
        }
        runCatching {
            when (next) {
                AspectMode.FIT -> {
                    MPVLib.setPropertyDouble("panscan", 0.0)
                    MPVLib.setPropertyString("keepaspect", "yes")
                }
                AspectMode.ZOOM -> {
                    MPVLib.setPropertyString("keepaspect", "yes")
                    MPVLib.setPropertyDouble("panscan", 1.0)
                }
                AspectMode.STRETCH -> {
                    MPVLib.setPropertyDouble("panscan", 0.0)
                    MPVLib.setPropertyString("keepaspect", "no")
                }
            }
            _state.update { it.copy(aspectMode = next) }
        }
    }

    fun release() {
        if (!initialized) return
        runCatching {
            MPVLib.removeObserver(this)
            if (surfaceAttached) {
                MPVLib.detachSurface()
                surfaceAttached = false
            }
            MPVLib.destroy()
        }
        initialized = false
    }

    private fun refreshTrackList(count: Int) {
        if (!initialized || count <= 0) return
        runCatching {
            val audio = mutableListOf<PlayerTrackInfo>()
            val embeddedSubs = mutableListOf<PlayerTrackInfo>()
            for (i in 0 until count) {
                val type = MPVLib.getPropertyString("track-list/$i/type") ?: continue
                val id = MPVLib.getPropertyInt("track-list/$i/id")?.toString() ?: continue
                val lang = MPVLib.getPropertyString("track-list/$i/lang") ?: "und"
                val title = MPVLib.getPropertyString("track-list/$i/title")
                val selected = MPVLib.getPropertyBoolean("track-list/$i/selected") == true
                val label = listOfNotNull(
                    title?.takeIf { it.isNotBlank() },
                    lang.takeIf { it.isNotBlank() && it != "und" }?.uppercase()
                ).joinToString(" • ").ifBlank { "${type.uppercase()} #$id" }

                when (type) {
                    "audio" -> audio.add(
                        PlayerTrackInfo(id = id, label = label, language = lang, isSelected = selected)
                    )
                    "sub" -> embeddedSubs.add(
                        PlayerTrackInfo(id = id, label = label, language = lang, isSelected = selected)
                    )
                }
            }

            val prefLang = currentPreferredAudioLang
            if (prefLang.isNotBlank() && !prefLang.equals("auto", ignoreCase = true)) {
                val hasMatchingSelectedAudio = audio.any { it.isSelected && LanguageCodeMatcher.matches(it.language, prefLang) }
                if (!hasMatchingSelectedAudio) {
                    val candidate = audio.firstOrNull {
                        LanguageCodeMatcher.matches(it.language, prefLang) ||
                            LanguageCodeMatcher.matches(it.label, prefLang)
                    }
                    if (candidate != null && !candidate.isSelected) {
                        MPVLib.setPropertyString("aid", candidate.id)
                        audio.forEachIndexed { idx, tr ->
                            audio[idx] = tr.copy(isSelected = tr.id == candidate.id)
                        }
                    }
                }
            }

            val mergedSubs = buildList {
                addAll(embeddedSubs)
                externalSubtitles.forEachIndexed { idx, ext ->
                    if (none { it.externalUrl == ext.url }) {
                        add(
                            PlayerTrackInfo(
                                id = "mpv_ext_$idx",
                                label = "${ext.displayLanguage()} (${ext.subFileName ?: "External"})",
                                language = ext.lang,
                                isSelected = !subtitlesDisabledByPref && attachedSubUrl == ext.url,
                                isExternal = true,
                                externalUrl = ext.url
                            )
                        )
                    }
                }
            }
            _state.update { it.copy(audioTracks = audio, subtitleTracks = mergedSubs) }
        }
    }

    // ---- MPVLib.EventObserver ----

    override fun eventProperty(property: String) {}

    override fun eventProperty(property: String, value: Long) {
        when (property) {
            "time-pos" -> _state.update { it.copy(positionMs = (value * 1000L).coerceAtLeast(0L), isBuffering = false) }
            "demuxer-cache-time" -> _state.update {
                val buf = (value * 1000L).coerceAtLeast(it.positionMs)
                val fwBytes = runCatching { MPVLib.getPropertyInt("demuxer-cache-state/fw-bytes")?.toLong() }.getOrNull()
                    ?: (((buf - it.positionMs).coerceAtLeast(0L) / 1000L) * 650_000L)
                it.copy(bufferedPositionMs = buf, bufferedBytesEstimate = fwBytes.coerceAtLeast(0L))
            }
            "cache-speed" -> _state.update {
                it.copy(networkSpeedBytesPerSec = value.coerceAtLeast(0L))
            }
            "duration" -> _state.update { it.copy(durationMs = (value * 1000L).coerceAtLeast(0L)) }
            "track-list/count" -> refreshTrackList(value.toInt())
        }
    }

    override fun eventProperty(property: String, value: Boolean) {
        when (property) {
            "pause" -> _state.update { it.copy(isPlaying = !value, isBuffering = false) }
            "paused-for-cache" -> _state.update { it.copy(isBuffering = value) }
            "eof-reached" -> if (value) _state.update { it.copy(isEnded = true, isPlaying = false) }
        }
    }

    override fun eventProperty(property: String, value: String) {}

    override fun eventProperty(property: String, value: Double) {
        when (property) {
            "time-pos" -> _state.update { it.copy(positionMs = (value * 1000.0).toLong().coerceAtLeast(0L), isBuffering = false) }
            "demuxer-cache-time" -> _state.update {
                val buf = (value * 1000.0).toLong().coerceAtLeast(it.positionMs)
                it.copy(bufferedPositionMs = buf)
            }
            "duration" -> _state.update { it.copy(durationMs = (value * 1000.0).toLong().coerceAtLeast(0L)) }
        }
    }

    override fun event(eventId: Int) {}

    private companion object {
        const val TAG = "LibMpvController"
        const val MPV_FORMAT_FLAG = 3
        const val MPV_FORMAT_INT64 = 4
    }
}
