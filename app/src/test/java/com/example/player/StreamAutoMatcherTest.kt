package com.example.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamAutoMatcherTest {

    @Test
    fun extractResolutionAndTags_parsesReleaseTokensAndGroup() {
        val fileName = "Severance.S02E03.2160p.WEB-DL.DDP5.1.DV.HEVC-NTB.mkv"
        assertEquals("4K", StreamAutoMatcher.extractResolution(fileName))
        val tags = StreamAutoMatcher.extractTags(fileName)
        assertTrue(tags.contains("WEB-DL"))
        assertTrue(tags.contains("DV"))
        assertTrue(tags.contains("HEVC"))
        assertTrue(tags.contains("NTB"))
    }

    @Test
    fun scoreStream_weightsAddonResolutionLanguageAndTagsAccurately() {
        val pref = StreamFingerprint(
            addonName = "Torrentio",
            addonId = "torrentio",
            resolution = "1080p",
            tags = listOf("WEB-DL", "HEVC")
        )
        val exactMatch = StreamAutoMatcher.parseOption(
            id = "s1",
            addonId = "torrentio",
            addonName = "Torrentio",
            fullFileName = "Show.S01E02.1080p.WEB-DL.HEVC.Hindi-FLUX.mkv",
            badges = listOf("1080p", "Torrentio", "Hindi"),
            streamUrl = "https://cdn.example.com/s1e2_1080.mkv"
        )
        // Expected score: 50 (addon) + 40 (1080p) + 25 (Hindi audio) + 30 (WEB-DL + HEVC) = 145
        val scoreExact = StreamAutoMatcher.scoreStream(exactMatch, pref, "hi")
        assertEquals(145, scoreExact)

        val wrongAddon4k = StreamAutoMatcher.parseOption(
            id = "s2",
            addonId = "comet",
            addonName = "Comet",
            fullFileName = "Show.S01E02.2160p.BluRay.x264.mkv",
            badges = listOf("4K", "Comet"),
            streamUrl = "https://cdn.example.com/s1e2_4k.mkv"
        )
        val scoreOther = StreamAutoMatcher.scoreStream(wrongAddon4k, pref, "hi")
        assertEquals(0, scoreOther)
    }

    @Test
    fun pickBestMatch_selectsBestWeightedStreamAndHandlesEmpty() {
        val pref = StreamFingerprint(
            addonName = "Torrentio",
            addonId = "torrentio",
            resolution = "1080p",
            tags = listOf("HEVC")
        )
        val opt1 = StreamAutoMatcher.parseOption(
            id = "a",
            addonId = "mediafusion",
            addonName = "MediaFusion",
            fullFileName = "Show.S01E02.2160p.REMUX.mkv",
            badges = listOf("4K", "MediaFusion"),
            streamUrl = "https://cdn.example.com/a.mkv"
        )
        val opt2 = StreamAutoMatcher.parseOption(
            id = "b",
            addonId = "torrentio",
            addonName = "Torrentio",
            fullFileName = "Show.S01E02.1080p.WEB-DL.HEVC.mkv",
            badges = listOf("1080p", "Torrentio"),
            streamUrl = "https://cdn.example.com/b.mkv"
        )
        val picked = StreamAutoMatcher.pickBestMatch(listOf(opt1, opt2), pref, "en")
        assertNotNull(picked)
        assertEquals("b", picked?.id)
        assertNull(StreamAutoMatcher.pickBestMatch(emptyList(), pref, "en"))
    }
}
