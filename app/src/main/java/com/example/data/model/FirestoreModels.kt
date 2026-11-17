package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class SyncedAddon(
    @DocumentId val id: String = "",
    val userId: String = "",
    val addonKey: String = "",
    val manifestUrl: String = "",
    val name: String = "",
    val version: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val types: List<String> = emptyList(),
    val resources: List<String> = emptyList(),
    val enabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LibraryMediaItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mediaKey: String = "",
    val title: String = "",
    val mediaType: String = "movie", // movie | series | cloud_video | local_video
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val overview: String = "",
    val year: String = "",
    val imdbRating: String = "",
    val streamUrl: String = "",
    val sourceLabel: String = "Cinemeta",
    val isFavorite: Boolean = true,
    val watchProgressMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastWatchedAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserPreferences(
    @DocumentId val id: String = "",
    val userId: String = "",
    val preferredEngine: String = "EXOPLAYER", // EXOPLAYER | LIBMPV
    val hardwareDecoding: Boolean = true,
    val subtitleFontSizeSp: Int = 18,
    val subtitleColorHex: String = "#FFFFFF",
    val subtitleBgOpacity: Double = 0.65,
    val subtitleDelayMs: Int = 0,
    val preferredSubtitleLang: String = "en",
    val preferredAudioLang: String = "en",
    val accentPreset: String = "TUNGSTEN_GOLD",
    val compactGrid: Boolean = false,
    val amoledBlack: Boolean = false,
    val autoPlayTrailers: String = "WIFI_ONLY", // OFF | WIFI_ONLY | ALWAYS
    val autoFetchLocalMetadata: Boolean = true,
    val downloadWifiOnly: Boolean = true,
    val ambientBackdropGlow: Boolean = true,
    val autoMatchBestStream: Boolean = false,
    val autoSkipIntro: Boolean = true,
    val autoPlayNextEpisode: Boolean = true,
    val codecCompatibilityFallback: Boolean = true,
    val defaultDownloadQuality: String = "1080p", // 1080p | 720p | ask
    val simultaneousDownloadsLimit: Int = 2,
    val embeddedSubtitlePriority: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class PlaybackEngine(val key: String, val displayName: String) {
    EXOPLAYER("EXOPLAYER", "Media3 ExoPlayer"),
    LIBMPV("LIBMPV", "libmpv (MPV)");

    companion object {
        fun fromKey(key: String?): PlaybackEngine =
            entries.firstOrNull { it.key == key } ?: EXOPLAYER
    }
}

enum class TrailerAutoPlayMode(val key: String, val label: String) {
    OFF("OFF", "Off"),
    WIFI_ONLY("WIFI_ONLY", "Wi-Fi Only"),
    ALWAYS("ALWAYS", "Always");

    companion object {
        fun fromKey(key: String?): TrailerAutoPlayMode =
            entries.firstOrNull { it.key == key } ?: WIFI_ONLY
    }
}
