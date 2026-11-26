package com.android.git.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
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
    content: @Composable () -> Unit
) {
    val coui = remember(darkTheme) { if (darkTheme) couiDarkColorScheme() else couiLightColorScheme() }
    val materialColors = remember(coui, darkTheme) {
        val page = coui.backgroundWithCard
        val card = coui.cardBackground.compositeOver(page)
        val raised = coui.cardPressed.compositeOver(page)
        // Keep specialized Git controls on the same neutral surfaces as COUI preferences.
        if (darkTheme) darkColorScheme(
            primary = coui.primary,
            onPrimary = coui.onPrimary,
            primaryContainer = coui.secondary.compositeOver(page),
            onPrimaryContainer = coui.primaryText,
            secondary = coui.primary,
            onSecondary = coui.onPrimary,
            secondaryContainer = raised,
            onSecondaryContainer = coui.labelPrimary,
            tertiary = coui.primary,
            onTertiary = coui.onPrimary,
            tertiaryContainer = coui.secondary.compositeOver(page),
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
            errorContainer = coui.error.copy(alpha = 0.16f).compositeOver(page),
            onErrorContainer = coui.error,
            inverseSurface = OplusCardLight,
            inverseOnSurface = Color(0xE6000000),
            inversePrimary = OplusBlue,
            scrim = Color.Black
        ) else lightColorScheme(
            primary = coui.primary,
            onPrimary = coui.onPrimary,
            primaryContainer = coui.secondary.compositeOver(page),
            onPrimaryContainer = coui.primaryText,
            secondary = coui.primary,
            onSecondary = coui.onPrimary,
            secondaryContainer = raised,
            onSecondaryContainer = coui.labelPrimary,
            tertiary = coui.primary,
            onTertiary = coui.onPrimary,
            tertiaryContainer = coui.secondary.compositeOver(page),
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
            errorContainer = coui.error.copy(alpha = 0.16f).compositeOver(page),
            onErrorContainer = coui.error,
            inverseSurface = OplusCardDark,
            inverseOnSurface = Color(0xE6FFFFFF),
            inversePrimary = OplusBlueDark,
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext === this) null else baseContext.findActivity()
    else -> null
}
