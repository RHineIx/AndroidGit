package com.android.git.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.BasicComponentDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.theme.COUITheme

/** Group geometry is shared by settings, repository actions and status rows. */
@Composable
fun OplusPreferenceGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        insideMargin = PaddingValues(vertical = 2.dp),
        content = content
    )
}

@Composable
fun OplusPreferenceRow(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    danger: Boolean = false
) {
    val tint = if (!enabled) COUITheme.colorScheme.disabledOnSurface else if (danger) COUITheme.colorScheme.error else COUITheme.colorScheme.onSurface
    CompositionLocalProvider(LocalContentColor provides tint) {
        BasicComponent(
            modifier = modifier.fillMaxWidth(),
            title = title,
            titleColor = BasicComponentDefaults.titleColor(color = tint),
            summary = summary,
            enabled = enabled,
            onClick = onClick,
            startAction = if (icon != null || leadingContent != null) {
                {
                    Box(Modifier.padding(end = 8.dp), contentAlignment = Alignment.Center) {
                        if (leadingContent != null) leadingContent()
                        else if (icon != null) Icon(icon, null, Modifier.size(22.dp), tint = tint)
                    }
                }
            } else null,
            endActions = if (trailing != null || onClick != null) {
                {
                    if (trailing != null) trailing()
                    else Icon(
                        GitIcons.Default.ChevronRight,
                        null,
                        Modifier.size(24.dp),
                        tint = COUITheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            } else null
        )
    }
}

@Composable
fun OplusPreferenceDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        thickness = 0.33.dp,
        color = COUITheme.colorScheme.dividerLine
    )
}
