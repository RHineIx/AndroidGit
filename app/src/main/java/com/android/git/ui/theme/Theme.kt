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
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme as couiDarkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme as couiLightColorScheme

val LocalAndroidGitDarkTheme = staticCompositionLocalOf { false }

private val AndroidGitShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(20.dp)
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
    val coui = remember(darkTheme, appearance, system) {
        val base = if (darkTheme) couiDarkColorScheme() else couiLightColorScheme()
        val amoled = darkTheme && appearance.darkSurface == DarkSurface.AMOLED
        val page = when {
            amoled -> Color.Black
            system != null -> if (darkTheme) system.surface else system.surfaceContainerLow
            darkTheme -> Color(0xFF121212)
            else -> base.backgroundWithCard
        }
        val card = when {
            amoled -> Color(0xFF1A1A1A)
            system != null -> if (darkTheme) system.surfaceContainer else system.surface
            darkTheme -> Color(0xFF202020)
            else -> base.cardBackground
        }
        val raised = when {
            amoled -> Color(0xFF292929)
            system != null -> system.surfaceContainerHigh
            darkTheme -> Color(0xFF303030)
            else -> base.cardPressed
        }
        val foreground = system?.onSurface ?: base.labelPrimary
        val accent = when (appearance.palette) {
            ThemePalette.CUSTOM -> readableAccent(Color(appearance.customAccent), page)
            ThemePalette.SYSTEM -> system?.primary ?: base.primary
            ThemePalette.DEFAULT -> base.primary
        }
        val onAccent = if (system != null) system.onPrimary else bestOnAccent(accent)
        val textAccent = readableAccent(accent, page)
        val tinted = accent.copy(alpha = if (darkTheme) 0.25f else 0.15f)
        base.copy(
            primary = accent, onPrimary = onAccent, labelOnColor = onAccent,
            primaryText = textAccent, primaryTextOnPopup = readableAccent(accent, card),
            labelTheme = textAccent, link = textAccent, tips = textAccent,
            additional = accent, secondary = tinted, containerTheme = accent,
            containerThemeHalftone = tinted, textColorHighlight = tinted,
            disable = accent.copy(alpha = 0.30f), focusOutline = accent.copy(alpha = 0.4f),
            disabledOnPrimaryButton = onAccent.copy(alpha = 0.45f),
            background = page, backgroundElevated = card, backgroundElevatedWithCard = page,
            backgroundWithCard = page, surface = card, surfaceWithCard = page,
            surfaceTop = card, backgroundTop = card, bar = card, bottomBar = card,
            card = card, cardBackground = card, cardPressed = raised, surfaceContainerHighest = raised,
            labelPrimary = foreground,
            labelSecondary = system?.onSurfaceVariant ?: base.labelSecondary,
            labelSecondaryVariant = system?.onSurfaceVariant ?: base.labelSecondaryVariant,
            outline = system?.outline ?: base.outline,
            divider = system?.outlineVariant ?: base.divider
        )
    }
    val materialColors = remember(coui, darkTheme) {
        val page = coui.backgroundWithCard
        val card = coui.cardBackground.compositeOver(page)
        val raised = coui.cardPressed.compositeOver(page)
        // Keep specialized Git controls on the same neutral surfaces as COUI preferences.
        if (darkTheme) darkColorScheme(
            primary = coui.primary,
            onPrimary = coui.onPrimary,
            primaryContainer = coui.containerThemeHalftone.compositeOver(page),
            onPrimaryContainer = coui.primaryText,
            secondary = coui.primary,
            onSecondary = coui.onPrimary,
            secondaryContainer = raised,
            onSecondaryContainer = coui.labelPrimary,
            tertiary = coui.primary,
            onTertiary = coui.onPrimary,
            tertiaryContainer = coui.containerThemeHalftone.compositeOver(page),
            onTertiaryContainer = coui.primaryText,
            background = page,
            onBackground = coui.labelPrimary,
            surface = page,
            onSurface = coui.labelPrimary,
            surfaceVariant = raised,
            onSurfaceVariant = coui.labelSecondaryVariant,
            surfaceTint = Color.Transparent,
            surfaceDim = page,
            surfaceBright = raised,
            surfaceContainerLowest = page,
            surfaceContainerLow = card,
            surfaceContainer = card,
            surfaceContainerHigh = raised,
            surfaceContainerHighest = raised,
            outline = coui.outline,
            outlineVariant = coui.divider,
            error = coui.error,
            onError = coui.onError,
            errorContainer = coui.containerRed.compositeOver(page),
            onErrorContainer = coui.error,
            inverseSurface = OplusCardLight,
            inverseOnSurface = Color(0xE6000000),
            inversePrimary = coui.primaryText,
            scrim = Color.Black
        ) else lightColorScheme(
            primary = coui.primary,
            onPrimary = coui.onPrimary,
            primaryContainer = coui.containerThemeHalftone.compositeOver(page),
            onPrimaryContainer = coui.primaryText,
            secondary = coui.primary,
            onSecondary = coui.onPrimary,
            secondaryContainer = raised,
            onSecondaryContainer = coui.labelPrimary,
            tertiary = coui.primary,
            onTertiary = coui.onPrimary,
            tertiaryContainer = coui.containerThemeHalftone.compositeOver(page),
            onTertiaryContainer = coui.primaryText,
            background = page,
            onBackground = coui.labelPrimary,
            surface = page,
            onSurface = coui.labelPrimary,
            surfaceVariant = raised,
            onSurfaceVariant = coui.labelSecondaryVariant,
            surfaceTint = Color.Transparent,
            surfaceDim = page,
            surfaceBright = card,
            surfaceContainerLowest = page,
            surfaceContainerLow = card,
            surfaceContainer = card,
            surfaceContainerHigh = raised,
            surfaceContainerHighest = raised,
            outline = coui.outline,
            outlineVariant = coui.divider,
            error = coui.error,
            onError = coui.onError,
            errorContainer = coui.containerRed.compositeOver(page),
            onErrorContainer = coui.error,
            inverseSurface = OplusCardDark,
            inverseOnSurface = Color(0xE6FFFFFF),
            inversePrimary = coui.primaryText,
            scrim = Color.Black
        )
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
        COUITheme(colors = coui) {
            MaterialTheme(colorScheme = materialColors, typography = Typography, shapes = AndroidGitShapes, content = content)
        }
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
