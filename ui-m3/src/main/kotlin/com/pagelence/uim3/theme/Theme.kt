package com.pagelence.uim3.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.ThemeMode

/**
 * Material 3 palette: white surfaces, ink-blue primary and a terracotta
 * accent. The phone app reads in the system sans face by default; the e-ink app
 * passes [FontFamily.Serif] to the same style builder.
 */
private val Light = lightColorScheme(
    primary = Color(0xFF2F4F66),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE8F2),
    onPrimaryContainer = Color(0xFF0A1D29),
    secondary = Color(0xFF4F6474),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7EDF2),
    onSecondaryContainer = Color(0xFF1D2C36),
    tertiary = Color(0xFF6B5B7B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEADFEF),
    onTertiaryContainer = Color(0xFF251A2C),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF17181A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17181A),
    surfaceVariant = Color(0xFFECECEF),
    onSurfaceVariant = Color(0xFF5B5B63),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F8FA),
    surfaceContainer = Color(0xFFF3F3F5),
    surfaceContainerHigh = Color(0xFFEDEDF0),
    surfaceContainerHighest = Color(0xFFE7E7EA),
    outline = Color(0xFF8B8B93),
    outlineVariant = Color(0xFFE1E1E5),
    inverseSurface = Color(0xFF2C2C2F),
    inverseOnSurface = Color(0xFFF5F5F7),
    inversePrimary = Color(0xFFA9C7DE),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA9C7DE),
    onPrimary = Color(0xFF15313F),
    primaryContainer = Color(0xFF2B465A),
    onPrimaryContainer = Color(0xFFDCE8F2),
    secondary = Color(0xFFB7C7D4),
    onSecondary = Color(0xFF22323D),
    secondaryContainer = Color(0xFF33454F),
    onSecondaryContainer = Color(0xFFDCE7EF),
    tertiary = Color(0xFFD8B9CA),
    onTertiary = Color(0xFF3B2331),
    tertiaryContainer = Color(0xFF523848),
    onTertiaryContainer = Color(0xFFEADFEF),
    background = Color(0xFF121214),
    onBackground = Color(0xFFE7E7EA),
    surface = Color(0xFF121214),
    onSurface = Color(0xFFE7E7EA),
    surfaceVariant = Color(0xFF46464B),
    onSurfaceVariant = Color(0xFFC6C6CC),
    surfaceContainerLowest = Color(0xFF0D0D0E),
    surfaceContainerLow = Color(0xFF1B1B1D),
    surfaceContainer = Color(0xFF1F1F22),
    surfaceContainerHigh = Color(0xFF28282B),
    surfaceContainerHighest = Color(0xFF323235),
    outline = Color(0xFF909097),
    outlineVariant = Color(0xFF46464B),
    inverseSurface = Color(0xFFE7E7EA),
    inverseOnSurface = Color(0xFF2C2C2F),
    inversePrimary = Color(0xFF2F4F66),
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

/**
 * Body style for the reading surface. The phone shell uses the default sans
 * family; the e-ink shell can pass [FontFamily.Serif] without changing the
 * reading rules or the state schema.
 */
@Composable
fun readingTextStyle(
    fontSize: FontSize,
    fontFamily: FontFamily = FontFamily.SansSerif,
): TextStyle {
    val size = when (fontSize) {
        FontSize.SMALL -> 17.sp
        FontSize.MEDIUM -> 19.sp
        FontSize.LARGE -> 22.sp
        FontSize.XLARGE -> 25.sp
    }
    return TextStyle(
        fontFamily = fontFamily,
        fontSize = size,
        lineHeight = size * 1.62f,
        letterSpacing = 0.1.sp,
    )
}

@Composable
fun isDark(theme: ThemeMode): Boolean = when (theme) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun PagelenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
