package com.example.data.local

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

data class ParsedFilenameMetadata(
    val cleanTitle: String,
    val year: String = "",
    val season: Int? = null,
    val episode: Int? = null,
    val qualityTags: List<String> = emptyList(),
    val isSeries: Boolean = false
)

object LocalFilenameParser {

    private val seasonEpisodeRegex = Regex(
        """(?i)(?:^|[\s._\-\[])S(\d{1,2})[\s._\-]?E(\d{1,3})(?:$|[\s._\-\]])"""
    )
    private val altSeasonEpisodeRegex = Regex(
        """(?i)(?:^|[\s._\-])(\d{1,2})x(\d{1,2})(?:$|[\s._\-])"""
    )
    private val yearRegex = Regex("""(?<!\d)(19\d{2}|20[0-3]\d)(?!\d)""")
    private val qualityRegex = Regex(
        """(?i)\b(2160p|4K|UHD|1080p|720p|480p|HDR10\+?|HDR|DV|Dolby\.?Vision|REMUX|BluRay|BDRip|WEB-DL|WEBRip|HDTV|x265|HEVC|x264|H\.?264|AV1|10bit|AAC|DTS|TrueHD|Atmos)\b"""
    )
    private val bracketedRegex = Regex("""\[[^\]]*\]|\([^)]*\)""")

    fun parse(rawFilename: String): ParsedFilenameMetadata {
        val withoutExt = rawFilename.substringBeforeLast('.', rawFilename)
        val qualities = qualityRegex.findAll(withoutExt)
            .map { it.value.uppercase() }
            .distinct()
            .toList()

        val seMatch = seasonEpisodeRegex.find(withoutExt) ?: altSeasonEpisodeRegex.find(withoutExt)
        val season = seMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
        val episode = seMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
        val isSeries = season != null && episode != null

        val yearMatches = yearRegex.findAll(withoutExt).toList()
        val detectedYear = yearMatches.lastOrNull()?.value ?: ""

        // Cut string at season/episode marker or first quality/year token to isolate title
        var working = bracketedRegex.replace(withoutExt, " ")
        if (seMatch != null) {
            val seInWorking = seasonEpisodeRegex.find(working) ?: altSeasonEpisodeRegex.find(working)
            if (seInWorking != null) {
                working = working.substring(0, seInWorking.range.first)
            }
        }

        val workingYearMatches = yearRegex.findAll(working).toList()
        if (workingYearMatches.isNotEmpty() && !isSeries) {
            working = working.substring(0, workingYearMatches.last().range.first)
        }

        val firstQuality = qualityRegex.find(working)
        if (firstQuality != null) {
            working = working.substring(0, firstQuality.range.first)
        }

        working = working
            .replace('.', ' ')
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("""[()\[\]]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        val cleanTitle = if (working.isBlank()) {
            withoutExt.replace('.', ' ').replace('_', ' ').trim().ifBlank { "Untitled Video" }
        } else {
            working.split(' ').joinToString(" ") { word ->
                if (word.length <= 2 && word.all { it.isUpperCase() }) word
                else word.replaceFirstChar { it.uppercase() }
            }
        }

        return ParsedFilenameMetadata(
            cleanTitle = cleanTitle,
            year = detectedYear,
            season = season,
            episode = episode,
            qualityTags = qualities,
            isSeries = isSeries
        )
    }

    data class LocalVideoProbe(
        val displayName: String,
        val fileSizeBytes: Long,
        val durationMs: Long,
        val resolution: String,
        val mimeType: String
    )

    fun probeUri(context: Context, uri: Uri): LocalVideoProbe {
        var displayName = uri.lastPathSegment ?: "local_video.mp4"
        var fileSize = 0L
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "video/mp4"

        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) fileSize = cursor.getLong(sizeIdx)
                    }
                }
        }

        var durationMs = 0L
        var resolution = ""
        val retriever = MediaMetadataRetriever()
        runCatching {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            if (!width.isNullOrBlank() && !height.isNullOrBlank()) {
                resolution = "${width}x${height}"
            }
        }
        runCatching { retriever.release() }

        return LocalVideoProbe(
            displayName = displayName,
            fileSizeBytes = fileSize,
            durationMs = durationMs,
            resolution = resolution,
            mimeType = mimeType
        )
    }
}
