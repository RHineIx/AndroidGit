package com.android.git.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ActionTile(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, danger: Boolean = false, horizontal: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val accent = if (danger) colors.error else colors.primary
    androidx.compose.material3.ElevatedCard(
        onClick = onClick, enabled = enabled, modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        if (horizontal) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TileIcon(icon, accent, enabled)
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            }
        } else {
            Column(
                Modifier.fillMaxWidth().heightIn(min = 84.dp).padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
            ) {
                TileIcon(icon, accent, enabled)
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun TileIcon(icon: ImageVector, accent: androidx.compose.ui.graphics.Color, enabled: Boolean) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = accent.copy(alpha = if (enabled) 1f else 0.38f))
    }
}
