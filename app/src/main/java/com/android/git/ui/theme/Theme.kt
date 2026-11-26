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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.android.git.data.AppearanceSettings
import com.android.git.data.ThemePalette
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalAndroidGitDarkTheme = staticCompositionLocalOf { false }
val LocalAndroidGitUsesSystemColors = staticCompositionLocalOf { false }

// Dark palette calibrated against GitHub web Tritanopia screenshots.
// JPEG samples are normalized to the nearest matching UI color tokens.
// Links and solid buttons intentionally use different blues for contrast.
val GitHubDarkButtonBlue = Color(0xFF1F6FEB)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4493F8), onPrimary = Color(0xFF0D1117),
    primaryContainer = Color(0xFF121D2F), onPrimaryContainer = Color(0xFFF0F6FC),
    secondary = Color(0xFF9198A1), onSecondary = Color(0xFF0D1117),
    secondaryContainer = Color(0xFF212830), onSecondaryContainer = Color(0xFFF0F6FC),
    tertiary = Color(0xFFF78166), onTertiary = Color(0xFF0D1117),
    tertiaryContainer = Color(0xFF3D221D), onTertiaryContainer = Color(0xFFFFB4A2),
    background = Color(0xFF0D1117), onBackground = Color(0xFFF0F6FC),
    surface = Color(0xFF0D1117), onSurface = Color(0xFFF0F6FC),
    surfaceVariant = Color(0xFF151B23), onSurfaceVariant = Color(0xFF9198A1),
    surfaceDim = Color(0xFF010409), surfaceBright = Color(0xFF30363D),
    surfaceContainerLowest = Color(0xFF010409), surfaceContainerLow = Color(0xFF151B23),
    surfaceContainer = Color(0xFF151B23), surfaceContainerHigh = Color(0xFF212830),
    surfaceContainerHighest = Color(0xFF252B34),
    outline = Color(0xFF656C76), outlineVariant = Color(0xFF3D444D),
    error = Color(0xFFF85149), onError = Color(0xFF0D1117),
    errorContainer = Color(0xFF27171C), onErrorContainer = Color(0xFFFFA198),
    inverseSurface = Color(0xFFF0F6FC), inverseOnSurface = Color(0xFF1F2328),
    inversePrimary = Color(0xFF0969DA), surfaceTint = Color(0xFF4493F8),
    scrim = Color.Black
)
// Light is not sampled: the supplied screenshots only show dark mode.
// Keep the corresponding existing Primer-based light palette.
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0969DA), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF4FF), onPrimaryContainer = Color(0xFF0550AE),
    secondary = Color(0xFF59636E), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6EAEF), onSecondaryContainer = Color(0xFF1F2328),
    tertiary = Color(0xFFCF222E), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEBE9), onTertiaryContainer = Color(0xFF82071E),
    background = Color.White, onBackground = Color(0xFF1F2328),
    surface = Color.White, onSurface = Color(0xFF1F2328),
    surfaceVariant = Color(0xFFEFF2F5), onSurfaceVariant = Color(0xFF59636E),
    surfaceDim = Color(0xFFE0E6EB), surfaceBright = Color.White,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF6F8FA),
    surfaceContainer = Color(0xFFEFF2F5), surfaceContainerHigh = Color(0xFFE6EAEF),
    surfaceContainerHighest = Color(0xFFE0E6EB),
    outline = Color(0xFF818B98), outlineVariant = Color(0xFFD1D9E0),
    error = Color(0xFFCF222E), onError = Color.White,
    errorContainer = Color(0xFFFFEBE9), onErrorContainer = Color(0xFF82071E),
    inverseSurface = Color(0xFF24292F), inverseOnSurface = Color(0xFFF6F8FA),
    inversePrimary = Color(0xFF4493F8), surfaceTint = Color(0xFF0969DA),
    scrim = Color.Black
)

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
    val materialColors = system ?: if (darkTheme) DarkColorScheme else LightColorScheme
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
    CompositionLocalProvider(
        LocalAndroidGitDarkTheme provides darkTheme,
        LocalAndroidGitUsesSystemColors provides (system != null)
    ) {
        MaterialTheme(colorScheme = materialColors, typography = Typography, shapes = AndroidGitShapes, content = content)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext === this) null else baseContext.findActivity()
    else -> null
}
