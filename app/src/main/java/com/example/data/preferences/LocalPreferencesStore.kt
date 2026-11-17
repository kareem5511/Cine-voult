package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cineVaultDataStore by preferencesDataStore(name = "cinevault_prefs")

class LocalPreferencesStore(private val context: Context) {

    private object Keys {
        val ENGINE = stringPreferencesKey("preferred_engine")
        val HW_DECODING = booleanPreferencesKey("hardware_decoding")
        val SUB_FONT_SIZE = intPreferencesKey("subtitle_font_size")
        val SUB_COLOR = stringPreferencesKey("subtitle_color_hex")
        val SUB_OPACITY = doublePreferencesKey("subtitle_bg_opacity")
        val SUB_DELAY = intPreferencesKey("subtitle_delay_ms")
        val SUB_LANG = stringPreferencesKey("preferred_subtitle_lang")
        val AUDIO_LANG = stringPreferencesKey("preferred_audio_lang")
        val ACCENT = stringPreferencesKey("accent_preset")
        val COMPACT_GRID = booleanPreferencesKey("compact_grid")
        val POSTER_LAYOUT = stringPreferencesKey("poster_layout")
        val UI_TEXT_SCALE = stringPreferencesKey("ui_text_scale")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val AUTO_PLAY_TRAILERS = stringPreferencesKey("auto_play_trailers")
        val AUTO_LOCAL_META = booleanPreferencesKey("auto_fetch_local_metadata")
        val WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
        val AMBIENT_GLOW = booleanPreferencesKey("ambient_backdrop_glow")
        val AUTO_MATCH_STREAM = booleanPreferencesKey("auto_match_best_stream")
        val AUTO_SKIP_INTRO = booleanPreferencesKey("auto_skip_intro")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next_episode")
        val CODEC_FALLBACK = booleanPreferencesKey("codec_compatibility_fallback")
        val DL_QUALITY = stringPreferencesKey("default_download_quality")
        val SIMULTANEOUS_DL = intPreferencesKey("simultaneous_downloads_limit")
        val EMBEDDED_SUB_PRIORITY = booleanPreferencesKey("embedded_subtitle_priority")
        val GUEST_SKIPPED = booleanPreferencesKey("guest_mode_skipped")
        val STARTER_ADDONS_SEEDED = booleanPreferencesKey("starter_addons_seeded")
        val HIDDEN_CATALOGS = stringSetPreferencesKey("hidden_catalog_keys")
        val CATALOG_ORDER = stringPreferencesKey("catalog_order_csv")
    }

    val guestModeSkippedFlow: Flow<Boolean> = context.cineVaultDataStore.data.map { prefs ->
        prefs[Keys.GUEST_SKIPPED] ?: false
    }

    suspend fun setGuestModeSkipped(skipped: Boolean) {
        context.cineVaultDataStore.edit { mutable ->
            mutable[Keys.GUEST_SKIPPED] = skipped
        }
    }

    val starterAddonsSeededFlow: Flow<Boolean> = context.cineVaultDataStore.data.map { prefs ->
        prefs[Keys.STARTER_ADDONS_SEEDED] ?: false
    }

    suspend fun setStarterAddonsSeeded(seeded: Boolean) {
        context.cineVaultDataStore.edit { mutable ->
            mutable[Keys.STARTER_ADDONS_SEEDED] = seeded
        }
    }

    val hiddenCatalogsFlow: Flow<Set<String>> = context.cineVaultDataStore.data.map { prefs ->
        prefs[Keys.HIDDEN_CATALOGS] ?: emptySet()
    }

    val catalogOrderFlow: Flow<List<String>> = context.cineVaultDataStore.data.map { prefs ->
        prefs[Keys.CATALOG_ORDER].orEmpty()
            .split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    suspend fun setCatalogHidden(sectionKey: String, hidden: Boolean) {
        context.cineVaultDataStore.edit { mutable ->
            val current = (mutable[Keys.HIDDEN_CATALOGS] ?: emptySet()).toMutableSet()
            if (hidden) current.add(sectionKey) else current.remove(sectionKey)
            mutable[Keys.HIDDEN_CATALOGS] = current
        }
    }

    suspend fun saveCatalogOrder(orderedKeys: List<String>) {
        context.cineVaultDataStore.edit { mutable ->
            mutable[Keys.CATALOG_ORDER] = orderedKeys.distinct().joinToString("|")
        }
    }

    val preferencesFlow: Flow<UserPreferences> = context.cineVaultDataStore.data.map { prefs ->
        UserPreferences(
            preferredEngine = prefs[Keys.ENGINE] ?: "NEXTPLAYER",
            hardwareDecoding = prefs[Keys.HW_DECODING] ?: true,
            subtitleFontSizeSp = prefs[Keys.SUB_FONT_SIZE] ?: 18,
            subtitleColorHex = prefs[Keys.SUB_COLOR] ?: "#FFFFFF",
            subtitleBgOpacity = prefs[Keys.SUB_OPACITY] ?: 0.65,
            subtitleDelayMs = prefs[Keys.SUB_DELAY] ?: 0,
            preferredSubtitleLang = prefs[Keys.SUB_LANG] ?: "en",
            preferredAudioLang = prefs[Keys.AUDIO_LANG] ?: "en",
            accentPreset = prefs[Keys.ACCENT] ?: "TUNGSTEN_GOLD",
            compactGrid = prefs[Keys.COMPACT_GRID] ?: false,
            posterLayout = prefs[Keys.POSTER_LAYOUT] ?: "STANDARD_2",
            uiTextScale = prefs[Keys.UI_TEXT_SCALE] ?: "NORMAL",
            amoledBlack = prefs[Keys.AMOLED_BLACK] ?: false,
            autoPlayTrailers = prefs[Keys.AUTO_PLAY_TRAILERS] ?: "WIFI_ONLY",
            autoFetchLocalMetadata = prefs[Keys.AUTO_LOCAL_META] ?: true,
            downloadWifiOnly = prefs[Keys.WIFI_ONLY] ?: true,
            ambientBackdropGlow = prefs[Keys.AMBIENT_GLOW] ?: true,
            autoMatchBestStream = prefs[Keys.AUTO_MATCH_STREAM] ?: false,
            autoSkipIntro = prefs[Keys.AUTO_SKIP_INTRO] ?: true,
            autoPlayNextEpisode = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
            codecCompatibilityFallback = prefs[Keys.CODEC_FALLBACK] ?: true,
            defaultDownloadQuality = prefs[Keys.DL_QUALITY] ?: "1080p",
            simultaneousDownloadsLimit = prefs[Keys.SIMULTANEOUS_DL] ?: 2,
            embeddedSubtitlePriority = prefs[Keys.EMBEDDED_SUB_PRIORITY] ?: false
        )
    }

    suspend fun save(prefs: UserPreferences) {
        context.cineVaultDataStore.edit { mutable ->
            mutable[Keys.ENGINE] = prefs.preferredEngine
            mutable[Keys.HW_DECODING] = prefs.hardwareDecoding
            mutable[Keys.SUB_FONT_SIZE] = prefs.subtitleFontSizeSp
            mutable[Keys.SUB_COLOR] = prefs.subtitleColorHex
            mutable[Keys.SUB_OPACITY] = prefs.subtitleBgOpacity
            mutable[Keys.SUB_DELAY] = prefs.subtitleDelayMs
            mutable[Keys.SUB_LANG] = prefs.preferredSubtitleLang
            mutable[Keys.AUDIO_LANG] = prefs.preferredAudioLang
            mutable[Keys.ACCENT] = prefs.accentPreset
            mutable[Keys.COMPACT_GRID] = prefs.compactGrid
            mutable[Keys.POSTER_LAYOUT] = prefs.posterLayout
            mutable[Keys.UI_TEXT_SCALE] = prefs.uiTextScale
            mutable[Keys.AMOLED_BLACK] = prefs.amoledBlack
            mutable[Keys.AUTO_PLAY_TRAILERS] = prefs.autoPlayTrailers
            mutable[Keys.AUTO_LOCAL_META] = prefs.autoFetchLocalMetadata
            mutable[Keys.WIFI_ONLY] = prefs.downloadWifiOnly
            mutable[Keys.AMBIENT_GLOW] = prefs.ambientBackdropGlow
            mutable[Keys.AUTO_MATCH_STREAM] = prefs.autoMatchBestStream
            mutable[Keys.AUTO_SKIP_INTRO] = prefs.autoSkipIntro
            mutable[Keys.AUTO_PLAY_NEXT] = prefs.autoPlayNextEpisode
            mutable[Keys.CODEC_FALLBACK] = prefs.codecCompatibilityFallback
            mutable[Keys.DL_QUALITY] = prefs.defaultDownloadQuality
            mutable[Keys.SIMULTANEOUS_DL] = prefs.simultaneousDownloadsLimit
            mutable[Keys.EMBEDDED_SUB_PRIORITY] = prefs.embeddedSubtitlePriority
        }
    }

    suspend fun mergeRemotePreferences(remote: UserPreferences) {
        context.cineVaultDataStore.edit { mutable ->
            mutable[Keys.ENGINE] = remote.preferredEngine
            mutable[Keys.HW_DECODING] = remote.hardwareDecoding
            mutable[Keys.SUB_FONT_SIZE] = remote.subtitleFontSizeSp
            mutable[Keys.SUB_COLOR] = remote.subtitleColorHex
            mutable[Keys.SUB_OPACITY] = remote.subtitleBgOpacity
            mutable[Keys.SUB_DELAY] = remote.subtitleDelayMs
            mutable[Keys.SUB_LANG] = remote.preferredSubtitleLang
            mutable[Keys.AUDIO_LANG] = remote.preferredAudioLang
            mutable[Keys.ACCENT] = remote.accentPreset
            mutable[Keys.COMPACT_GRID] = remote.compactGrid
            mutable[Keys.POSTER_LAYOUT] = remote.posterLayout
            mutable[Keys.UI_TEXT_SCALE] = remote.uiTextScale
            mutable[Keys.AMOLED_BLACK] = remote.amoledBlack
            mutable[Keys.AUTO_PLAY_TRAILERS] = remote.autoPlayTrailers
            mutable[Keys.AUTO_LOCAL_META] = remote.autoFetchLocalMetadata
            mutable[Keys.WIFI_ONLY] = remote.downloadWifiOnly
            mutable[Keys.AMBIENT_GLOW] = remote.ambientBackdropGlow
            mutable[Keys.AUTO_MATCH_STREAM] = remote.autoMatchBestStream
            mutable[Keys.AUTO_SKIP_INTRO] = remote.autoSkipIntro
            mutable[Keys.AUTO_PLAY_NEXT] = remote.autoPlayNextEpisode
            mutable[Keys.CODEC_FALLBACK] = remote.codecCompatibilityFallback
            mutable[Keys.DL_QUALITY] = remote.defaultDownloadQuality
            mutable[Keys.SIMULTANEOUS_DL] = remote.simultaneousDownloadsLimit
            mutable[Keys.EMBEDDED_SUB_PRIORITY] = remote.embeddedSubtitlePriority
        }
    }
}
