package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.LibraryMediaItem
import com.example.data.model.SyncedAddon
import com.example.data.model.UserPreferences
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addonCrud_authenticatedOwner_succeedsAndEmitsRealtime() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreSyncRepository(firestore)
        val uniqueKey = "addon_${UUID.randomUUID().toString().take(8)}"

        val addon = SyncedAddon(
            userId = uid,
            addonKey = uniqueKey,
            manifestUrl = "https://v3-cinemeta.strem.io/manifest.json",
            name = "Cinemeta Test",
            version = "3.0.1",
            description = "Test catalog addon",
            types = listOf("movie", "series"),
            resources = listOf("catalog", "meta"),
            enabled = true
        )

        val upsertRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.upsertAddon(addon) }
        assertTrue(upsertRes.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeAddons().first { list -> list.any { it.addonKey == uniqueKey } }
        }
        assertEquals("Cinemeta Test", observed.first { it.addonKey == uniqueKey }.name)

        val deleteRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.removeAddon(uniqueKey) }
        assertTrue(deleteRes.isSuccess)
    }

    @Test
    fun libraryAndPreferencesSync_authenticatedOwner_persistsAcrossReads() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreSyncRepository(firestore)
        val mediaKey = "cloud_${UUID.randomUUID().toString().take(8)}"

        val item = LibraryMediaItem(
            userId = uid,
            mediaKey = mediaKey,
            title = "Personal WebDAV Movie",
            mediaType = "cloud_video",
            streamUrl = "https://example.com/movies/sample.mp4",
            sourceLabel = "Nextcloud",
            isFavorite = true,
            watchProgressMs = 45_000L,
            durationMs = 180_000L,
            lastWatchedAtMs = System.currentTimeMillis()
        )

        val itemResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.upsertLibraryItem(item) }
        assertTrue(itemResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getLibraryItem(mediaKey) }
        assertEquals("Personal WebDAV Movie", fetched.getOrNull()?.title)
        assertEquals(45_000L, fetched.getOrNull()?.watchProgressMs)

        val prefs = UserPreferences(
            userId = uid,
            preferredEngine = "NEXTPLAYER",
            hardwareDecoding = true,
            subtitleFontSizeSp = 22,
            subtitleColorHex = "#F5B74F",
            subtitleBgOpacity = 0.75,
            subtitleDelayMs = 250,
            preferredSubtitleLang = "en",
            preferredAudioLang = "hi",
            accentPreset = "CINEMA_CYAN",
            compactGrid = true,
            autoFetchLocalMetadata = true,
            downloadWifiOnly = false
        )
        val prefRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.savePreferences(prefs) }
        assertTrue("savePreferences failed: ${prefRes.exceptionOrNull()}", prefRes.isSuccess)

        val observedPrefs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePreferences().first { it.preferredEngine == "NEXTPLAYER" }
        }
        assertEquals("CINEMA_CYAN", observedPrefs.accentPreset)
        assertEquals("hi", observedPrefs.preferredAudioLang)
        assertEquals(22, observedPrefs.subtitleFontSizeSp)
    }

    @Test
    fun crossUserAccess_bobReadingAliceLibrary_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreSyncRepository(firestore)
        val mediaKey = "secret_${UUID.randomUUID().toString().take(8)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertLibraryItem(
                LibraryMediaItem(
                    userId = aliceUid,
                    mediaKey = mediaKey,
                    title = "Alice Private Stream",
                    mediaType = "cloud_video"
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val thrown = runCatching {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("library").document(mediaKey)
                    .get().await()
            }
        }.exceptionOrNull()

        val fsEx = thrown?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun unauthenticatedQuery_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val thrown = runCatching {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document("some_uid")
                    .collection("addons").get().await()
            }
        }.exceptionOrNull()

        val fsEx = thrown?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_cinevault@test.com"
        const val BOB_EMAIL = "bob_cinevault@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
