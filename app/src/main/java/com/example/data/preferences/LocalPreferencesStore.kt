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
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val AUTO_PLAY_TRAILERS = stringPreferencesKey("auto_play_trailers")
        val AUTO_LOCAL_META = booleanPreferencesKey("auto_fetch_local_metadata")
        val WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
        val GUEST_SKIPPED = booleanPreferencesKey("guest_mode_skipped")
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
            preferredEngine = prefs[Keys.ENGINE] ?: "EXOPLAYER",
            hardwareDecoding = prefs[Keys.HW_DECODING] ?: true,
            subtitleFontSizeSp = prefs[Keys.SUB_FONT_SIZE] ?: 18,
            subtitleColorHex = prefs[Keys.SUB_COLOR] ?: "#FFFFFF",
            subtitleBgOpacity = prefs[Keys.SUB_OPACITY] ?: 0.65,
            subtitleDelayMs = prefs[Keys.SUB_DELAY] ?: 0,
            preferredSubtitleLang = prefs[Keys.SUB_LANG] ?: "en",
            preferredAudioLang = prefs[Keys.AUDIO_LANG] ?: "en",
            accentPreset = prefs[Keys.ACCENT] ?: "TUNGSTEN_GOLD",
            compactGrid = prefs[Keys.COMPACT_GRID] ?: false,
            amoledBlack = prefs[Keys.AMOLED_BLACK] ?: false,
            autoPlayTrailers = prefs[Keys.AUTO_PLAY_TRAILERS] ?: "WIFI_ONLY",
            autoFetchLocalMetadata = prefs[Keys.AUTO_LOCAL_META] ?: true,
            downloadWifiOnly = prefs[Keys.WIFI_ONLY] ?: true
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
            mutable[Keys.AMOLED_BLACK] = prefs.amoledBlack
            mutable[Keys.AUTO_PLAY_TRAILERS] = prefs.autoPlayTrailers
            mutable[Keys.AUTO_LOCAL_META] = prefs.autoFetchLocalMetadata
            mutable[Keys.WIFI_ONLY] = prefs.downloadWifiOnly
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
            mutable[Keys.AUTO_LOCAL_META] = remote.autoFetchLocalMetadata
            mutable[Keys.WIFI_ONLY] = remote.downloadWifiOnly
            // amoledBlack and autoPlayTrailers are kept as-is since Firestore does not store them
        }
    }
}
