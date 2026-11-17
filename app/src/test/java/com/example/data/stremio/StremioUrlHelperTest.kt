package com.example.data.stremio

import com.example.data.model.StremioBehaviorHints
import com.example.data.model.StremioCatalogDef
import com.example.data.model.StremioExtraDef
import com.example.data.model.StremioManifest
import com.example.data.model.StremioStream
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StremioUrlHelperTest {

    @Test
    fun normalizeManifestUrl_convertsStremioProtocolAndAppendsManifest() {
        assertEquals(
            "https://v3-cinemeta.strem.io/manifest.json",
            StremioUrlHelper.normalizeManifestUrl("stremio://v3-cinemeta.strem.io/manifest.json")
        )
        assertEquals(
            "https://example-addon.org/manifest.json",
            StremioUrlHelper.normalizeManifestUrl("https://example-addon.org/")
        )
        assertEquals(
            "https://custom-addon.net/manifest.json",
            StremioUrlHelper.normalizeManifestUrl("custom-addon.net")
        )
    }

    @Test
    fun normalizeAndBuildEndpoint_preservesQueryParameters() {
        val raw = "https://torrentio.strem.fun/sort=qualitysize/manifest.json?apikey=abc123"
        assertEquals(
            "https://torrentio.strem.fun/sort=qualitysize/manifest.json?apikey=abc123",
            StremioUrlHelper.normalizeManifestUrl(raw)
        )
        assertEquals(
            "https://torrentio.strem.fun/sort=qualitysize",
            StremioUrlHelper.baseUrlFromManifest(raw)
        )
        assertEquals(
            "https://torrentio.strem.fun/sort=qualitysize/catalog/movie/top.json?apikey=abc123",
            StremioUrlHelper.buildEndpointUrl(raw, "catalog/movie/top.json")
        )
    }

    @Test
    fun baseUrlFromManifest_stripsManifestJson() {
        assertEquals(
            "https://opensubtitles-v3.strem.io",
            StremioUrlHelper.baseUrlFromManifest("https://opensubtitles-v3.strem.io/manifest.json")
        )
    }

    @Test
    fun parseManifest_handlesStringAndObjectResources() {
        val json = """
            {
              "id": "org.stremio.test",
              "name": "Test Streamer",
              "version": "1.2.0",
              "types": ["movie", "series"],
              "resources": [
                "catalog",
                {"name": "stream", "types": ["movie"], "idPrefixes": ["tt"]},
                "subtitles"
              ]
            }
        """.trimIndent()
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val manifest = moshi.adapter(StremioManifest::class.java).fromJson(json)!!
        val resNames = manifest.extractedResourceNames()
        assertEquals(listOf("catalog", "stream", "subtitles"), resNames)
        assertTrue(manifest.types.contains("movie"))
    }

    @Test
    fun catalogDef_detectsRequiredExtrasAndSearchSupport() {
        val searchOnly = StremioCatalogDef(
            type = "movie",
            id = "search_cat",
            extraSupported = listOf("search"),
            extraRequired = listOf("search")
        )
        assertTrue(searchOnly.hasRequiredExtraForBrowse())
        assertTrue(searchOnly.supportsSearch())

        val browseCatalog = StremioCatalogDef(
            type = "movie",
            id = "popular",
            extra = listOf(StremioExtraDef(name = "genre", isRequired = false), StremioExtraDef(name = "search", isRequired = false))
        )
        assertFalse(browseCatalog.hasRequiredExtraForBrowse())
        assertTrue(browseCatalog.supportsSearch())
    }

    @Test
    fun stream_returnsFullFileNameAndStreamingBadgesWithoutPlayerNames() {
        val stream = StremioStream(
            name = "Torrentio 4K",
            title = "4K HDR10 HEVC Atmos 12.4 GB",
            url = "https://cdn.example.com/Dune.Part.Two.2024.2160p.WEB-DL.mkv",
            behaviorHints = StremioBehaviorHints(
                filename = "Dune.Part.Two.2024.2160p.WEB-DL.DDP5.1.Atmos.DV.HDR.H.265-GROUP.mkv"
            )
        )
        val fullName = stream.fullFileName()
        assertTrue(fullName.contains("Dune.Part.Two.2024.2160p.WEB-DL.DDP5.1.Atmos.DV.HDR.H.265-GROUP.mkv"))
        assertTrue(fullName.contains("4K HDR10 HEVC Atmos 12.4 GB"))

        val badges = stream.streamingBadges()
        assertEquals("Streaming", badges.first())
        assertTrue(badges.contains("4K"))
        assertTrue(badges.none { it.contains("ExoPlayer", ignoreCase = true) || it.contains("libmpv", ignoreCase = true) })
    }

    @Test
    fun catalogDef_supportsRequiredExtrasWithOptionsGenresAndSkip() {
        val genreRequiredWithOptions = StremioCatalogDef(
            type = "movie",
            id = "by_genre",
            extra = listOf(
                StremioExtraDef(
                    name = "genre",
                    isRequired = true,
                    options = listOf("Action", "Sci-Fi", "Drama")
                ),
                StremioExtraDef(name = "skip")
            ),
            extraRequired = listOf("genre")
        )
        assertFalse(genreRequiredWithOptions.hasRequiredExtraForBrowse())
        assertEquals(mapOf("genre" to "Action"), genreRequiredWithOptions.defaultBrowseExtras())
        assertEquals(listOf("Action", "Sci-Fi", "Drama"), genreRequiredWithOptions.availableGenres())
        assertTrue(genreRequiredWithOptions.supportsSkip())

        val opaqueRequiredExtra = StremioCatalogDef(
            type = "movie",
            id = "custom_query",
            extraRequired = listOf("query")
        )
        assertTrue(opaqueRequiredExtra.hasRequiredExtraForBrowse())
    }

    @Test
    fun stream_extractsAudioLanguageBadges() {
        val dualAudioStream = StremioStream(
            name = "Telegram Bot",
            title = "Movie.2026.1080p.WEB-DL.Dual Audio.Hindi.English.2.4 GB.mkv",
            url = "https://cdn.example.com/movie.mkv"
        )
        val badges = dualAudioStream.streamingBadges()
        assertTrue(badges.contains("Streaming"))
        assertTrue(badges.contains("1080p"))
        assertTrue(badges.contains("Dual Audio"))
        assertTrue(badges.contains("2.4 GB"))

        val hindiStream = StremioStream(
            name = "StreamVault",
            title = "Movie.2026.4K.HDR.Hindi.4.1 GB.mkv",
            url = "https://cdn.example.com/movie_hi.mkv"
        )
        val hindiBadges = hindiStream.streamingBadges()
        assertTrue(hindiBadges.contains("4K"))
        assertTrue(hindiBadges.contains("HDR"))
        assertTrue(hindiBadges.contains("Hindi"))
    }
}

