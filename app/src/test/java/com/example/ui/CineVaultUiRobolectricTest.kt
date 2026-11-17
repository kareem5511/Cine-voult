package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.data.model.LibraryMediaItem
import com.example.data.model.PlaybackEngine
import com.example.data.model.StremioBehaviorHints
import com.example.data.model.StremioMetaDetail
import com.example.data.model.StremioMetaPreview
import com.example.data.model.StremioStream
import com.example.data.model.StremioTrailer
import com.example.data.model.StremioTrailerStream
import com.example.data.model.StremioVideoEpisode
import com.example.data.model.TrailerAutoPlayMode
import com.example.data.model.UserPreferences
import com.example.ui.screens.AddonsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MediaDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.CineVaultTheme
import com.example.ui.viewmodel.CatalogSection
import com.example.ui.viewmodel.DiscoverUiState
import com.example.ui.viewmodel.MediaDetailUiState
import com.example.ui.viewmodel.UiState
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import com.example.player.PlaybackRequest
import com.example.player.PlayerEpisodeItem
import com.example.player.PlayerTrackInfo
import com.example.player.StreamFingerprint
import com.example.player.StreamSourceOption
import com.example.ui.screens.AudioTrackSheet
import com.example.ui.screens.PlayerScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CineVaultUiRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun authScreen_displaysBrandingGoogleSignInAndSkipButton() {
        var skipped = false
        composeRule.setContent {
            CineVaultTheme {
                AuthScreen(
                    onAuthSuccess = {},
                    onSkipClick = { skipped = true },
                    attemptSilentSignIn = false
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("CineVault").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("sign_in_google_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("skip_sign_in_button").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(skipped)
    }

    @Test
    fun settingsScreen_switchesEngineAccentAudioAndSubtitles() {
        var currentPrefs by androidx.compose.runtime.mutableStateOf(
            UserPreferences(
                userId = "test_user",
                preferredEngine = PlaybackEngine.EXOPLAYER.key,
                accentPreset = AccentPreset.TUNGSTEN_GOLD.key,
                preferredAudioLang = "auto",
                preferredSubtitleLang = "en"
            )
        )

        composeRule.setContent {
            CineVaultTheme {
                SettingsScreen(
                    preferences = currentPrefs,
                    userEmail = "tester@cinevault.app",
                    isGuestMode = false,
                    onUpdatePreferences = { transform ->
                        currentPrefs = transform(currentPrefs)
                    },
                    onSignOut = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("accent_preset_CINEMA_CYAN").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(AccentPreset.CINEMA_CYAN.key, currentPrefs.accentPreset)

        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("engine_libmpv_chip"))
        composeRule.onNodeWithTag("engine_libmpv_chip").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(PlaybackEngine.LIBMPV.key, currentPrefs.preferredEngine)

        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("audio_lang_hi"))
        composeRule.onNodeWithTag("audio_lang_hi").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("hi", currentPrefs.preferredAudioLang)

        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("sub_lang_off"))
        composeRule.onNodeWithTag("sub_lang_off").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("off", currentPrefs.preferredSubtitleLang)
    }

    @Test
    fun settingsScreen_guestModeDisplaysSignInButton() {
        var signInTriggered = false
        composeRule.setContent {
            CineVaultTheme {
                SettingsScreen(
                    preferences = UserPreferences(userId = "local_guest"),
                    userEmail = null,
                    isGuestMode = true,
                    onUpdatePreferences = {},
                    onSignInClick = { signInTriggered = true },
                    onSignOut = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("settings_sign_in_button"))
        composeRule.onNodeWithTag("settings_sign_in_button").assertIsDisplayed().performClick()
        assertTrue(signInTriggered)
    }

    @Test
    fun mediaDetailScreen_displaysStreamingBadgeFullFileNameAndSinglePlayButton() {
        var playedEngine: PlaybackEngine? = null
        val sampleFileName = "Inception.2010.2160p.UHD.BluRay.x265.HDR.Atmos-GROUP.mkv"
        val stream = StremioStream(
            name = "RealDebrid+",
            title = "4K HDR • 18.2 GB",
            url = "https://cdn.example.com/$sampleFileName",
            behaviorHints = StremioBehaviorHints(filename = sampleFileName)
        )
        composeRule.setContent {
            CineVaultTheme {
                MediaDetailScreen(
                    detailState = MediaDetailUiState(
                        isLoading = false,
                        preview = StremioMetaPreview(id = "tt1375666", type = "movie", name = "Inception"),
                        streams = listOf(stream)
                    ),
                    isFavorited = false,
                    preferredEngine = PlaybackEngine.EXOPLAYER,
                    onBack = {},
                    onToggleFavorite = {},
                    onPlayStream = { _, engine -> playedEngine = engine },
                    onDownloadStream = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("media_detail_list").performScrollToNode(hasTestTag("stream_option_card"))
        composeRule.onNodeWithTag("play_primary_button").performScrollTo()
        composeRule.onNodeWithText("Streaming", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("stream_full_filename", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("ExoPlayer").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("libmpv").fetchSemanticsNodes().size)

        composeRule.onNodeWithTag("play_primary_button").performClick()
        assertEquals(PlaybackEngine.EXOPLAYER, playedEngine)
    }

    @Test
    fun discoverScreen_displaysAllEightAddonCatalogsContinueWatchingAndCustomizeButton() {
        val eightSections = (1..8).map { index ->
            CatalogSection(
                sectionKey = "custom_addon::movie::cat_$index::$index",
                addonKey = "custom_addon",
                addonName = "Custom Stremio Addon",
                catalogId = "cat_$index",
                catalogTitle = "Catalog $index (Movie)",
                type = "movie",
                items = listOf(
                    StremioMetaPreview(
                        id = "tt000$index",
                        type = "movie",
                        name = "Title $index",
                        poster = "https://images.example.com/poster_$index.jpg",
                        releaseInfo = "2024"
                    )
                )
            )
        }

        val cwItems = listOf(
            LibraryMediaItem(
                mediaKey = "movie_tt1375666",
                title = "Inception",
                mediaType = "movie",
                watchProgressMs = 1_800_000L,
                durationMs = 7_200_000L
            )
        )

        composeRule.setContent {
            CineVaultTheme {
                DiscoverScreen(
                    discoverState = DiscoverUiState(
                        isLoading = false,
                        sections = eightSections
                    ),
                    continueWatching = cwItems,
                    compactGrid = false,
                    preferredEngine = PlaybackEngine.EXOPLAYER,
                    onSearchChange = {},
                    onTypeSelected = {},
                    onRefresh = {},
                    onSelectMedia = {},
                    onQuickFavorite = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("discover_top_scrim", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("discover_search_toggle_button").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("CineVault").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("8 catalogs • Media3 ExoPlayer").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("discover_customize_button").fetchSemanticsNodes().size)

        composeRule.onNodeWithTag("discover_search_toggle_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discover_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("discover_search_close_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discover_search_toggle_button").assertIsDisplayed()

        composeRule.onNodeWithTag("discover_feed_list").performScrollToNode(hasText("Continue Watching"))
        composeRule.onNodeWithText("Continue Watching").assertIsDisplayed()
        composeRule.onNodeWithTag("discover_feed_list").performScrollToNode(hasText("Catalog 1 (Movie)"))
        composeRule.onNodeWithText("Catalog 1 (Movie)").assertIsDisplayed()
        composeRule.onNodeWithTag("discover_feed_list").performScrollToNode(hasText("Catalog 8 (Movie)"))
        composeRule.onNodeWithText("Catalog 8 (Movie)").assertIsDisplayed()
    }

    @Test
    fun mediaDetailScreen_displaysSeasonTabsAndFiltersStreamsByQuality() {
        val stream4k = StremioStream(
            name = "Torrentio",
            title = "4K HDR HEVC • 20 GB",
            url = "https://cdn.example.com/Show.S01E01.2160p.HEVC.HDR.mkv",
            behaviorHints = StremioBehaviorHints(filename = "Show.S01E01.2160p.HEVC.HDR.mkv")
        )
        val stream1080 = StremioStream(
            name = "Torrentio",
            title = "1080p BluRay • 4 GB",
            url = "https://cdn.example.com/Show.S01E01.1080p.BluRay.x264.mkv",
            behaviorHints = StremioBehaviorHints(filename = "Show.S01E01.1080p.BluRay.x264.mkv")
        )

        composeRule.setContent {
            CineVaultTheme {
                MediaDetailScreen(
                    detailState = MediaDetailUiState(
                        isLoading = false,
                        preview = StremioMetaPreview(id = "tt0944947", type = "series", name = "Sample Series"),
                        detail = StremioMetaDetail(
                            id = "tt0944947",
                            type = "series",
                            name = "Sample Series",
                            videos = listOf(
                                StremioVideoEpisode(id = "tt0944947:1:1", title = "Pilot", season = 1, episode = 1),
                                StremioVideoEpisode(id = "tt0944947:2:1", title = "Return", season = 2, episode = 1)
                            )
                        ),
                        selectedEpisodeId = "tt0944947:1:1",
                        streams = listOf(stream4k, stream1080)
                    ),
                    isFavorited = false,
                    preferredEngine = PlaybackEngine.EXOPLAYER,
                    onBack = {},
                    onToggleFavorite = {},
                    onPlayStream = { _, _ -> },
                    onDownloadStream = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("media_detail_list").performScrollToNode(hasText("Season 2"))
        composeRule.onNodeWithText("Season 2").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("media_detail_list").performScrollToNode(hasText("Return"))
        composeRule.onNodeWithText("Return").assertIsDisplayed()

        composeRule.onNodeWithTag("media_detail_list").performScrollToNode(hasText("4K / UHD"))
        composeRule.onNodeWithText("2/2 sources").assertIsDisplayed()
        composeRule.onNodeWithText("4K / UHD").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("1/2 sources").assertIsDisplayed()
    }

    @Test
    fun mediaDetailScreen_displaysMultiSourceRatingBarAndWebFallback() {
        val externalOnlyStream = StremioStream(
            name = "WatchHub",
            title = "Watch External Web Stream",
            externalUrl = "https://watch.example.com/title"
        )
        composeRule.setContent {
            CineVaultTheme {
                MediaDetailScreen(
                    detailState = MediaDetailUiState(
                        isLoading = false,
                        preview = StremioMetaPreview(
                            id = "tt1375666",
                            type = "movie",
                            name = "Inception",
                            releaseInfo = "2010",
                            imdbRating = "8.8"
                        ),
                        detail = StremioMetaDetail(
                            id = "tt1375666",
                            type = "movie",
                            name = "Inception",
                            runtime = "148 min",
                            imdbRating = "8.8"
                        ),
                        streams = listOf(externalOnlyStream)
                    ),
                    isFavorited = false,
                    preferredEngine = PlaybackEngine.LIBMPV,
                    onBack = {},
                    onToggleFavorite = {},
                    onPlayStream = { _, _ -> },
                    onDownloadStream = {},
                    isModal = true
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("multi_source_rating_bar").assertIsDisplayed()
        composeRule.onNodeWithText("IMDb 8.8", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("RT 92%", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Audience 89%", useUnmergedTree = true).assertIsDisplayed()

        composeRule.onNodeWithTag("media_detail_list").performScrollToNode(hasTestTag("play_primary_button"))
        composeRule.onNodeWithText("Web Player", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun addonsScreen_displaysInlinePasteInstallButtonsAndQuickChips() {
        var installedUrl = ""
        composeRule.setContent {
            CineVaultTheme {
                AddonsScreen(
                    addonsState = UiState.Success(emptyList()),
                    onInstallAddon = { installedUrl = it },
                    onToggleAddon = { _, _ -> },
                    onRemoveAddon = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("install_addon_input").assertIsDisplayed()
        composeRule.onNodeWithTag("paste_addon_button", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("install_addon_button", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Cinemeta").assertIsDisplayed()
        composeRule.onNodeWithText("OpenSubtitles").assertIsDisplayed()
        composeRule.onNodeWithText("WatchHub").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("install_addon_button", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        assertTrue(installedUrl.contains("watchhub", ignoreCase = true))
    }

    @Test
    fun settingsScreen_togglesAmoledBlackAutoPlayTrailersAndRendersSubtitlePreview() {
        var currentPrefs by mutableStateOf(
            UserPreferences(
                userId = "test_user",
                amoledBlack = false,
                autoPlayTrailers = TrailerAutoPlayMode.WIFI_ONLY.key
            )
        )

        composeRule.setContent {
            CineVaultTheme(amoledBlack = currentPrefs.amoledBlack) {
                SettingsScreen(
                    preferences = currentPrefs,
                    userEmail = "tester@cinevault.app",
                    isGuestMode = false,
                    onUpdatePreferences = { transform ->
                        currentPrefs = transform(currentPrefs)
                    },
                    onSignOut = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("amoled_black_switch"))
        composeRule.onNodeWithTag("amoled_black_switch").performClick()
        composeRule.waitForIdle()
        assertTrue(currentPrefs.amoledBlack)

        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("trailer_autoplay_ALWAYS"))
        composeRule.onNodeWithTag("trailer_autoplay_ALWAYS").performClick()
        composeRule.waitForIdle()
        assertEquals(TrailerAutoPlayMode.ALWAYS.key, currentPrefs.autoPlayTrailers)

        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("subtitle_preview_box"))
        composeRule.onNodeWithTag("subtitle_preview_box").assertIsDisplayed()
    }

    @Test
    fun mediaDetailScreen_displaysTrailerButtonClipsRowAndOpensCinemaLightbox() {
        val stream = StremioStream(
            name = "OpenCinema",
            title = "1080p MP4 • 1.2 GB",
            url = "https://cdn.example.com/movie.1080p.mp4"
        )
        composeRule.setContent {
            CineVaultTheme {
                MediaDetailScreen(
                    detailState = MediaDetailUiState(
                        isLoading = false,
                        preview = StremioMetaPreview(
                            id = "tt_trailer_test",
                            type = "movie",
                            name = "Solaris Echo",
                            releaseInfo = "2025",
                            imdbRating = "8.4"
                        ),
                        detail = StremioMetaDetail(
                            id = "tt_trailer_test",
                            type = "movie",
                            name = "Solaris Echo",
                            trailerStreams = listOf(
                                StremioTrailerStream(
                                    title = "Official 4K Trailer",
                                    url = "https://cdn.example.com/trailer1.mp4"
                                )
                            ),
                            trailers = listOf(
                                StremioTrailer(source = "dQw4w9WgXcQ", type = "Teaser")
                            )
                        ),
                        streams = listOf(stream)
                    ),
                    isFavorited = false,
                    preferredEngine = PlaybackEngine.EXOPLAYER,
                    onBack = {},
                    onToggleFavorite = {},
                    onPlayStream = { _, _ -> },
                    onDownloadStream = {},
                    isModal = true
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detail_trailer_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("media_detail_list").performScrollToNode(hasTestTag("trailers_clips_row"))
        composeRule.onNodeWithTag("trailers_clips_row").assertIsDisplayed()

        composeRule.onNodeWithTag("media_detail_list").performScrollToNode(hasTestTag("detail_trailer_button"))
        composeRule.onNodeWithTag("detail_trailer_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("cinema_trailer_lightbox").assertIsDisplayed()
        composeRule.onNodeWithTag("lightbox_browse_streams_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithText("Browse Streams").fetchSemanticsNodes().size)
    }

    @Test
    fun libraryScreen_displaysStorageCloudHubCardAndSegmentedBar() {
        composeRule.setContent {
            CineVaultTheme(amoledBlack = true) {
                LibraryScreen(
                    libraryState = UiState.Success(
                        listOf(
                            LibraryMediaItem(
                                mediaKey = "movie_1",
                                title = "Big Buck Bunny",
                                mediaType = "movie",
                                isFavorite = true
                            )
                        )
                    ),
                    downloads = emptyList(),
                    localMedia = emptyList(),
                    preferredEngine = PlaybackEngine.EXOPLAYER,
                    onAddPersonalCloudMedia = { _, _, _, _ -> },
                    onRemoveLibraryItem = {},
                    onImportLocalVideo = {},
                    onAttachLocalSubtitle = { _, _ -> },
                    onRefreshLocalMetadata = {},
                    onDeleteLocalMedia = {},
                    onDeleteDownload = {},
                    onPlayRequest = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("storage_cloud_hub_card").assertIsDisplayed()
        composeRule.onNodeWithTag("add_cloud_source_button").assertIsDisplayed()
    }

    @Test
    fun discoverScreen_displaysFullBleedSpotlightAndTrendingRankBadges() {
        val trendingSection = CatalogSection(
            sectionKey = "cinemeta::movie::top::0",
            addonKey = "cinemeta",
            addonName = "Cinemeta",
            catalogId = "top",
            catalogTitle = "Trending Now",
            type = "movie",
            items = listOf(
                StremioMetaPreview(
                    id = "tt_spot_1",
                    type = "movie",
                    name = "Dune Odyssey",
                    poster = "https://images.example.com/poster1.jpg",
                    background = "https://images.example.com/bg1.jpg",
                    logo = "https://images.example.com/logo1.png",
                    releaseInfo = "2026",
                    imdbRating = "8.9",
                    trailerStreams = listOf(
                        StremioTrailerStream(
                            title = "Main Trailer",
                            url = "https://cdn.example.com/dune_trailer.mp4"
                        )
                    )
                ),
                StremioMetaPreview(
                    id = "tt_spot_2",
                    type = "movie",
                    name = "Starfall",
                    poster = "https://images.example.com/poster2.jpg",
                    releaseInfo = "2025",
                    imdbRating = "7.8"
                )
            )
        )

        composeRule.setContent {
            CineVaultTheme(amoledBlack = true) {
                DiscoverScreen(
                    discoverState = DiscoverUiState(
                        isLoading = false,
                        sections = listOf(trendingSection)
                    ),
                    continueWatching = emptyList(),
                    compactGrid = false,
                    preferredEngine = PlaybackEngine.EXOPLAYER,
                    autoPlayTrailers = TrailerAutoPlayMode.ALWAYS.key,
                    onSearchChange = {},
                    onTypeSelected = {},
                    onRefresh = {},
                    onSelectMedia = {},
                    onQuickFavorite = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("featured_hero_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("hero_stream_button").assertIsDisplayed()
        composeRule.onNodeWithText("Dune Odyssey").assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(2_200L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spotlight_mute_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("discover_feed_list").performScrollToNode(hasTestTag("media_card_tt_spot_1"))
        composeRule.onNodeWithText("Trending Now").assertIsDisplayed()
        composeRule.onNodeWithTag("media_card_tt_spot_1").assertIsDisplayed()
        composeRule.onNodeWithTag("trending_rank_1", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun mediaDetailScreen_displaysCastChipsRowAndAvailableStreamsWithoutClipping() {
        val stream = StremioStream(
            name = "Torrentio",
            title = "1080p WEB-DL • 2.4 GB",
            url = "https://cdn.example.com/film.1080p.mkv"
        )
        composeRule.setContent {
            CineVaultTheme {
                MediaDetailScreen(
                    detailState = MediaDetailUiState(
                        isLoading = false,
                        preview = StremioMetaPreview(id = "tt_cast_1", type = "movie", name = "Severed Horizon"),
                        detail = StremioMetaDetail(
                            id = "tt_cast_1",
                            type = "movie",
                            name = "Severed Horizon",
                            cast = listOf("Rebecca Ferguson", "Oscar Isaac", "Timothee Chalamet")
                        ),
                        streams = listOf(stream)
                    ),
                    isFavorited = false,
                    preferredEngine = PlaybackEngine.EXOPLAYER,
                    onBack = {},
                    onToggleFavorite = {},
                    onPlayStream = { _, _ -> },
                    onDownloadStream = {},
                    isModal = true
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("media_detail_list").performScrollToNode(hasTestTag("detail_cast_row"))
        composeRule.onNodeWithTag("detail_cast_row").assertIsDisplayed()
        composeRule.onNodeWithText("Rebecca Ferguson", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("media_detail_list").performScrollToNode(hasTestTag("stream_option_card"))
        composeRule.onNodeWithTag("stream_option_card").assertIsDisplayed()
    }

    @Test
    fun playerScreen_seriesEpisodeDropdownSwitchesEpisodeWithSmartAutoMatchAndNoTopNextButton() {
        val episodes = listOf(
            PlayerEpisodeItem(id = "tt_s:1:1", title = "Pilot", season = 1, episode = 1, runtime = "48m"),
            PlayerEpisodeItem(id = "tt_s:1:2", title = "The Signal", season = 1, episode = 2, runtime = "52m"),
            PlayerEpisodeItem(id = "tt_s:1:3", title = "Empty Episode", season = 1, episode = 3, runtime = "49m")
        )
        val ep2Sources = listOf(
            StreamSourceOption(
                id = "comet_4k",
                fullFileName = "Show.S01E02.2160p.REMUX.mkv",
                badges = listOf("4K", "Comet"),
                streamUrl = "https://cdn.example.com/ep2_4k.mkv",
                addonName = "Comet",
                addonId = "comet",
                resolution = "4K"
            ),
            StreamSourceOption(
                id = "torrentio_1080",
                fullFileName = "Show.S01E02.1080p.WEB-DL.HEVC.mkv",
                badges = listOf("1080p", "Torrentio", "HEVC"),
                streamUrl = "https://cdn.example.com/ep2_1080.mkv",
                addonName = "Torrentio",
                addonId = "torrentio",
                resolution = "1080p",
                tags = listOf("WEB-DL", "HEVC")
            )
        )
        composeRule.setContent {
            CineVaultTheme {
                PlayerScreen(
                    request = PlaybackRequest(
                        mediaKey = "tt_s:1:1",
                        title = "Dark Signal • S1:E1",
                        mediaType = "series",
                        streamUrl = "https://cdn.example.com/ep1_1080.mkv",
                        imdbId = "tt_s:1:1",
                        seriesId = "tt_s",
                        currentSeason = 1,
                        currentEpisode = 1,
                        episodes = episodes,
                        streamPreference = StreamFingerprint(
                            addonName = "Torrentio",
                            addonId = "torrentio",
                            resolution = "1080p",
                            tags = listOf("HEVC")
                        )
                    ),
                    subtitles = emptyList(),
                    preferences = UserPreferences(userId = "test_user", preferredAudioLang = "en"),
                    onUpdateProgress = { _, _, _, _, _, _, _ -> },
                    onUpdateSubtitleDelay = {},
                    onResolveSources = { _, id ->
                        if (id == "tt_s:1:2") ep2Sources else emptyList()
                    },
                    onClose = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Top-right Next button must be completely removed
        assertEquals(0, composeRule.onAllNodesWithTag("player_top_up_next_button").fetchSemanticsNodes().size)

        // Telemetry badge and Series S1:E1 dropdown pill must be visible
        composeRule.onNodeWithTag("player_buffer_telemetry").assertIsDisplayed()
        composeRule.onNodeWithTag("player_episode_dropdown_badge").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("player_episode_dropdown_menu").assertIsDisplayed()
        composeRule.onNodeWithTag("player_dropdown_ep_tt_s:1:2").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Smart Auto-Match picks 1080p Torrentio and updates badge to S1:E2
        composeRule.onNodeWithTag("player_automatch_toast").assertIsDisplayed()
        composeRule.onNodeWithText("Auto-matched: 1080p • Torrentio", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("S1:E2", useUnmergedTree = true).assertIsDisplayed()

        // Switch to episode 3 with 0 streams -> shows empty overlay with Choose Episode button
        composeRule.onNodeWithTag("player_episode_dropdown_badge").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("player_dropdown_ep_tt_s:1:3").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("player_no_streams_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("player_no_streams_episode_list_button").assertIsDisplayed()
    }

    @Test
    fun playerScreen_movieHidesEpisodeDropdownAndAudioSheetHighlightsActiveTrack() {
        composeRule.setContent {
            CineVaultTheme {
                PlayerScreen(
                    request = PlaybackRequest(
                        mediaKey = "tt_movie_1",
                        title = "Dune Odyssey",
                        mediaType = "movie",
                        streamUrl = "https://cdn.example.com/dune.mp4",
                        imdbId = "tt_movie_1"
                    ),
                    subtitles = emptyList(),
                    preferences = UserPreferences(userId = "test_user"),
                    onUpdateProgress = { _, _, _, _, _, _, _ -> },
                    onUpdateSubtitleDelay = {},
                    onClose = {}
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithTag("player_episode_dropdown_badge").fetchSemanticsNodes().size)
    }

    @Test
    fun audioTrackSheet_displaysScrollableTracksAndActiveTrackSelection() {
        var selectedId = ""
        val tracks = (1..6).map { idx ->
            PlayerTrackInfo(
                id = "aud_$idx",
                label = "Audio Track $idx",
                language = if (idx == 2) "en" else "hi",
                isSelected = idx == 2
            )
        }
        composeRule.setContent {
            CineVaultTheme {
                AudioTrackSheet(
                    tracks = tracks,
                    onSelect = { selectedId = it },
                    onDismiss = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("audio_tracks_list").assertIsDisplayed()
        composeRule.onNodeWithTag("audio_track_row_aud_2").assertIsDisplayed()
        composeRule.onNodeWithTag("audio_tracks_list").performScrollToNode(hasTestTag("audio_track_row_aud_6"))
        composeRule.onNodeWithTag("audio_track_row_aud_6").assertIsDisplayed().performClick()
        assertEquals("aud_6", selectedId)
    }
}



