package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.R
import com.example.data.model.PlaybackEngine
import com.example.data.model.TrailerAutoPlayMode
import com.example.data.model.UserPreferences
import com.example.ui.theme.AccentPreset
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "Interface & Playback",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isGuestMode) {
                            "Local Guest Mode — sign in anytime to sync across devices"
                        } else {
                            "Settings sync automatically across signed-in devices"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Interface Customization
        item {
            SettingsCard(title = "Interface Appearance") {
                Text(
                    text = "Accent Theme Palette",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccentPreset.entries.forEach { preset ->
                        val selected = preferences.accentPreset == preset.key
                        Surface(
                            color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) preset.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                ToggleRow(
                    title = "Compact Poster Grid",
                    subtitle = "Display denser cards on Discover & Vault",
                    checked = preferences.compactGrid,
                    testTag = "compact_grid_switch",
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(compactGrid = checked) }
                    }
                )

                ToggleRow(
                    title = "Pure AMOLED Black",
                    subtitle = "True #000000 pitch-black canvas for OLED screens",
                    checked = preferences.amoledBlack,
                    testTag = "amoled_black_switch",
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(amoledBlack = checked) }
                    }
                )
            }
        }

        // Playback Engine Settings
        item {
            SettingsCard(title = "Video Playback Engines") {
                Text(
                    text = "Default Player Engine",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = preferences.preferredEngine == PlaybackEngine.EXOPLAYER.key,
                        onClick = {
                            onUpdatePreferences { it.copy(preferredEngine = PlaybackEngine.EXOPLAYER.key) }
                        },
                        label = { Text("Media3 ExoPlayer") },
                        modifier = Modifier.testTag("engine_exoplayer_chip")
                    )
                    FilterChip(
                        selected = preferences.preferredEngine == PlaybackEngine.LIBMPV.key,
                        onClick = {
                            onUpdatePreferences { it.copy(preferredEngine = PlaybackEngine.LIBMPV.key) }
                        },
                        label = { Text("libmpv (MPV)") },
                        modifier = Modifier.testTag("engine_libmpv_chip")
                    )
                }

                Text(
                    text = "Auto-Play Spotlight Trailers",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val activeTrailerMode = TrailerAutoPlayMode.fromKey(preferences.autoPlayTrailers)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .machinedCardBorder(shape = RoundedCornerShape(14.dp), cornerRadiusDp = 14.dp)
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
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onUpdatePreferences { it.copy(autoPlayTrailers = mode.key) }
                                    }
                                    .testTag("trailer_autoplay_${mode.key}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Text(text = "Preferred Audio Language", style = MaterialTheme.typography.titleMedium)
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
                            label = { Text(label) },
                            modifier = Modifier.testTag("audio_lang_$code")
                        )
                    }
                }

                ToggleRow(
                    title = "Hardware Video Decoding",
                    subtitle = "Use MediaCodec / mediacodec-copy for 4K HDR efficiency",
                    checked = preferences.hardwareDecoding,
                    testTag = "hw_decoding_switch",
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(hardwareDecoding = checked) }
                    }
                )
            }
        }

        // Subtitle Appearance Studio
        item {
            SettingsCard(title = "Subtitle Styling & Sync") {
                // Live Preview over Sample Cinematic Frame
                val fgColor = runCatching { Color(preferences.subtitleColorHex.toColorInt()) }
                    .getOrElse { Color.White }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .machinedCardBorder(shape = RoundedCornerShape(14.dp), cornerRadiusDp = 14.dp)
                        .testTag("subtitle_preview_box"),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_cinema),
                        contentDescription = "Subtitle cinema frame preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.12f),
                                        Color.Black.copy(alpha = 0.55f)
                                    )
                                )
                            )
                    )
                    Surface(
                        color = Color.Black.copy(alpha = preferences.subtitleBgOpacity.toFloat()),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
                    ) {
                        Text(
                            text = "Cinema subtitle preview • ${preferences.subtitleFontSizeSp}sp",
                            color = fgColor,
                            fontSize = preferences.subtitleFontSizeSp.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Font Size: ${preferences.subtitleFontSizeSp} sp",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = preferences.subtitleFontSizeSp.toFloat(),
                    onValueChange = { value ->
                        onUpdatePreferences { it.copy(subtitleFontSizeSp = value.roundToInt()) }
                    },
                    valueRange = 12f..36f
                )

                Text(
                    text = "Backdrop Opacity: ${(preferences.subtitleBgOpacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = preferences.subtitleBgOpacity.toFloat(),
                    onValueChange = { value ->
                        onUpdatePreferences { it.copy(subtitleBgOpacity = value.toDouble()) }
                    },
                    valueRange = 0f..1f
                )

                Text(text = "Subtitle Color", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("#FFFFFF" to "White", "#F5B74F" to "Amber", "#48C9E8" to "Cyan").forEach { (hex, label) ->
                        FilterChip(
                            selected = preferences.subtitleColorHex.equals(hex, ignoreCase = true),
                            onClick = {
                                onUpdatePreferences { it.copy(subtitleColorHex = hex) }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Text(text = "Preferred Subtitle Language", style = MaterialTheme.typography.labelLarge)
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
                            label = { Text(label) },
                            modifier = Modifier.testTag("sub_lang_$code")
                        )
                    }
                }
            }
        }

        // Downloads & Local Library
        item {
            SettingsCard(title = "Downloads & Local Library") {
                ToggleRow(
                    title = "Auto-Retrieve Local Metadata",
                    subtitle = "Automatically match local videos with IMDb posters & synopsis",
                    checked = preferences.autoFetchLocalMetadata,
                    testTag = "auto_metadata_switch",
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(autoFetchLocalMetadata = checked) }
                    }
                )
                ToggleRow(
                    title = "Wi-Fi Only Offline Downloads",
                    subtitle = "Pause WorkManager offline downloads on metered cellular data",
                    checked = preferences.downloadWifiOnly,
                    testTag = "wifi_only_switch",
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(downloadWifiOnly = checked) }
                    }
                )
            }
        }

        // Account Sign In / Sign Out
        item {
            SettingsCard(title = "Cloud Account") {
                if (isGuestMode) {
                    Text(
                        text = "You are using CineVault in Local Guest Mode. Your addons, library, and preferences are saved on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onSignInClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_sign_in_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.sign_in_sync))
                    }
                } else {
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = "Signed in as $userEmail",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .machinedCardBorder(shape = cardShape, cornerRadiusDp = 18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun ToggleRow(
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
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
