package com.example.player

import com.example.data.model.PlaybackEngine
import kotlinx.serialization.Serializable

@Serializable
data class StreamFingerprint(
    val addonName: String = "",
    val addonId: String = "",
    val resolution: String = "",
    val tags: List<String> = emptyList()
)

@Serializable
data class PlayerEpisodeItem(
    val id: String,
    val title: String,
    val season: Int? = null,
    val episode: Int? = null,
    val thumbnail: String = "",
    val runtime: String = ""
)

@Serializable
data class StreamSourceOption(
    val id: String,
    val fullFileName: String,
    val badges: List<String> = emptyList(),
    val streamUrl: String,
    val addonName: String = "",
    val addonId: String = "",
    val resolution: String = "",
    val tags: List<String> = emptyList()
)

@Serializable
data class UpNextInfo(
    val mediaKey: String,
    val title: String,
    val subtitle: String = "",
    val mediaType: String = "series",
    val streamUrl: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val imdbId: String = "",
    val season: Int? = null,
    val episode: Int? = null
)

@Serializable
data class PlaybackRequest(
    val mediaKey: String,
    val title: String,
    val mediaType: String = "movie",
    val streamUrl: String,
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val imdbId: String = "",
    val initialPositionMs: Long = 0L,
    val preferredEngine: String = PlaybackEngine.NEXTPLAYER.key,
    val localSubtitleUri: String = "",
    val availableSources: List<StreamSourceOption> = emptyList(),
    val upNext: UpNextInfo? = null,
    val seriesId: String = "",
    val currentSeason: Int? = null,
    val currentEpisode: Int? = null,
    val episodes: List<PlayerEpisodeItem> = emptyList(),
    val streamPreference: StreamFingerprint? = null
)

data class PlayerTrackInfo(
    val id: String,
    val label: String,
    val language: String,
    val isSelected: Boolean,
    val isExternal: Boolean = false,
    val externalUrl: String = ""
)

enum class AspectMode(val label: String) {
    FIT("Fit"),
    ZOOM("Fill / Crop"),
    STRETCH("Stretch")
}

data class ActivePlaybackState(
    val engine: PlaybackEngine = PlaybackEngine.NEXTPLAYER,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val isEnded: Boolean = false,
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedBytesEstimate: Long = 0L,
    val networkSpeedBytesPerSec: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val aspectMode: AspectMode = AspectMode.FIT,
    val audioTracks: List<PlayerTrackInfo> = emptyList(),
    val subtitleTracks: List<PlayerTrackInfo> = emptyList(),
    val activeSubtitleDelayMs: Int = 0,
    val errorMessage: String? = null,
    val hardwareDecodingActive: Boolean = true,
    val volumeBoost: Float = 1.0f,
    val nightModeActive: Boolean = false
)

val LocalPipMode = androidx.compose.runtime.staticCompositionLocalOf { false }

object LanguageCodeMatcher {
    private val aliases: Map<String, Set<String>> = mapOf(
        "en" to setOf("en", "eng", "english"),
        "hi" to setOf("hi", "hin", "hindi"),
        "es" to setOf("es", "spa", "spanish", "espanol"),
        "fr" to setOf("fr", "fre", "fra", "french", "francais"),
        "de" to setOf("de", "ger", "deu", "german", "deutsch"),
        "ja" to setOf("ja", "jpn", "japanese"),
        "ko" to setOf("ko", "kor", "korean"),
        "ar" to setOf("ar", "ara", "arabic"),
        "pt" to setOf("pt", "por", "pob", "portuguese", "portugues"),
        "it" to setOf("it", "ita", "italian", "italiano"),
        "zh" to setOf("zh", "chi", "zho", "chinese"),
        "ru" to setOf("ru", "rus", "russian"),
        "tr" to setOf("tr", "tur", "turkish")
    )

    fun getCodes(preferred: String): Set<String> {
        val pref = preferred.trim().lowercase()
        if (pref.isBlank() || pref == "auto" || pref == "off") return emptySet()
        val direct = aliases[pref]
        if (direct != null) return direct
        // Search if preferred is an alias value
        for ((_, set) in aliases) {
            if (pref in set) return set
        }
        return setOf(pref)
    }

    fun matches(candidate: String?, preferred: String): Boolean {
        val pref = preferred.trim().lowercase()
        if (pref.isBlank() || pref == "auto" || pref == "off") return false
        val cand = candidate?.trim()?.lowercase().orEmpty()
        if (cand.isBlank()) return false
        val codes = getCodes(pref)
        if (cand in codes) return true
        if (codes.any { cand.startsWith("$it-") || cand.startsWith("${it}_") }) return true
        return codes.any { cand.contains(it) }
    }

    fun expandForMpv(preferred: String): String {
        val pref = preferred.trim().lowercase()
        if (pref.isBlank() || pref == "auto" || pref == "off") return ""
        val codes = getCodes(pref)
        return if (codes.isNotEmpty()) codes.joinToString(",") else pref
    }
}

