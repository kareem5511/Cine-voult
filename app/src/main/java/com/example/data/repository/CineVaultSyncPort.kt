package com.example.data.repository

import com.example.data.model.LibraryMediaItem
import com.example.data.model.SyncedAddon
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface CineVaultSyncPort {
    fun observeAddons(): Flow<List<SyncedAddon>>
    suspend fun upsertAddon(addon: SyncedAddon): Result<String>
    suspend fun toggleAddonEnabled(addonId: String, enabled: Boolean): Result<Unit>
    suspend fun removeAddon(addonId: String): Result<Unit>

    fun observeLibrary(): Flow<List<LibraryMediaItem>>
    suspend fun upsertLibraryItem(item: LibraryMediaItem): Result<String>
    suspend fun getLibraryItem(mediaKey: String): Result<LibraryMediaItem?>
    suspend fun deleteLibraryItem(mediaKey: String): Result<Unit>

    fun observePreferences(): Flow<UserPreferences>
    suspend fun savePreferences(prefs: UserPreferences): Result<Unit>
}
