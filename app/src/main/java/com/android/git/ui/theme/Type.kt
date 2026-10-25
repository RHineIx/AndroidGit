package com.android.git.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private fun oplusText(size: Float, height: Float, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = height.sp,
    letterSpacing = 0.sp,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Top, LineHeightStyle.Trim.None)
)

/** COUI's system-font metrics also apply to the Git-specific Material content. */
val Typography = Typography(
    displayLarge = oplusText(32f, 39.43f, FontWeight.SemiBold),
    displayMedium = oplusText(32f, 39.43f, FontWeight.SemiBold),
    displaySmall = oplusText(24f, 28.8f, FontWeight.Medium),
    headlineLarge = oplusText(32f, 39.43f, FontWeight.SemiBold),
    headlineMedium = oplusText(24f, 28.8f, FontWeight.Medium),
    headlineSmall = oplusText(20f, 23.66f, FontWeight.Medium),
    titleLarge = oplusText(18f, 22.29f, FontWeight.Medium),
    titleMedium = oplusText(16f, 18.53f, FontWeight.Medium),
    titleSmall = oplusText(14f, 17.14f, FontWeight.Medium),
    bodyLarge = oplusText(16f, 18.53f),
    bodyMedium = oplusText(14f, 17.14f),
    bodySmall = oplusText(13f, 15.92f),
    labelLarge = oplusText(16f, 20.21f, FontWeight.Medium),
    labelMedium = oplusText(12f, 14.69f, FontWeight.Medium),
    labelSmall = oplusText(11f, 12.57f)
)
