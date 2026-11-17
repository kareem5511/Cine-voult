package com.example.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFilenameParserTest {

    @Test
    fun parseMovieFilename_extractsTitleYearAndQualityTags() {
        val result = LocalFilenameParser.parse("Blade.Runner.2049.2017.2160p.UHD.BluRay.x265.HDR.mkv")
        assertEquals("Blade Runner 2049", result.cleanTitle)
        assertEquals("2017", result.year)
        assertFalse(result.isSeries)
        assertNull(result.season)
        assertNull(result.episode)
        assertTrue(result.qualityTags.contains("2160P"))
        assertTrue(result.qualityTags.contains("X265"))
    }

    @Test
    fun parseSeriesFilename_extractsTitleSeasonAndEpisode() {
        val result = LocalFilenameParser.parse("Severance.S01E09.1080p.WEB-DL.H264.mp4")
        assertEquals("Severance", result.cleanTitle)
        assertTrue(result.isSeries)
        assertEquals(1, result.season)
        assertEquals(9, result.episode)
        assertTrue(result.qualityTags.contains("1080P"))
    }

    @Test
    fun parseAlternativeSeriesFormat_extractsSeasonAndEpisode() {
        val result = LocalFilenameParser.parse("Andor_2x04_720p_WEBRip.mkv")
        assertEquals("Andor", result.cleanTitle)
        assertTrue(result.isSeries)
        assertEquals(2, result.season)
        assertEquals(4, result.episode)
    }

    @Test
    fun parsePlainTitle_handlesSpacesAndBrackets() {
        val result = LocalFilenameParser.parse("[ReleaseGroup] Tears of Steel (2012) [1080p].mp4")
        assertEquals("Tears Of Steel", result.cleanTitle)
        assertEquals("2012", result.year)
        assertFalse(result.isSeries)
    }
}
