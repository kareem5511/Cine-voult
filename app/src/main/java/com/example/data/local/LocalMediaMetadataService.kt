package com.example.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.StremioMetaPreview
import com.example.data.stremio.StremioAddonClient
import com.example.data.stremio.StremioUrlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMediaMetadataService(
    private val context: Context,
    private val stremioClient: StremioAddonClient = StremioAddonClient()
) {
    private val db = CineVaultDatabase.getInstance(context)

    suspend fun importLocalVideo(
        uri: Uri,
        autoFetchMetadata: Boolean,
        metadataAddonManifestUrls: List<String> = listOf(StremioUrlHelper.DEFAULT_CINEMETA_MANIFEST)
    ): LocalMediaEntity = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val probe = LocalFilenameParser.probeUri(context, uri)
        val parsed = LocalFilenameParser.parse(probe.displayName)

        var entity = LocalMediaEntity(
            uriString = uri.toString(),
            displayName = probe.displayName,
            parsedTitle = parsed.cleanTitle,
            parsedYear = parsed.year,
            parsedSeason = parsed.season,
            parsedEpisode = parsed.episode,
            qualityTags = parsed.qualityTags.joinToString(" • "),
            durationMs = probe.durationMs,
            fileSizeBytes = probe.fileSizeBytes,
            resolution = probe.resolution,
            mimeType = probe.mimeType,
            metadataStatus = if (autoFetchMetadata) {
                MetadataMatchStatus.PENDING.name
            } else {
                MetadataMatchStatus.UNMATCHED.name
            }
        )
        db.localMediaDao().upsert(entity)

        if (autoFetchMetadata) {
            entity = enrichMetadata(entity, parsed.isSeries, metadataAddonManifestUrls)
            db.localMediaDao().upsert(entity)
        }
        entity
    }

    suspend fun refreshMetadata(
        uriString: String,
        metadataAddonManifestUrls: List<String> = listOf(StremioUrlHelper.DEFAULT_CINEMETA_MANIFEST)
    ): LocalMediaEntity? = withContext(Dispatchers.IO) {
        val existing = db.localMediaDao().getByUri(uriString) ?: return@withContext null
        val isSeries = existing.parsedSeason != null && existing.parsedEpisode != null
        val updated = enrichMetadata(existing, isSeries, metadataAddonManifestUrls)
        db.localMediaDao().upsert(updated)
        updated
    }

    suspend fun attachLocalSubtitle(uriString: String, subtitleUri: Uri) = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                subtitleUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        db.localMediaDao().getByUri(uriString)?.let { existing ->
            db.localMediaDao().upsert(existing.copy(localSubtitleUri = subtitleUri.toString()))
        }
    }

    suspend fun deleteLocalMedia(uriString: String) = withContext(Dispatchers.IO) {
        db.localMediaDao().deleteByUri(uriString)
    }

    private suspend fun enrichMetadata(
        entity: LocalMediaEntity,
        isSeries: Boolean,
        addonUrls: List<String>
    ): LocalMediaEntity {
        val searchType = if (isSeries) "series" else "movie"
        val effectiveAddons = (addonUrls + StremioUrlHelper.DEFAULT_CINEMETA_MANIFEST).distinct()
        var bestMatch: StremioMetaPreview? = null

        for (manifestUrl in effectiveAddons) {
            val res = stremioClient.fetchCatalog(
                manifestUrl = manifestUrl,
                type = searchType,
                catalogId = "top",
                searchQuery = entity.parsedTitle
            )
            val candidates = res.getOrNull().orEmpty()
            if (candidates.isNotEmpty()) {
                bestMatch = if (entity.parsedYear.isNotBlank()) {
                    candidates.firstOrNull { it.releaseInfo?.contains(entity.parsedYear) == true }
                        ?: candidates.first()
                } else {
                    candidates.first()
                }
                break
            }
        }

        if (bestMatch == null) {
            return entity.copy(metadataStatus = MetadataMatchStatus.UNMATCHED.name)
        }

        val detail = stremioClient.fetchMeta(
            manifestUrl = StremioUrlHelper.DEFAULT_CINEMETA_MANIFEST,
            type = bestMatch.type,
            mediaId = bestMatch.id
        ).getOrNull()

        return entity.copy(
            imdbId = bestMatch.id,
            parsedTitle = detail?.name ?: bestMatch.name,
            parsedYear = (detail?.releaseInfo ?: bestMatch.releaseInfo ?: entity.parsedYear),
            posterUrl = detail?.poster ?: bestMatch.poster.orEmpty(),
            backdropUrl = detail?.background ?: bestMatch.background ?: bestMatch.poster.orEmpty(),
            overview = detail?.description ?: bestMatch.description.orEmpty(),
            imdbRating = detail?.imdbRating ?: bestMatch.imdbRating.orEmpty(),
            metadataStatus = MetadataMatchStatus.MATCHED.name
        )
    }
}
