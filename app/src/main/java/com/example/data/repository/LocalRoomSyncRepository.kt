package com.example.data.repository

import android.content.Context
import com.example.data.local.CineVaultDatabase
import com.example.data.local.LocalAddonEntity
import com.example.data.local.LocalLibraryEntity
import com.example.data.model.LibraryMediaItem
import com.example.data.model.SyncedAddon
import com.example.data.model.UserPreferences
import com.example.data.preferences.LocalPreferencesStore
import com.example.data.stremio.StremioUrlHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalRoomSyncRepository(context: Context) : CineVaultSyncPort {

    private val db = CineVaultDatabase.getInstance(context.applicationContext)
    private val addonDao = db.localAddonDao()
    private val libraryDao = db.localLibraryDao()
    private val prefsStore = LocalPreferencesStore(context.applicationContext)

    override fun observeAddons(): Flow<List<SyncedAddon>> =
        addonDao.observeAll().map { list ->
            list.map { entity ->
                SyncedAddon(
                    id = entity.addonKey,
                    userId = GUEST_USER_ID,
                    addonKey = entity.addonKey,
                    manifestUrl = entity.manifestUrl,
                    name = entity.name,
                    version = entity.version,
                    description = entity.description,
                    types = entity.typesCsv.split(',').filter { it.isNotBlank() },
                    resources = entity.resourcesCsv.split(',').filter { it.isNotBlank() },
                    enabled = entity.enabled
                )
            }
        }

    override suspend fun upsertAddon(addon: SyncedAddon): Result<String> = runCatching {
        val docId = StremioUrlHelper.sanitizeDocId(addon.addonKey)
        addonDao.upsert(
            LocalAddonEntity(
                addonKey = docId,
                manifestUrl = addon.manifestUrl,
                name = addon.name,
                version = addon.version,
                description = addon.description,
                typesCsv = addon.types.joinToString(","),
                resourcesCsv = addon.resources.joinToString(","),
                enabled = addon.enabled,
                updatedAtMs = System.currentTimeMillis()
            )
        )
        docId
    }

    override suspend fun toggleAddonEnabled(addonId: String, enabled: Boolean): Result<Unit> = runCatching {
        addonDao.setEnabled(addonId, enabled, System.currentTimeMillis())
    }

    override suspend fun removeAddon(addonId: String): Result<Unit> = runCatching {
        addonDao.deleteByKey(addonId)
    }

    override fun observeLibrary(): Flow<List<LibraryMediaItem>> =
        libraryDao.observeAll().map { list ->
            list.map { entity ->
                LibraryMediaItem(
                    id = entity.mediaKey,
                    userId = GUEST_USER_ID,
                    mediaKey = entity.mediaKey,
                    title = entity.title,
                    mediaType = entity.mediaType,
                    posterUrl = entity.posterUrl,
                    backdropUrl = entity.backdropUrl,
                    overview = entity.overview,
                    year = entity.year,
                    imdbRating = entity.imdbRating,
                    streamUrl = entity.streamUrl,
                    sourceLabel = entity.sourceLabel,
                    isFavorite = entity.isFavorite,
                    watchProgressMs = entity.watchProgressMs,
                    durationMs = entity.durationMs,
                    lastWatchedAtMs = entity.lastWatchedAtMs
                )
            }
        }

    override suspend fun upsertLibraryItem(item: LibraryMediaItem): Result<String> = runCatching {
        val docId = StremioUrlHelper.sanitizeDocId(item.mediaKey)
        libraryDao.upsert(
            LocalLibraryEntity(
                mediaKey = docId,
                title = item.title,
                mediaType = item.mediaType,
                posterUrl = item.posterUrl,
                backdropUrl = item.backdropUrl,
                overview = item.overview,
                year = item.year,
                imdbRating = item.imdbRating,
                streamUrl = item.streamUrl,
                sourceLabel = item.sourceLabel,
                isFavorite = item.isFavorite,
                watchProgressMs = item.watchProgressMs,
                durationMs = item.durationMs,
                lastWatchedAtMs = item.lastWatchedAtMs
            )
        )
        docId
    }

    override suspend fun getLibraryItem(mediaKey: String): Result<LibraryMediaItem?> = runCatching {
        val docId = StremioUrlHelper.sanitizeDocId(mediaKey)
        libraryDao.getByKey(docId)?.let { entity ->
            LibraryMediaItem(
                id = entity.mediaKey,
                userId = GUEST_USER_ID,
                mediaKey = entity.mediaKey,
                title = entity.title,
                mediaType = entity.mediaType,
                posterUrl = entity.posterUrl,
                backdropUrl = entity.backdropUrl,
                overview = entity.overview,
                year = entity.year,
                imdbRating = entity.imdbRating,
                streamUrl = entity.streamUrl,
                sourceLabel = entity.sourceLabel,
                isFavorite = entity.isFavorite,
                watchProgressMs = entity.watchProgressMs,
                durationMs = entity.durationMs,
                lastWatchedAtMs = entity.lastWatchedAtMs
            )
        }
    }

    override suspend fun deleteLibraryItem(mediaKey: String): Result<Unit> = runCatching {
        val docId = StremioUrlHelper.sanitizeDocId(mediaKey)
        libraryDao.deleteByKey(docId)
    }

    override fun observePreferences(): Flow<UserPreferences> =
        prefsStore.preferencesFlow.map { it.copy(userId = GUEST_USER_ID) }

    override suspend fun savePreferences(prefs: UserPreferences): Result<Unit> = runCatching {
        prefsStore.save(prefs.copy(userId = GUEST_USER_ID))
    }

    companion object {
        const val GUEST_USER_ID = "local_guest"
    }
}
