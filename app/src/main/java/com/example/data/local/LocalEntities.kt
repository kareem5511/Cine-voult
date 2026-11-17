package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED
}

enum class MetadataMatchStatus {
    PENDING,
    MATCHED,
    UNMATCHED
}

@Entity(tableName = "offline_downloads")
data class OfflineDownloadEntity(
    @PrimaryKey val downloadKey: String,
    val mediaKey: String,
    val title: String,
    val mediaType: String,
    val posterUrl: String,
    val streamUrl: String,
    val subtitleUrl: String,
    val localFilePath: String,
    val localSubtitlePath: String,
    val qualityLabel: String,
    val status: String = DownloadStatus.QUEUED.name,
    val progressPercent: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_media_files")
data class LocalMediaEntity(
    @PrimaryKey val uriString: String,
    val displayName: String,
    val parsedTitle: String,
    val parsedYear: String,
    val parsedSeason: Int? = null,
    val parsedEpisode: Int? = null,
    val qualityTags: String = "",
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val resolution: String = "",
    val mimeType: String = "",
    val imdbId: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val overview: String = "",
    val imdbRating: String = "",
    val metadataStatus: String = MetadataMatchStatus.PENDING.name,
    val localSubtitleUri: String = "",
    val addedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_addons")
data class LocalAddonEntity(
    @PrimaryKey val addonKey: String,
    val manifestUrl: String,
    val name: String,
    val version: String,
    val description: String,
    val typesCsv: String,
    val resourcesCsv: String,
    val enabled: Boolean = true,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_library_items")
data class LocalLibraryEntity(
    @PrimaryKey val mediaKey: String,
    val title: String,
    val mediaType: String,
    val posterUrl: String,
    val backdropUrl: String,
    val overview: String,
    val year: String,
    val imdbRating: String,
    val streamUrl: String,
    val sourceLabel: String,
    val isFavorite: Boolean = true,
    val watchProgressMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastWatchedAtMs: Long = 0L
)
