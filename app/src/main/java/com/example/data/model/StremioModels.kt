package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StremioManifest(
    val id: String = "",
    val name: String = "",
    val version: String = "1.0.0",
    val description: String = "",
    val logo: String? = null,
    val background: String? = null,
    val types: List<String> = emptyList(),
    val resources: List<Any> = emptyList(),
    val catalogs: List<StremioCatalogDef> = emptyList(),
    val idPrefixes: List<String>? = null
) {
    fun extractedResourceNames(): List<String> {
        return resources.mapNotNull { item ->
            when (item) {
                is String -> item
                is Map<*, *> -> item["name"] as? String
                else -> null
            }
        }.distinct()
    }
}

@JsonClass(generateAdapter = true)
data class StremioCatalogDef(
    val type: String = "movie",
    val id: String = "",
    val name: String? = null,
    val extra: List<StremioExtraDef>? = null,
    val extraSupported: List<String>? = null,
    val extraRequired: List<String>? = null
) {
    fun hasRequiredExtraForBrowse(): Boolean {
        if (extraRequired?.any { it.equals("search", ignoreCase = true) } == true) return true
        if (extraRequired?.any { req ->
                val matchingDef = extra?.firstOrNull { it.name.equals(req, ignoreCase = true) }
                matchingDef == null || matchingDef.options.isNullOrEmpty()
            } == true
        ) return true
        return extra?.any {
            it.isRequired == true && (it.name.equals("search", ignoreCase = true) || it.options.isNullOrEmpty())
        } == true
    }

    fun defaultBrowseExtras(): Map<String, String> {
        val defaults = linkedMapOf<String, String>()
        extraRequired.orEmpty().forEach { reqName ->
            if (!reqName.equals("search", ignoreCase = true)) {
                val opt = extra?.firstOrNull { it.name.equals(reqName, ignoreCase = true) }?.options?.firstOrNull()
                if (!opt.isNullOrBlank()) defaults[reqName] = opt
            }
        }
        extra.orEmpty()
            .filter { it.isRequired == true && !it.name.equals("search", ignoreCase = true) }
            .forEach { def ->
                val opt = def.options?.firstOrNull()
                if (!opt.isNullOrBlank()) defaults[def.name] = opt
            }
        return defaults
    }

    fun availableGenres(): List<String> {
        return extra?.firstOrNull { it.name.equals("genre", ignoreCase = true) }?.options.orEmpty()
    }

    fun supportsSkip(): Boolean {
        if (extraSupported?.any { it.equals("skip", ignoreCase = true) } == true) return true
        return extra?.any { it.name.equals("skip", ignoreCase = true) } == true
    }

    fun supportsSearch(): Boolean {
        if (extraSupported?.any { it.equals("search", ignoreCase = true) } == true) return true
        return extra?.any { it.name.equals("search", ignoreCase = true) } == true
    }
}

@JsonClass(generateAdapter = true)
data class StremioExtraDef(
    val name: String = "",
    val isRequired: Boolean? = null,
    val options: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class StremioCatalogResponse(
    val metas: List<StremioMetaPreview> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioTrailer(
    val source: String = "",
    val type: String = "Trailer"
)

@JsonClass(generateAdapter = true)
data class StremioTrailerStream(
    val title: String? = null,
    val ytId: String? = null,
    val url: String? = null
)

data class TrailerOption(
    val id: String,
    val title: String,
    val typeLabel: String,
    val directUrl: String? = null,
    val ytId: String? = null
) {
    fun thumbnailUrl(fallbackBackdrop: String?): String? =
        ytId?.takeIf { it.isNotBlank() }?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
            ?: fallbackBackdrop
}

@JsonClass(generateAdapter = true)
data class StremioMetaPreview(
    val id: String = "",
    val type: String = "movie",
    val name: String = "",
    val poster: String? = null,
    val posterShape: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val genres: List<String>? = null,
    val trailers: List<StremioTrailer>? = null,
    val trailerStreams: List<StremioTrailerStream>? = null
)

@JsonClass(generateAdapter = true)
data class StremioMetaResponse(
    val meta: StremioMetaDetail? = null
)

@JsonClass(generateAdapter = true)
data class StremioMetaDetail(
    val id: String = "",
    val type: String = "movie",
    val name: String = "",
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val runtime: String? = null,
    val genres: List<String>? = null,
    val cast: List<String>? = null,
    val director: List<String>? = null,
    val trailers: List<StremioTrailer>? = null,
    val trailerStreams: List<StremioTrailerStream>? = null,
    val videos: List<StremioVideoEpisode>? = null
) {
    fun collectTrailers(previewFallback: StremioMetaPreview? = null): List<TrailerOption> {
        val result = mutableListOf<TrailerOption>()
        val seenKeys = mutableSetOf<String>()

        val allStreams = (trailerStreams.orEmpty() + previewFallback?.trailerStreams.orEmpty())
        allStreams.forEachIndexed { idx, ts ->
            val direct = ts.url?.trim()?.takeIf { it.isNotEmpty() }
            val yt = ts.ytId?.trim()?.takeIf { it.isNotEmpty() }
            val key = direct ?: yt ?: return@forEachIndexed
            if (seenKeys.add(key)) {
                result += TrailerOption(
                    id = "ts_${idx}_${key.hashCode()}",
                    title = ts.title?.takeIf { it.isNotBlank() } ?: "Official Trailer",
                    typeLabel = if (direct != null) "HD Stream" else "YouTube",
                    directUrl = direct,
                    ytId = yt
                )
            }
        }

        val allTrailers = (trailers.orEmpty() + previewFallback?.trailers.orEmpty())
        allTrailers.forEachIndexed { idx, tr ->
            val src = tr.source.trim().takeIf { it.isNotEmpty() } ?: return@forEachIndexed
            if (seenKeys.add(src)) {
                val isDirect = src.startsWith("http://", ignoreCase = true) || src.startsWith("https://", ignoreCase = true)
                result += TrailerOption(
                    id = "tr_${idx}_${src.hashCode()}",
                    title = "${tr.type.ifBlank { "Trailer" }} #${idx + 1}",
                    typeLabel = tr.type.ifBlank { "Trailer" },
                    directUrl = if (isDirect) src else null,
                    ytId = if (isDirect) null else src
                )
            }
        }
        return result
    }
}

fun StremioMetaPreview.collectTrailers(): List<TrailerOption> =
    StremioMetaDetail(
        id = id,
        type = type,
        name = name,
        trailers = trailers,
        trailerStreams = trailerStreams
    ).collectTrailers()

@JsonClass(generateAdapter = true)
data class StremioVideoEpisode(
    val id: String = "",
    val title: String? = null,
    val name: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val overview: String? = null,
    val thumbnail: String? = null,
    val released: String? = null
)

@JsonClass(generateAdapter = true)
data class StremioStreamResponse(
    val streams: List<StremioStream> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioStream(
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val externalUrl: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val behaviorHints: StremioBehaviorHints? = null
) {
    fun displayName(): String = listOfNotNull(name, title, description)
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() } ?: "Stream"

    fun subtitleLabel(): String = fullFileName()

    fun fullFileName(): String {
        val hintFile = behaviorHints?.filename?.trim()?.takeIf { it.isNotEmpty() }
        val rawTitle = title?.trim()?.takeIf { it.isNotEmpty() }
        val rawDesc = description?.trim()?.takeIf { it.isNotEmpty() }
        val rawName = name?.trim()?.takeIf { it.isNotEmpty() }
        return when {
            hintFile != null && rawTitle != null && !rawTitle.contains(hintFile, ignoreCase = true) ->
                "$hintFile\n$rawTitle"
            hintFile != null -> hintFile
            rawTitle != null -> rawTitle
            rawDesc != null -> rawDesc
            rawName != null -> rawName
            else -> "Direct Stream"
        }
    }

    fun streamingBadges(): List<String> {
        val combined = listOfNotNull(name, title, description, behaviorHints?.filename, url)
            .joinToString(" ")
        val lower = combined.lowercase()
        val badges = mutableListOf("Streaming")

        val provider = name?.lines()?.firstOrNull()?.trim()
            ?.takeIf { it.isNotBlank() && it.length <= 28 }
        if (provider != null && !provider.equals("Streaming", ignoreCase = true)) {
            badges.add(provider)
        }

        when {
            lower.contains(".m3u8") || lower.contains("hls") -> badges.add("HLS")
            lower.contains(".mpd") || lower.contains("dash") -> badges.add("DASH")
            lower.contains(".mkv") -> badges.add("MKV")
            lower.contains(".mp4") -> badges.add("MP4")
        }

        when {
            lower.contains("2160p") || lower.contains("4k") || lower.contains("uhd") -> badges.add("4K")
            lower.contains("1080p") || lower.contains("fhd") -> badges.add("1080p")
            lower.contains("720p") || lower.contains(" hd") -> badges.add("720p")
            lower.contains("480p") -> badges.add("480p")
        }

        if (lower.contains("dolby vision") || lower.contains("dovi") || Regex("""\bdv\b""").containsMatchIn(lower)) {
            badges.add("Dolby Vision")
        } else if (lower.contains("hdr10+")) {
            badges.add("HDR10+")
        } else if (lower.contains("hdr")) {
            badges.add("HDR")
        }

        when {
            lower.contains("hevc") || lower.contains("x265") || lower.contains("h265") || lower.contains("h.265") -> badges.add("HEVC")
            lower.contains("av1") -> badges.add("AV1")
            lower.contains("x264") || lower.contains("h264") || lower.contains("h.264") -> badges.add("H.264")
        }

        if (lower.contains("atmos")) badges.add("Atmos")

        when {
            lower.contains("dual audio") || lower.contains("dual-audio") -> badges.add("Dual Audio")
            lower.contains("multi") -> badges.add("Multi Audio")
            Regex("""\b(hindi|hin)\b""").containsMatchIn(lower) -> badges.add("Hindi")
            Regex("""\b(eng|english)\b""").containsMatchIn(lower) -> badges.add("English")
            Regex("""\b(jpn|japanese)\b""").containsMatchIn(lower) -> badges.add("Japanese")
            Regex("""\b(kor|korean)\b""").containsMatchIn(lower) -> badges.add("Korean")
            Regex("""\b(spa|spanish|latino)\b""").containsMatchIn(lower) -> badges.add("Spanish")
        }

        val sizeMatch = Regex("""(\d+(?:\.\d+)?\s*(?:GB|MB|GiB|MiB))""", RegexOption.IGNORE_CASE).find(combined)
        if (sizeMatch != null) {
            badges.add(sizeMatch.groupValues[1].uppercase())
        }

        return badges.distinct().take(8)
    }
}

@JsonClass(generateAdapter = true)
data class StremioBehaviorHints(
    val bingeGroup: String? = null,
    val filename: String? = null,
    val notWebReady: Boolean? = null,
    val proxyHeaders: StremioProxyHeaders? = null
)

@JsonClass(generateAdapter = true)
data class StremioProxyHeaders(
    val request: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class StremioSubtitlesResponse(
    val subtitles: List<StremioSubtitle> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioSubtitle(
    val id: String = "",
    val url: String = "",
    val lang: String = "en",
    @Json(name = "SubFileName") val subFileName: String? = null
) {
    fun displayLanguage(): String {
        val code = lang.trim()
        return when (code.lowercase()) {
            "en", "eng" -> "English"
            "es", "spa" -> "Spanish"
            "fr", "fre", "fra" -> "French"
            "de", "ger", "deu" -> "German"
            "it", "ita" -> "Italian"
            "pt", "por", "pob" -> "Portuguese"
            "ar", "ara" -> "Arabic"
            "hi", "hin" -> "Hindi"
            "ja", "jpn" -> "Japanese"
            "ko", "kor" -> "Korean"
            "zh", "chi", "zho" -> "Chinese"
            else -> code.uppercase()
        }
    }
}
