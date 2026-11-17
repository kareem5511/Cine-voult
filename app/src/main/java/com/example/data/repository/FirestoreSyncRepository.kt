package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.LibraryMediaItem
import com.example.data.model.SyncedAddon
import com.example.data.model.UserPreferences
import com.example.data.stremio.StremioUrlHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository(private val db: FirebaseFirestore) : CineVaultSyncPort {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // ---- Addons Sync ----

    override fun observeAddons(): Flow<List<SyncedAddon>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/addons"
        emitAll(
            db.collection("users").document(uid).collection("addons")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SyncedAddon::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override suspend fun upsertAddon(addon: SyncedAddon): Result<String> {
        val uid = requireUserId()
        val docId = StremioUrlHelper.sanitizeDocId(addon.addonKey)
        val docRef = db.collection("users").document(uid).collection("addons").document(docId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val createdAt = existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
                val updateMap = mapOf(
                    "userId" to uid,
                    "addonKey" to docId,
                    "manifestUrl" to addon.manifestUrl.take(500),
                    "name" to addon.name.take(120).ifBlank { "Stremio Addon" },
                    "version" to addon.version.take(40),
                    "description" to addon.description.take(1000),
                    "types" to addon.types.take(12).map { it.take(64) },
                    "resources" to addon.resources.take(15).map { it.take(64) },
                    "enabled" to addon.enabled,
                    "createdAt" to createdAt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(updateMap).await()
            } else {
                val createMap = mapOf(
                    "userId" to uid,
                    "addonKey" to docId,
                    "manifestUrl" to addon.manifestUrl.take(500),
                    "name" to addon.name.take(120).ifBlank { "Stremio Addon" },
                    "version" to addon.version.take(40),
                    "description" to addon.description.take(1000),
                    "types" to addon.types.take(12).map { it.take(64) },
                    "resources" to addon.resources.take(15).map { it.take(64) },
                    "enabled" to addon.enabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createMap).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun toggleAddonEnabled(addonId: String, enabled: Boolean): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("addons").document(addonId)
        return try {
            docRef.update(
                mapOf(
                    "enabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun removeAddon(addonId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("addons").document(addonId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // ---- Cloud Library & Watch Progress Sync ----

    override fun observeLibrary(): Flow<List<LibraryMediaItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/library"
        emitAll(
            db.collection("users").document(uid).collection("library")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LibraryMediaItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.lastWatchedAtMs }
                }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override suspend fun upsertLibraryItem(item: LibraryMediaItem): Result<String> {
        val uid = requireUserId()
        val docId = StremioUrlHelper.sanitizeDocId(item.mediaKey)
        val docRef = db.collection("users").document(uid).collection("library").document(docId)
        return try {
            val existing = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "mediaKey" to docId,
                "title" to item.title.take(200).ifBlank { "Untitled" },
                "mediaType" to item.mediaType.take(32),
                "posterUrl" to item.posterUrl.take(600),
                "backdropUrl" to item.backdropUrl.take(600),
                "overview" to item.overview.take(2000),
                "year" to item.year.take(16),
                "imdbRating" to item.imdbRating.take(16),
                "streamUrl" to item.streamUrl.take(1000),
                "sourceLabel" to item.sourceLabel.take(120),
                "isFavorite" to item.isFavorite,
                "watchProgressMs" to item.watchProgressMs.coerceAtLeast(0L),
                "durationMs" to item.durationMs.coerceAtLeast(0L),
                "lastWatchedAtMs" to item.lastWatchedAtMs.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existing.exists()) {
                payload["createdAt"] = existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun getLibraryItem(mediaKey: String): Result<LibraryMediaItem?> {
        val uid = requireUserId()
        val docId = StremioUrlHelper.sanitizeDocId(mediaKey)
        val docRef = db.collection("users").document(uid).collection("library").document(docId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    LibraryMediaItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun deleteLibraryItem(mediaKey: String): Result<Unit> {
        val uid = requireUserId()
        val docId = StremioUrlHelper.sanitizeDocId(mediaKey)
        val docRef = db.collection("users").document(uid).collection("library").document(docId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // ---- Synced User Preferences ----

    override fun observePreferences(): Flow<UserPreferences> = flow {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("preferences").document(DEFAULT_PREF_DOC_ID)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserPreferences::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: UserPreferences(id = DEFAULT_PREF_DOC_ID, userId = uid)
                }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    override suspend fun savePreferences(prefs: UserPreferences): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("preferences").document(DEFAULT_PREF_DOC_ID)
        return try {
            val existing = docRef.get().await()
            val payload = mapOf(
                "userId" to uid,
                "preferredEngine" to prefs.preferredEngine,
                "hardwareDecoding" to prefs.hardwareDecoding,
                "subtitleFontSizeSp" to prefs.subtitleFontSizeSp.coerceIn(12, 42),
                "subtitleColorHex" to prefs.subtitleColorHex,
                "subtitleBgOpacity" to prefs.subtitleBgOpacity.coerceIn(0.0, 1.0),
                "subtitleDelayMs" to prefs.subtitleDelayMs.coerceIn(-10000, 10000),
                "preferredSubtitleLang" to prefs.preferredSubtitleLang.take(16).ifBlank { "en" },
                "preferredAudioLang" to prefs.preferredAudioLang.take(16).ifBlank { "en" },
                "accentPreset" to prefs.accentPreset,
                "compactGrid" to prefs.compactGrid,
                "autoFetchLocalMetadata" to prefs.autoFetchLocalMetadata,
                "downloadWifiOnly" to prefs.downloadWifiOnly,
                "createdAt" to (existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        const val DEFAULT_PREF_DOC_ID = "default"
    }
}
