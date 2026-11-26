package com.android.git.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PreferenceGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        content = content
    )
}

@Composable
fun PreferenceRow(
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
    val colors = MaterialTheme.colorScheme
    val accent = if (danger) colors.error else colors.primary
    val opacity = if (enabled) 1f else 0.38f
    Row(
        modifier = modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (leadingContent != null) leadingContent()
        else if (icon != null) Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.12f * opacity)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, Modifier.size(22.dp), tint = accent.copy(alpha = opacity)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                color = (if (danger) colors.error else colors.onSurface).copy(alpha = opacity))
            if (!summary.isNullOrBlank()) Text(summary, style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = opacity))
        }
        if (trailing != null) trailing()
        else if (onClick != null) Icon(GitIcons.Default.ChevronRight, null, Modifier.size(20.dp),
            tint = colors.onSurfaceVariant.copy(alpha = opacity))
    }
}

@Composable
fun PreferenceDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
}
