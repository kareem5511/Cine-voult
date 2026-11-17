package com.example.data.stremio

import com.example.data.model.StremioCatalogResponse
import com.example.data.model.StremioManifest
import com.example.data.model.StremioMetaDetail
import com.example.data.model.StremioMetaPreview
import com.example.data.model.StremioMetaResponse
import com.example.data.model.StremioStream
import com.example.data.model.StremioStreamResponse
import com.example.data.model.StremioSubtitle
import com.example.data.model.StremioSubtitlesResponse
import com.example.data.model.StremioTrailerStream
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object StremioUrlHelper {
    const val DEFAULT_CINEMETA_MANIFEST = "https://v3-cinemeta.strem.io/manifest.json"
    const val DEFAULT_OPENSUBTITLES_MANIFEST = "https://opensubtitles-v3.strem.io/manifest.json"
    const val BUILTIN_OPEN_CINEMA_KEY = "cinevault_open_cinema"
    const val BUILTIN_OPEN_CINEMA_MANIFEST = "https://cinevault.local/open-cinema/manifest.json"

    fun normalizeManifestUrl(rawInput: String): String {
        val trimmed = rawInput.trim()
        val httpNormalized = when {
            trimmed.startsWith("stremio://", ignoreCase = true) ->
                "https://" + trimmed.removePrefix("stremio://").removePrefix("STREMIO://")
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }
        val queryIdx = httpNormalized.indexOf('?')
        val pathPart = if (queryIdx >= 0) httpNormalized.substring(0, queryIdx) else httpNormalized
        val queryPart = if (queryIdx >= 0) httpNormalized.substring(queryIdx) else ""
        val normalizedPath = if (pathPart.endsWith("/manifest.json", ignoreCase = true)) {
            pathPart
        } else {
            pathPart.trimEnd('/') + "/manifest.json"
        }
        return normalizedPath + queryPart
    }

    fun baseUrlFromManifest(manifestUrl: String): String {
        val normalized = normalizeManifestUrl(manifestUrl)
        val queryIdx = normalized.indexOf('?')
        val pathPart = if (queryIdx >= 0) normalized.substring(0, queryIdx) else normalized
        return pathPart
            .replace(Regex("/manifest\\.json$", RegexOption.IGNORE_CASE), "")
            .trimEnd('/')
    }

    fun buildEndpointUrl(manifestUrl: String, relativePath: String): String {
        val normalized = normalizeManifestUrl(manifestUrl)
        val queryIdx = normalized.indexOf('?')
        val queryPart = if (queryIdx >= 0) normalized.substring(queryIdx) else ""
        val base = baseUrlFromManifest(normalized)
        val cleanRelative = relativePath.trimStart('/')
        return "$base/$cleanRelative$queryPart"
    }

    fun sanitizeDocId(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(100)
            .ifBlank { "addon_${System.currentTimeMillis()}" }
    }
}

class StremioAddonClient(
    private val okHttpClient: OkHttpClient = defaultOkHttpClient(),
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
) {
    private val manifestAdapter = moshi.adapter(StremioManifest::class.java)
    private val catalogAdapter = moshi.adapter(StremioCatalogResponse::class.java)
    private val metaAdapter = moshi.adapter(StremioMetaResponse::class.java)
    private val streamAdapter = moshi.adapter(StremioStreamResponse::class.java)
    private val subtitlesAdapter = moshi.adapter(StremioSubtitlesResponse::class.java)
    private val manifestCache = java.util.concurrent.ConcurrentHashMap<String, StremioManifest>()

    suspend fun fetchManifest(rawUrl: String, forceRefresh: Boolean = false): Result<Pair<String, StremioManifest>> =
        withContext(Dispatchers.IO) {
            val normalizedUrl = StremioUrlHelper.normalizeManifestUrl(rawUrl)
            if (normalizedUrl == StremioUrlHelper.BUILTIN_OPEN_CINEMA_MANIFEST) {
                return@withContext Result.success(normalizedUrl to builtInOpenCinemaManifest())
            }
            if (!forceRefresh) {
                manifestCache[normalizedUrl]?.let { cached ->
                    return@withContext Result.success(normalizedUrl to cached)
                }
            }
            runCatching {
                val json = executeGet(normalizedUrl)
                val parsed = manifestAdapter.fromJson(json)
                    ?: error("Invalid Stremio manifest JSON")
                manifestCache[normalizedUrl] = parsed
                normalizedUrl to parsed
            }
        }

    suspend fun fetchCatalog(
        manifestUrl: String,
        type: String,
        catalogId: String,
        searchQuery: String? = null,
        extraParams: Map<String, String> = emptyMap()
    ): Result<List<StremioMetaPreview>> = withContext(Dispatchers.IO) {
        if (manifestUrl == StremioUrlHelper.BUILTIN_OPEN_CINEMA_MANIFEST) {
            val skip = extraParams["skip"]?.toIntOrNull() ?: 0
            val genreFilter = extraParams["genre"]
            val filtered = builtInOpenCinemaCatalog().filter { item ->
                item.type == type &&
                    (searchQuery.isNullOrBlank() || item.name.contains(searchQuery, ignoreCase = true)) &&
                    (genreFilter.isNullOrBlank() || item.genres?.any { it.equals(genreFilter, ignoreCase = true) } == true)
            }.drop(skip)
            return@withContext Result.success(filtered)
        }
        runCatching {
            val encodedType = URLEncoder.encode(type, "UTF-8").replace("+", "%20")
            val encodedCatalogId = URLEncoder.encode(catalogId, "UTF-8").replace("+", "%20")
            val combinedExtras = linkedMapOf<String, String>()
            extraParams.forEach { (k, v) ->
                if (k.isNotBlank() && v.isNotBlank()) combinedExtras[k] = v
            }
            if (!searchQuery.isNullOrBlank()) {
                combinedExtras["search"] = searchQuery.trim()
            }
            val relPath = if (combinedExtras.isNotEmpty()) {
                val extraSegment = combinedExtras.entries.joinToString("&") { (k, v) ->
                    val ek = URLEncoder.encode(k, "UTF-8").replace("+", "%20")
                    val ev = URLEncoder.encode(v, "UTF-8").replace("+", "%20")
                    "$ek=$ev"
                }
                "catalog/$encodedType/$encodedCatalogId/$extraSegment.json"
            } else {
                "catalog/$encodedType/$encodedCatalogId.json"
            }
            val url = StremioUrlHelper.buildEndpointUrl(manifestUrl, relPath)
            val json = executeGet(url)
            catalogAdapter.fromJson(json)?.metas ?: emptyList()
        }
    }

    suspend fun fetchMeta(
        manifestUrl: String,
        type: String,
        mediaId: String
    ): Result<StremioMetaDetail?> = withContext(Dispatchers.IO) {
        if (manifestUrl == StremioUrlHelper.BUILTIN_OPEN_CINEMA_MANIFEST ||
            mediaId.startsWith("cv_open_")
        ) {
            return@withContext Result.success(builtInOpenCinemaMeta(mediaId))
        }
        runCatching {
            val encodedId = URLEncoder.encode(mediaId, "UTF-8").replace("+", "%20")
            val url = StremioUrlHelper.buildEndpointUrl(manifestUrl, "meta/$type/$encodedId.json")
            val json = executeGet(url)
            metaAdapter.fromJson(json)?.meta
        }
    }

    suspend fun fetchStreams(
        manifestUrl: String,
        type: String,
        mediaId: String
    ): Result<List<StremioStream>> = withContext(Dispatchers.IO) {
        if (manifestUrl == StremioUrlHelper.BUILTIN_OPEN_CINEMA_MANIFEST ||
            mediaId.startsWith("cv_open_")
        ) {
            return@withContext Result.success(builtInOpenCinemaStreams(mediaId))
        }
        runCatching {
            val encodedId = URLEncoder.encode(mediaId, "UTF-8").replace("+", "%20")
            val url = StremioUrlHelper.buildEndpointUrl(manifestUrl, "stream/$type/$encodedId.json")
            val json = executeGet(url)
            streamAdapter.fromJson(json)?.streams ?: emptyList()
        }
    }

    suspend fun fetchSubtitles(
        manifestUrl: String,
        type: String,
        mediaId: String
    ): Result<List<StremioSubtitle>> = withContext(Dispatchers.IO) {
        if (mediaId.startsWith("cv_open_")) {
            return@withContext Result.success(builtInOpenCinemaSubtitles(mediaId))
        }
        runCatching {
            val encodedId = URLEncoder.encode(mediaId, "UTF-8").replace("+", "%20")
            val url = StremioUrlHelper.buildEndpointUrl(manifestUrl, "subtitles/$type/$encodedId.json")
            val json = executeGet(url)
            subtitlesAdapter.fromJson(json)?.subtitles ?: emptyList()
        }
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "CineVault-Android/1.0")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code} fetching $url")
            }
            return response.body?.string() ?: error("Empty response body from $url")
        }
    }

    companion object {
        fun defaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        fun builtInOpenCinemaManifest(): StremioManifest = StremioManifest(
            id = StremioUrlHelper.BUILTIN_OPEN_CINEMA_KEY,
            name = "Open Cinema Vault",
            version = "1.0.0",
            description = "Curated Creative Commons & open-source 4K/HD films with instant HLS & MP4 streams for Next Player testing.",
            types = listOf("movie"),
            resources = listOf("catalog", "meta", "stream", "subtitles")
        )

        fun builtInOpenCinemaCatalog(): List<StremioMetaPreview> = listOf(
            StremioMetaPreview(
                id = "cv_open_tears_of_steel",
                type = "movie",
                name = "Tears of Steel",
                poster = "https://mango.blender.org/wp-content/uploads/2013/05/01_thom_celia_bridge.jpg",
                background = "https://mango.blender.org/wp-content/uploads/2013/05/01_thom_celia_bridge.jpg",
                description = "In a dystopian Amsterdam, a group of warriors and scientists gather at the Oude Kerk to stage a desperate stand against robotic dominance.",
                releaseInfo = "2012",
                imdbRating = "7.2",
                genres = listOf("Sci-Fi", "Action", "Short"),
                trailerStreams = listOf(
                    StremioTrailerStream(
                        title = "Official 4K Teaser",
                        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
                    )
                )
            ),
            StremioMetaPreview(
                id = "cv_open_sintel",
                type = "movie",
                name = "Sintel",
                poster = "https://durian.blender.org/wp-content/uploads/2010/06/05.8b_comp_000272.jpg",
                background = "https://durian.blender.org/wp-content/uploads/2010/06/05.8b_comp_000272.jpg",
                description = "A lonely wanderer embarks on a dangerous quest across scorching deserts and frozen peaks to rescue a baby dragon she named Scales.",
                releaseInfo = "2010",
                imdbRating = "7.5",
                genres = listOf("Fantasy", "Adventure", "Animation"),
                trailerStreams = listOf(
                    StremioTrailerStream(
                        title = "Official Trailer",
                        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
                    )
                )
            ),
            StremioMetaPreview(
                id = "cv_open_big_buck_bunny",
                type = "movie",
                name = "Big Buck Bunny",
                poster = "https://peach.blender.org/wp-content/uploads/title_anouncement.jpg",
                background = "https://peach.blender.org/wp-content/uploads/title_anouncement.jpg",
                description = "A giant, gentle rabbit confronts three mischievous woodland bullies in a slapstick woodland showdown.",
                releaseInfo = "2008",
                imdbRating = "6.5",
                genres = listOf("Comedy", "Animation"),
                trailerStreams = listOf(
                    StremioTrailerStream(
                        title = "Official Preview",
                        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                    )
                )
            ),
            StremioMetaPreview(
                id = "cv_open_elephants_dream",
                type = "movie",
                name = "Elephants Dream",
                poster = "https://orange.blender.org/wp-content/themes/orange/images/media/gallery/s4_1024x576_proog.jpg",
                background = "https://orange.blender.org/wp-content/themes/orange/images/media/gallery/s4_1024x576_proog.jpg",
                description = "Two companions navigate a surreal, endlessly shifting mechanical labyrinth where imagination bends physical reality.",
                releaseInfo = "2006",
                imdbRating = "5.8",
                genres = listOf("Sci-Fi", "Surreal", "Animation"),
                trailerStreams = listOf(
                    StremioTrailerStream(
                        title = "Official Preview",
                        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
                    )
                )
            )
        )

        fun builtInOpenCinemaMeta(mediaId: String): StremioMetaDetail? {
            val preview = builtInOpenCinemaCatalog().firstOrNull { it.id == mediaId } ?: return null
            return StremioMetaDetail(
                id = preview.id,
                type = preview.type,
                name = preview.name,
                poster = preview.poster,
                background = preview.background,
                logo = preview.logo,
                description = preview.description,
                releaseInfo = preview.releaseInfo,
                imdbRating = preview.imdbRating,
                runtime = "12 min",
                genres = preview.genres,
                director = listOf("Blender Foundation"),
                trailerStreams = preview.trailerStreams
            )
        }

        fun builtInOpenCinemaStreams(mediaId: String): List<StremioStream> = when (mediaId) {
            "cv_open_tears_of_steel" -> listOf(
                StremioStream(
                    name = "Open Cinema • HLS Adaptive",
                    title = "1080p • H.264 / AAC • Fast Stream",
                    url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
                ),
                StremioStream(
                    name = "Open Cinema • MP4 Direct",
                    title = "720p MP4 • Offline Downloadable",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
                )
            )
            "cv_open_sintel" -> listOf(
                StremioStream(
                    name = "Open Cinema • MP4 HD",
                    title = "1080p MP4 • Offline Downloadable",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
                )
            )
            "cv_open_big_buck_bunny" -> listOf(
                StremioStream(
                    name = "Open Cinema • MP4 HD",
                    title = "1080p MP4 • Offline Downloadable",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                )
            )
            "cv_open_elephants_dream" -> listOf(
                StremioStream(
                    name = "Open Cinema • MP4 HD",
                    title = "720p MP4 • Offline Downloadable",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
                )
            )
            else -> emptyList()
        }

        fun builtInOpenCinemaSubtitles(mediaId: String): List<StremioSubtitle> = listOf(
            StremioSubtitle(
                id = "${mediaId}_en",
                url = "https://raw.githubusercontent.com/brenopolanski/html5-video-webvtt-example/master/MIB2-subtitles-pt-BR.vtt",
                lang = "en",
                subFileName = "$mediaId.en.vtt"
            )
        )
    }
}
