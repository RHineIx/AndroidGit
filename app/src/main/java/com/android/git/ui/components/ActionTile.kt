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
    enabled: Boolean = true, danger: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val accent = if (danger) colors.error else colors.primary
    androidx.compose.material3.ElevatedCard(
        onClick = onClick, enabled = enabled, modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 116.dp).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(26.dp), tint = accent.copy(alpha = if (enabled) 1f else 0.38f))
            }
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}
