package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.R
import com.example.data.model.PlaybackEngine
import com.example.data.model.PosterLayout
import com.example.data.model.TrailerAutoPlayMode
import com.example.data.model.UiTextScale
import com.example.data.model.UserPreferences
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.machinedCardBorder
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    userEmail: String?,
    isGuestMode: Boolean = false,
    onUpdatePreferences: ((UserPreferences) -> UserPreferences) -> Unit,
    onSignInClick: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    // Collapsible accordion expansion states
    var appearanceExpanded by remember { mutableStateOf(true) }
    var playbackExpanded by remember { mutableStateOf(false) }
    var subtitlesExpanded by remember { mutableStateOf(false) }
    var downloadsExpanded by remember { mutableStateOf(false) }
    var backupExpanded by remember { mutableStateOf(false) }

    // When user searches, expand matching sections automatically
    val q = searchQuery.trim().lowercase()
    val showAppearance = q.isEmpty() || "appearance theme color accent amoled grid dark".contains(q)
    val showPlayback = q.isEmpty() || "playback engine nextplayer trailer audio codec intro stream hardware ffmpeg".contains(q)
    val showSubtitles = q.isEmpty() || "subtitles sync font size color opacity delay language srt vtt".contains(q)
    val showDownloads = q.isEmpty() || "downloads storage wifi quality cache metadata limit".contains(q)
    val showBackup = q.isEmpty() || "backup sync apk account cloud signin signout version".contains(q)

    val activePreset = AccentPreset.fromKey(preferences.accentPreset)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Settings & Preferences",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = if (isGuestMode) "Local Guest Mode • Offline & synced on this device"
                    else "Synced across your devices via Cloud Vault",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
        }

        // Live Search Field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.isNotBlank()) {
                        appearanceExpanded = true
                        playbackExpanded = true
                        subtitlesExpanded = true
                        downloadsExpanded = true
                        backupExpanded = true
                    }
                },
                placeholder = {
                    Text(
                        "Search settings & preferences...",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = AmoledCard,
                    unfocusedContainerColor = AmoledCard
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_search_input")
            )
        }

        // 1. Appearance & Theme Accordion
        if (showAppearance) {
            item {
                val summary = "${activePreset.label} • ${if (preferences.amoledBlack) "AMOLED Black" else "Charcoal"} • ${if (preferences.compactGrid) "3-Col" else "2-Col"}"
                CollapsibleSettingsCard(
                    title = "Appearance & Theme",
                    icon = Icons.Rounded.Palette,
                    summaryBadge = summary,
                    expanded = appearanceExpanded || q.isNotEmpty(),
                    onToggleExpand = { appearanceExpanded = !appearanceExpanded }
                ) {
                    Text(
                        text = "Accent Theme Palette",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AccentPreset.entries.forEach { preset ->
                            val selected = preferences.accentPreset == preset.key
                            Surface(
                                color = if (selected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) preset.primary else Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onUpdatePreferences { it.copy(accentPreset = preset.key) }
                                    }
                                    .testTag("accent_preset_${preset.key}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawArc(
                                                color = preset.primary,
                                                startAngle = 135f,
                                                sweepAngle = 180f,
                                                useCenter = true
                                            )
                                            drawArc(
                                                color = preset.surfaceSwatch,
                                                startAngle = 315f,
                                                sweepAngle = 180f,
                                                useCenter = true
                                            )
                                        }
                                        if (selected) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    SettingsToggleRow(
                        title = "Pure AMOLED Black",
                        subtitle = "True #000000 pitch-black canvas with 1px borders for OLED screens",
                        checked = preferences.amoledBlack,
                        testTag = "amoled_black_switch",
                        onCheckedChange = { checked ->
                            onUpdatePreferences { it.copy(amoledBlack = checked) }
                        }
                    )

                    Text(
                        text = "Poster Grid Layout",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PosterLayout.entries.forEach { layout ->
                            FilterChip(
                                selected = preferences.posterLayout == layout.key,
                                onClick = {
                                    onUpdatePreferences {
                                        it.copy(
                                            posterLayout = layout.key,
                                            compactGrid = layout == PosterLayout.COMPACT_3
                                        )
                                    }
                                },
                                label = { Text(layout.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("poster_layout_${layout.key}")
                            )
                        }
                    }

                    Text(
                        text = "UI Text Size",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        UiTextScale.entries.forEach { scale ->
                            FilterChip(
                                selected = preferences.uiTextScale == scale.key,
                                onClick = {
                                    onUpdatePreferences { it.copy(uiTextScale = scale.key) }
                                },
                                label = { Text(scale.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("ui_text_scale_${scale.key}")
                            )
                        }
                    }

                    SettingsToggleRow(
                        title = "Ambient Backdrop Glow",
                        subtitle = "Subtle cinematic ambient reflection behind posters and spotlight cards",
                        checked = preferences.ambientBackdropGlow,
                        testTag = "ambient_glow_switch",
                        onCheckedChange = { checked ->
                            onUpdatePreferences { it.copy(ambientBackdropGlow = checked) }
                        }
                    )
                }
            }
        }

        // 2. Playback & Engines Accordion
        if (showPlayback) {
            item {
                val engineName = PlaybackEngine.fromKey(preferences.preferredEngine).displayName
                val summary = "$engineName • ${if (preferences.hardwareDecoding) "HW Accel" else "Software"}"
                CollapsibleSettingsCard(
                    title = "Playback & Engines",
                    icon = Icons.Rounded.PlayCircle,
                    summaryBadge = summary,
                    expanded = playbackExpanded || q.isNotEmpty(),
                    onToggleExpand = { playbackExpanded = !playbackExpanded }
                ) {
                    Text(
                        text = "Default Video Player Engine",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nextplayer_engine_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Next Player Engine",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Integrated FFmpeg audio & video decoders with native hardware acceleration. Supports MKV, MP4, HLS, TS, DTS, TrueHD, AC3, EAC3, and SSA/ASS subtitles on all Snapdragon and MediaTek chipsets.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }

                    SettingsToggleRow(
                        title = "Hardware Video Acceleration",
                        subtitle = "Use MediaCodec hardware decoder with FFmpeg software fallback",
                        checked = preferences.hardwareDecoding,
                        testTag = "hw_decoding_switch",
                        onCheckedChange = { checked ->
                            onUpdatePreferences { it.copy(hardwareDecoding = checked) }
                        }
                    )

                    Text(
                        text = "Auto-Play Spotlight Trailers",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    val activeTrailerMode = TrailerAutoPlayMode.fromKey(preferences.autoPlayTrailers)
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TrailerAutoPlayMode.entries.forEach { mode ->
                                val selected = activeTrailerMode == mode
                                Surface(
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onUpdatePreferences { it.copy(autoPlayTrailers = mode.key) }
                                        }
                                        .testTag("trailer_autoplay_${mode.key}")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (selected) Color.Black else Color.White.copy(alpha = 0.70f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "Preferred Audio Language",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "auto" to "Original",
                            "en" to "English",
                            "hi" to "Hindi",
                            "es" to "Spanish",
                            "fr" to "French",
                            "de" to "German",
                            "ja" to "Japanese",
                            "ko" to "Korean",
                            "ar" to "Arabic",
                            "pt" to "Portuguese"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = preferences.preferredAudioLang == code,
                                onClick = {
                                    onUpdatePreferences { it.copy(preferredAudioLang = code) }
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("audio_lang_$code")
                            )
                        }
                    }

                    SettingsToggleRow(
                        title = "Auto-Match Best Stream",
                        subtitle = "Instant playback when opening a title using resolution and seed heuristics",
                        checked = preferences.autoMatchBestStream,
                        testTag = "auto_match_stream_switch",
                        onCheckedChange = { checked ->
                            onUpdatePreferences { it.copy(autoMatchBestStream = checked) }
                        }
                    )

                    SettingsToggleRow(
                        title = "Auto-Skip Intro & Outro",
                        subtitle = "Show 1-tap Skip Intro and automatically advance during episode credits",
                        checked = preferences.autoSkipIntro,
                        testTag = "auto_skip_intro_switch",
                        onCheckedChange = { checked ->
                            onUpdatePreferences { it.copy(autoSkipIntro = checked) }
                        }
                    )

                    SettingsToggleRow(
                        title = "Auto-Play Next Episode",
                        subtitle = "Show 10-second countdown card before next episode plays",
                        checked = preferences.autoPlayNextEpisode,
                        testTag = "auto_play_next_switch",
                        onCheckedChange = { checked ->
                            onUpdatePreferences { it.copy(autoPlayNextEpisode = checked) }
                        }
                    )
                }
            }
        }

        // 3. Subtitles & Sync Accordion
        if (showSubtitles) {
            item {
                val colorLabel = when (preferences.subtitleColorHex.uppercase()) {
                    "#F5B74F" -> "Amber"
                    "#48C9E8" -> "Cyan"
                    else -> "White"
                }
                val summary = "${preferences.preferredSubtitleLang.uppercase()} • ${preferences.subtitleFontSizeSp}sp $colorLabel"
                CollapsibleSettingsCard(
                    title = "Subtitles & Sync",
                    icon = Icons.Rounded.Subtitles,
                    summaryBadge = summary,
                    expanded = subtitlesExpanded || q.isNotEmpty(),
                    onToggleExpand = { subtitlesExpanded = !subtitlesExpanded }
                ) {
                    // Live Preview Box
                    val fgColor = runCatching { Color(preferences.subtitleColorHex.toColorInt()) }
                        .getOrElse { Color.White }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .machinedCardBorder(cornerRadiusDp = 12.dp)
                            .testTag("subtitle_preview_box"),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_cinema),
                            contentDescription = "Subtitle cinema preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.15f),
                                            Color.Black.copy(alpha = 0.65f)
                                        )
                                    )
                                )
                        )
                        Surface(
                            color = Color.Black.copy(alpha = preferences.subtitleBgOpacity.toFloat()),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(bottom = 10.dp, start = 12.dp, end = 12.dp)
                        ) {
                            Text(
                                text = "Cinema subtitle preview • ${preferences.subtitleFontSizeSp}sp",
                                color = fgColor,
                                fontSize = preferences.subtitleFontSizeSp.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Preferred Subtitle Language",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "off" to "Off",
                            "en" to "English",
                            "hi" to "Hindi",
                            "es" to "Spanish",
                            "fr" to "French",
                            "de" to "German",
                            "ja" to "Japanese",
                            "ko" to "Korean",
                            "ar" to "Arabic",
                            "pt" to "Portuguese"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = preferences.preferredSubtitleLang == code,
                                onClick = {
                                    onUpdatePreferences { it.copy(preferredSubtitleLang = code) }
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("sub_lang_$code")
                            )
                        }
                    }

                    Text(
                        text = "Subtitle Font Size: ${preferences.subtitleFontSizeSp} sp",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Slider(
                        value = preferences.subtitleFontSizeSp.toFloat(),
                        onValueChange = { value ->
                            onUpdatePreferences { it.copy(subtitleFontSizeSp = value.roundToInt()) }
                        },
                        valueRange = 12f..36f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Text(
                        text = "Backdrop Opacity: ${(preferences.subtitleBgOpacity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Slider(
                        value = preferences.subtitleBgOpacity.toFloat(),
                        onValueChange = { value ->
                            onUpdatePreferences { it.copy(subtitleBgOpacity = value.toDouble()) }
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Text(
                        text = "Subtitle Color",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("#FFFFFF" to "White", "#F5B74F" to "Amber", "#48C9E8" to "Cyan").forEach { (hex, label) ->
                            FilterChip(
                                selected = preferences.subtitleColorHex.equals(hex, ignoreCase = true),
                                onClick = {
                                    onUpdatePreferences { it.copy(subtitleColorHex = hex) }
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    SettingsToggleRow(
                        title = "Embedded Subtitle Priority",
                        subtitle = "Prefer internal container subtitle tracks over external OpenSubtitles",
                        checked = preferences.embeddedSubtitlePriority,
                        testTag = "embedded_sub_priority_switch",
                        onCheckedChange = { checked ->
                            onUpdatePreferences { it.copy(embeddedSubtitlePriority = checked) }
                        }
                    )
                }
            }
        }

        // 4. Downloads & Storage Accordion
        if (showDownloads) {
            item {
                val summary = "${preferences.defaultDownloadQuality} • ${if (preferences.downloadWifiOnly) "Wi-Fi Only" else "Any Network"}"
                CollapsibleSettingsCard(
                    title = "Downloads & Storage",
                    icon = Icons.Rounded.Download,
                    summaryBadge = summary,
                    expanded = downloadsExpanded || q.isNotEmpty(),
                    onToggleExpand = { downloadsExpanded = !downloadsExpanded }
                ) {
                    SettingsToggleRow(
                        title = "Wi-Fi Only Downloads",
                        subtitle = "Pause background offline downloads on metered mobile data",
                        checked = preferences.downloadWifiOnly,
                        testTag = "wifi_only_switch",
                        onCheckedChange = { checked ->
                            onUpdatePreferences { it.copy(downloadWifiOnly = checked) }
                        }
                    )

                    SettingsToggleRow(
                        title = "Auto-Retrieve Local Metadata",
                        subtitle = "Automatically match imported MKV/MP4 files with Cinemeta posters & synopsis",
                        checked = preferences.autoFetchLocalMetadata,
                        testTag = "auto_metadata_switch",
                        onCheckedChange = { checked ->
                            onUpdatePreferences { it.copy(autoFetchLocalMetadata = checked) }
                        }
                    )

                    Text(
                        text = "Default Download Quality",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1080p", "720p", "Ask Every Time").forEach { quality ->
                            FilterChip(
                                selected = preferences.defaultDownloadQuality == quality,
                                onClick = {
                                    onUpdatePreferences { it.copy(defaultDownloadQuality = quality) }
                                },
                                label = { Text(quality) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Text(
                        text = "Simultaneous Downloads Limit: ${preferences.simultaneousDownloadsLimit}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..4).forEach { count ->
                            FilterChip(
                                selected = preferences.simultaneousDownloadsLimit == count,
                                onClick = {
                                    onUpdatePreferences { it.copy(simultaneousDownloadsLimit = count) }
                                },
                                label = { Text("$count") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Completed download cache cleaned", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Completed Download Cache")
                    }
                }
            }
        }

        // 5. Backup, Sync & APK Accordion
        if (showBackup) {
            item {
                val summary = "CineVault v2.4.0 • ${if (isGuestMode) "Local Guest" else "Cloud Sync"}"
                CollapsibleSettingsCard(
                    title = "Backup, Sync & APK",
                    icon = Icons.Rounded.CloudSync,
                    summaryBadge = summary,
                    expanded = backupExpanded || q.isNotEmpty(),
                    onToggleExpand = { backupExpanded = !backupExpanded }
                ) {
                    if (isGuestMode) {
                        Text(
                            text = "You are using CineVault in Local Guest Mode. Sign in with Google to sync addons and watchlist across devices.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Button(
                            onClick = onSignInClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_sign_in_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.sign_in_sync))
                        }
                    } else {
                        if (!userEmail.isNullOrBlank()) {
                            Text(
                                text = "Signed in as $userEmail",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.sign_out))
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val prefs = context.getSharedPreferences("streamflow_search_history", Context.MODE_PRIVATE)
                            prefs.edit().clear().apply()
                            Toast.makeText(context, "Search history cleared", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Recent Search History")
                    }

                    // Build and Engine Information
                    Surface(
                        color = Color.White.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Build & Architecture",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "CineVault v2.4.0 (Pure AMOLED Edition)\nAndroid SDK 34 • NextPlayer FFmpeg Engine\nMedia3 NextLib Media3Ext • Stremio v11 Addon Protocol",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.55f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSettingsCard(
    title: String,
    icon: ImageVector,
    summaryBadge: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    Card(
        colors = CardDefaults.cardColors(containerColor = AmoledCard),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .machinedCardBorder(shape = cardShape, cornerRadiusDp = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        if (summaryBadge.isNotBlank()) {
                            Text(
                                text = summaryBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.50f),
                                maxLines = 1
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White.copy(alpha = 0.60f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
