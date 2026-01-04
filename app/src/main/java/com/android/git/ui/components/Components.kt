package com.android.git.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AppSnackbar(
    message: String,
    onDismiss: () -> Unit
) {
    // Auto-dismiss after 3 seconds
    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            delay(3000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = message.isNotEmpty(),
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Surface(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(24.dp), // Capsule shape
            color = MaterialTheme.colorScheme.inverseSurface, // Dark on Light / Light on Dark
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Determine icon based on message content
                val icon = when {
                    message.contains("Error", ignoreCase = true) || message.contains("failed", ignoreCase = true) -> Icons.Default.Error
                    message.contains("Success", ignoreCase = true) -> Icons.Default.CheckCircle
                    else -> Icons.Default.Info
                }
                
                val iconColor = when {
                    message.contains("Error", ignoreCase = true) || message.contains("failed", ignoreCase = true) -> MaterialTheme.colorScheme.errorContainer
                    message.contains("Success", ignoreCase = true) -> Color(0xFF4CAF50) // Green
                    else -> MaterialTheme.colorScheme.inverseOnSurface
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}