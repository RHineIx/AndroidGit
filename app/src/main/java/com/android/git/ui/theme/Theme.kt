package com.android.git.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.android.git.data.AppearanceSettings
import com.android.git.data.ThemePalette
import com.android.git.data.DarkSurface
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalAndroidGitDarkTheme = staticCompositionLocalOf { false }

private val AndroidGitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AndroidGitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appearance: AppearanceSettings = AppearanceSettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Read configuration so wallpaper/theme configuration changes invalidate the palette.
    val configuration = LocalConfiguration.current
    val system = remember(context, configuration, darkTheme, appearance.palette) {
        if (appearance.palette == ThemePalette.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else null
    }
    val materialColors = remember(darkTheme, appearance, system) {
        val base = system ?: if (darkTheme) darkColorScheme(
            primary = Color(0xFFD0BCFF), secondary = Color(0xFFCCC2DC), tertiary = Color(0xFFEFB8C8),
            background = Color(0xFF121318), surface = Color(0xFF121318),
            surfaceContainerLowest = Color(0xFF0D0E12), surfaceContainerLow = Color(0xFF1B1C22),
            surfaceContainer = Color(0xFF202128), surfaceContainerHigh = Color(0xFF292A32),
            surfaceContainerHighest = Color(0xFF34353D)
        ) else lightColorScheme(
            primary = Color(0xFF6650A4), secondary = Color(0xFF625B71), tertiary = Color(0xFF7D5260),
            background = Color(0xFFF7F7FC), surface = Color(0xFFF7F7FC),
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF1F0F7),
            surfaceContainer = Color(0xFFECEBF3), surfaceContainerHigh = Color(0xFFE5E4EE),
            surfaceContainerHighest = Color(0xFFDEDEE8)
        )
        val surfaces = if (darkTheme && appearance.darkSurface == DarkSurface.AMOLED) base.copy(
            background = Color.Black, surface = Color.Black, surfaceDim = Color.Black,
            surfaceBright = Color(0xFF24242A), surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF101014), surfaceContainer = Color(0xFF18181E),
            surfaceContainerHigh = Color(0xFF212127), surfaceContainerHighest = Color(0xFF2B2B32)
        ) else base
        if (appearance.palette != ThemePalette.CUSTOM) surfaces else {
            val accent = readableAccent(Color(appearance.customAccent), surfaces.background)
            val container = accent.copy(alpha = if (darkTheme) 0.22f else 0.13f).compositeOver(surfaces.surface)
            val secondary = lerp(accent, surfaces.onSurface, 0.30f)
            surfaces.copy(
                primary = accent, onPrimary = bestOnAccent(accent),
                primaryContainer = container, onPrimaryContainer = readableAccent(accent, container),
                secondary = secondary, onSecondary = bestOnAccent(secondary),
                secondaryContainer = secondary.copy(alpha = 0.15f).compositeOver(surfaces.surface),
                onSecondaryContainer = surfaces.onSurface, inversePrimary = accent,
                surfaceTint = accent
            )
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        }
    }
    CompositionLocalProvider(LocalAndroidGitDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = materialColors, typography = Typography, shapes = AndroidGitShapes, content = content)
    }
}

private fun contrast(first: Color, second: Color): Float {
    val a = first.luminance()
    val b = second.luminance()
    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
}

private fun bestOnAccent(accent: Color): Color =
    if (contrast(Color.White, accent) >= contrast(Color.Black, accent)) Color.White else Color.Black

/** Keep custom accent text legible against the selected page background. */
private fun readableAccent(accent: Color, background: Color): Color {
    if (contrast(accent, background) >= 4.5f) return accent
    val target = bestOnAccent(background)
    for (step in 1..20) {
        val candidate = lerp(accent, target, step / 20f)
        if (contrast(candidate, background) >= 4.5f) return candidate
    }
    return target
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext === this) null else baseContext.findActivity()
    else -> null
}
