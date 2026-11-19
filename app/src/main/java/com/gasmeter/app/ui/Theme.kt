package com.gasmeter.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gasmeter.app.R

// "Night Drive": an instrument-cluster look. Dark is the main design; light is a
// daytime variant with the same structure.

private val Amber = Color(0xFFFFB020)
private val AmberDark = Color(0xFF9A5B00)

private val NightColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF14100A),
    primaryContainer = Color(0xFF3A2A0C),
    onPrimaryContainer = Color(0xFFFFD58A),
    secondary = Color(0xFFC9CED6),
    onSecondary = Color(0xFF14181F),
    secondaryContainer = Color(0xFF262C36),
    onSecondaryContainer = Color(0xFFE8EAED),
    tertiary = Amber,
    onTertiary = Color(0xFF14100A),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF0E1116),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF1A1F27),
    onSurfaceVariant = Color(0xFF9AA3AE),
    surfaceContainerLowest = Color(0xFF0B0D11),
    surfaceContainerLow = Color(0xFF151A21),
    surfaceContainer = Color(0xFF1A1F27),
    surfaceContainerHigh = Color(0xFF20262F),
    surfaceContainerHighest = Color(0xFF262C36),
    outline = Color(0xFF2A313C),
    outlineVariant = Color(0xFF2A313C),
    error = Color(0xFFFF5A5F),
    onError = Color(0xFF1A0506),
    errorContainer = Color(0xFF3B1214),
    onErrorContainer = Color(0xFFFFB3B5),
)

private val DayColors = lightColorScheme(
    primary = AmberDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE7BF),
    onPrimaryContainer = Color(0xFF3A2400),
    secondary = Color(0xFF4A525E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4E7EC),
    onSecondaryContainer = Color(0xFF14181F),
    tertiary = AmberDark,
    onTertiary = Color.White,
    background = Color(0xFFF3F4F6),
    onBackground = Color(0xFF14181F),
    surface = Color(0xFFF3F4F6),
    onSurface = Color(0xFF14181F),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF5B6470),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF8F9FA),
    surfaceContainerHighest = Color(0xFFECEEF1),
    outline = Color(0xFFD5D9DF),
    outlineVariant = Color(0xFFD5D9DF),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFDE7E7),
    onErrorContainer = Color(0xFF5C0B0B),
)

/** Colours that Material's scheme has no slot for. */
@Immutable
data class GasMeterColors(
    val efficient: Color,
    val normal: Color,
    val expensive: Color,
    val neutral: Color,
    val muted: Color,
    val card: Color,
)

private val NightExtras = GasMeterColors(
    efficient = Color(0xFF3DDC84),
    normal = Amber,
    expensive = Color(0xFFFF5A5F),
    neutral = Color(0xFF6B7480),
    muted = Color(0xFF9AA3AE),
    card = Color(0xFF1A1F27),
)

private val DayExtras = GasMeterColors(
    efficient = Color(0xFF1E7D4A),
    normal = AmberDark,
    expensive = Color(0xFFC62828),
    neutral = Color(0xFF8A929C),
    muted = Color(0xFF5B6470),
    card = Color(0xFFFFFFFF),
)

val LocalGasMeterColors = staticCompositionLocalOf { NightExtras }

/** Monospaced digits for every number, like an instrument cluster readout. */
val MonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

private val base = Typography()

private val NightTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = MonoFamily, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = MonoFamily, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = MonoFamily, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.25.sp),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp),
)

/** Numbers inside running text and rows. */
val MonoBody: TextStyle get() = TextStyle(fontFamily = MonoFamily, fontSize = 15.sp, lineHeight = 22.sp)

private val NightShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

@Composable
fun GasMeterTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalGasMeterColors provides if (dark) NightExtras else DayExtras) {
        MaterialTheme(
            colorScheme = if (dark) NightColors else DayColors,
            typography = NightTypography,
            shapes = NightShapes,
            content = content,
        )
    }
}
