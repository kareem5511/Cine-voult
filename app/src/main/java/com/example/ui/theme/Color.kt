package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Obsidian Cinema Palette
val ObsidianBg = Color(0xFF0B0D12)
val SlateSurface = Color(0xFF141821)
val SlateSurfaceElevated = Color(0xFF1C222E)
val SlateSurfaceHighest = Color(0xFF252D3D)
val SlateOutline = Color(0xFF2F394D)

// Pure AMOLED Black Surface Palette
val AmoledBg = Color(0xFF000000)
val AmoledCard = Color(0xFF080808)
val AmoledSurfaceElevated = Color(0xFF101010)
val AmoledSurfaceHighest = Color(0xFF181818)
val AmoledOutline = Color(0xFF242424)

val TextPrimary = Color(0xFFF2F4F8)
val TextSecondary = Color(0xFFA8B3C7)
val TextMuted = Color(0xFF6E7B91)

// Accent Presets (Synced via UserPreferences)
val TungstenGold = Color(0xFFF5B74F)
val TungstenGoldDark = Color(0xFF422C00)
val CinemaCyan = Color(0xFF48C9E8)
val CinemaCyanDark = Color(0xFF003642)
val CrimsonVelvet = Color(0xFFF05A7E)
val CrimsonVelvetDark = Color(0xFF4D0A1D)
val EmeraldNoir = Color(0xFF3DD68C)
val EmeraldNoirDark = Color(0xFF00391F)

val ErrorRed = Color(0xFFFF6B6B)
val ErrorContainerDark = Color(0xFF4F1418)
val SuccessGreen = Color(0xFF3DD68C)
val WarningAmber = Color(0xFFFFB84D)

enum class AccentPreset(
    val key: String,
    val label: String,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val surfaceSwatch: Color
) {
    TUNGSTEN_GOLD(
        key = "TUNGSTEN_GOLD",
        label = "Tungsten Gold",
        primary = TungstenGold,
        onPrimary = TungstenGoldDark,
        secondary = CinemaCyan,
        onSecondary = CinemaCyanDark,
        surfaceSwatch = Color(0xFF2B2214)
    ),
    CINEMA_CYAN(
        key = "CINEMA_CYAN",
        label = "Cinema Cyan",
        primary = CinemaCyan,
        onPrimary = CinemaCyanDark,
        secondary = TungstenGold,
        onSecondary = TungstenGoldDark,
        surfaceSwatch = Color(0xFF0E232B)
    ),
    CRIMSON_VELVET(
        key = "CRIMSON_VELVET",
        label = "Crimson Velvet",
        primary = CrimsonVelvet,
        onPrimary = Color(0xFF3A0413),
        secondary = TungstenGold,
        onSecondary = TungstenGoldDark,
        surfaceSwatch = Color(0xFF2D121B)
    ),
    EMERALD_NOIR(
        key = "EMERALD_NOIR",
        label = "Emerald Noir",
        primary = EmeraldNoir,
        onPrimary = EmeraldNoirDark,
        secondary = CinemaCyan,
        onSecondary = CinemaCyanDark,
        surfaceSwatch = Color(0xFF0E241B)
    );

    companion object {
        fun fromKey(key: String?): AccentPreset =
            entries.firstOrNull { it.key == key } ?: TUNGSTEN_GOLD
    }
}
