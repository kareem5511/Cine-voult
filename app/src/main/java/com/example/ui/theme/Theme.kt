package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalIsAmoledBlack = staticCompositionLocalOf { false }
val LocalAccentPreset = staticCompositionLocalOf { AccentPreset.EMERALD_NOIR }

val CineVaultShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CineVaultTheme(
    accentPreset: AccentPreset = AccentPreset.EMERALD_NOIR,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = accentPreset.primary,
        onPrimary = accentPreset.onPrimary,
        primaryContainer = accentPreset.primary.copy(alpha = 0.16f),
        onPrimaryContainer = accentPreset.primary,
        secondary = accentPreset.secondary,
        onSecondary = accentPreset.onSecondary,
        secondaryContainer = accentPreset.secondary.copy(alpha = 0.16f),
        onSecondaryContainer = accentPreset.secondary,
        tertiary = CrimsonVelvet,
        background = if (amoledBlack) AmoledBg else ObsidianBg,
        onBackground = TextPrimary,
        surface = if (amoledBlack) AmoledCard else SlateSurface,
        onSurface = TextPrimary,
        surfaceVariant = if (amoledBlack) AmoledSurfaceElevated else SlateSurfaceElevated,
        onSurfaceVariant = TextSecondary,
        surfaceContainerLow = if (amoledBlack) AmoledCard else SlateSurface,
        surfaceContainer = if (amoledBlack) AmoledCard else SlateSurfaceElevated,
        surfaceContainerHigh = if (amoledBlack) AmoledSurfaceElevated else SlateSurfaceHighest,
        outline = if (amoledBlack) AmoledOutline else SlateOutline,
        outlineVariant = Color.White.copy(alpha = 0.08f),
        error = ErrorRed,
        onError = ObsidianBg,
        errorContainer = ErrorContainerDark,
        onErrorContainer = ErrorRed
    )

    CompositionLocalProvider(
        LocalIsAmoledBlack provides amoledBlack,
        LocalAccentPreset provides accentPreset
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CineVaultTypography,
            shapes = CineVaultShapes,
            content = content
        )
    }
}

fun Modifier.machinedSurface(
    shape: Shape = RoundedCornerShape(18.dp),
    amoledBlack: Boolean = false,
    borderAlpha: Float = 0.08f,
    topHighlightAlpha: Float = 0.07f,
    cornerRadiusDp: Dp = 18.dp
): Modifier = this
    .clip(shape)
    .border(width = 1.dp, color = Color.White.copy(alpha = borderAlpha), shape = shape)
    .drawWithContent {
        drawContent()
        val inset = 1.dp.toPx()
        val cr = cornerRadiusDp.toPx()
        val lineWidth = (size.width - cr * 0.8f).coerceAtLeast(0f)
        if (lineWidth > 0f) {
            drawRoundRect(
                color = Color.White.copy(alpha = topHighlightAlpha),
                topLeft = Offset((size.width - lineWidth) / 2f, inset * 0.5f),
                size = Size(lineWidth, inset),
                cornerRadius = CornerRadius(inset, inset)
            )
        }
    }

@Composable
fun Modifier.machinedCardBorder(
    shape: Shape = RoundedCornerShape(18.dp),
    cornerRadiusDp: Dp = 18.dp
): Modifier {
    val isAmoled = LocalIsAmoledBlack.current
    return this.machinedSurface(
        shape = shape,
        amoledBlack = isAmoled,
        cornerRadiusDp = cornerRadiusDp
    )
}
