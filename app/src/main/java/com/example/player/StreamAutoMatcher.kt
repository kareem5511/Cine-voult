package com.example.player

object StreamAutoMatcher {

    private val resolutionPatterns = listOf(
        "4K" to listOf("2160p", "4k", "uhd"),
        "1080p" to listOf("1080p", "fhd"),
        "720p" to listOf("720p", " hd "),
        "480p" to listOf("480p", "sd")
    )

    private val keywordTags = listOf(
        "WEB-DL", "WEBRip", "BluRay", "BDRip", "REMUX", "HDR10+", "HDR",
        "DV", "Dolby Vision", "HEVC", "x265", "H265", "AV1", "x264", "H264",
        "Atmos", "TrueHD", "DTS-HD", "AAC", "Dual Audio", "Multi Audio"
    )

    private val languageDisplayNames = mapOf(
        "en" to listOf("english", "eng", "dual audio"),
        "hi" to listOf("hindi", "hin", "dual audio"),
        "es" to listOf("spanish", "spa", "latino", "castellano"),
        "fr" to listOf("french", "fre", "fra", "vff"),
        "de" to listOf("german", "ger", "deu"),
        "ja" to listOf("japanese", "jpn"),
        "ko" to listOf("korean", "kor"),
        "ar" to listOf("arabic", "ara"),
        "pt" to listOf("portuguese", "por", "pob", "dublado"),
        "it" to listOf("italian", "ita"),
        "zh" to listOf("chinese", "chi", "zho", "mandarin")
    )

    fun extractResolution(text: String): String {
        val padded = " ${text.lowercase()} "
        return resolutionPatterns.firstOrNull { (_, tokens) ->
            tokens.any { padded.contains(it) }
        }?.first.orEmpty()
    }

    fun extractTags(text: String): List<String> {
        val lower = text.lowercase()
        val matched = mutableListOf<String>()
        for (tag in keywordTags) {
            val token = tag.lowercase()
            if (token == "dv") {
                if (Regex("""\bdv\b|\bdovi\b|dolby\.?\s?vision""").containsMatchIn(lower)) {
                    matched.add("DV")
                }
            } else if (lower.contains(token)) {
                matched.add(tag)
            }
        }
        // Extract trailing release group name (e.g. "-NTb", "-SPARKS", "-FLUX")
        val groupMatch = Regex("""-([A-Za-z0-9]{2,14})(?:\.[a-z0-9]{2,4})?$""", RegexOption.MULTILINE)
            .find(text.trim())
        val group = groupMatch?.groupValues?.getOrNull(1)
        if (!group.isNullOrBlank() && !group.equals("dl", ignoreCase = true)) {
            matched.add(group.uppercase())
        }
        return matched.distinct()
    }

    fun parseOption(
        id: String,
        addonId: String,
        addonName: String,
        fullFileName: String,
        badges: List<String>,
        streamUrl: String
    ): StreamSourceOption {
        val combined = "$addonName $fullFileName ${badges.joinToString(" ")} $streamUrl"
        val res = extractResolution(combined)
        val tags = extractTags(combined)
        return StreamSourceOption(
            id = id,
            fullFileName = fullFileName,
            badges = badges,
            streamUrl = streamUrl,
            addonName = addonName,
            addonId = addonId,
            resolution = res,
            tags = tags
        )
    }

    fun extractFingerprint(option: StreamSourceOption): StreamFingerprint {
        val combined = "${option.addonName} ${option.fullFileName} ${option.badges.joinToString(" ")}"
        val resolution = option.resolution.ifBlank { extractResolution(combined) }
        val tags = option.tags.ifEmpty { extractTags(combined) }
        val inferredAddon = option.addonName.ifBlank {
            option.badges.drop(1).firstOrNull().orEmpty()
        }
        val inferredAddonId = option.addonId.ifBlank { inferredAddon.lowercase() }
        return StreamFingerprint(
            addonName = inferredAddon,
            addonId = inferredAddonId,
            resolution = resolution,
            tags = tags
        )
    }

    fun scoreStream(
        option: StreamSourceOption,
        preference: StreamFingerprint?,
        preferredAudioLang: String
    ): Int {
        var score = 0
        val combinedText = "${option.addonName} ${option.fullFileName} ${option.badges.joinToString(" ")}"
        val combinedLower = combinedText.lowercase()
        val optionRes = option.resolution.ifBlank { extractResolution(combinedText) }
        val optionTags = option.tags.ifEmpty { extractTags(combinedText) }.map { it.lowercase() }.toSet()

        if (preference != null) {
            if (preference.addonId.isNotBlank() &&
                (option.addonId.equals(preference.addonId, ignoreCase = true) ||
                    option.addonName.equals(preference.addonName, ignoreCase = true) ||
                    option.badges.any { it.equals(preference.addonName, ignoreCase = true) })
            ) {
                score += 50
            }
            if (preference.resolution.isNotBlank() &&
                normalizeResolution(optionRes) == normalizeResolution(preference.resolution)
            ) {
                score += 40
            }
            for (prefTag in preference.tags) {
                if (prefTag.lowercase() in optionTags) {
                    score += 15
                }
            }
        }

        val prefLang = preferredAudioLang.trim().lowercase()
        if (prefLang.isNotBlank() && prefLang != "auto" && prefLang != "off") {
            val langAliases = languageDisplayNames[prefLang] ?: listOf(prefLang)
            if (langAliases.any { alias -> combinedLower.contains(alias) } ||
                option.badges.any { LanguageCodeMatcher.matches(it, prefLang) }
            ) {
                score += 25
            }
        }
        return score
    }

    fun pickBestMatch(
        sources: List<StreamSourceOption>,
        preference: StreamFingerprint?,
        preferredAudioLang: String
    ): StreamSourceOption? {
        if (sources.isEmpty()) return null
        return sources.maxByOrNull { option ->
            scoreStream(option, preference, preferredAudioLang)
        }
    }

    private fun normalizeResolution(res: String): String {
        val r = res.trim().lowercase()
        return when (r) {
            "2160p", "4k", "uhd" -> "4K"
            "1080p", "fhd" -> "1080p"
            "720p", "hd" -> "720p"
            "480p", "sd" -> "480p"
            else -> r
        }
    }
}
