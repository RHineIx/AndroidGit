package com.android.git.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.R
import com.android.git.model.UpdateInfo
import kotlinx.coroutines.delay

enum class SnackbarType {
    SUCCESS, ERROR, INFO, WARNING
}

@Composable
fun AppSnackbar(
    message: String,
    type: SnackbarType = SnackbarType.INFO,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message, type) {
        if (message.isNotEmpty()) {
            val duration = if (type == SnackbarType.ERROR) 4000L else 2500L
            delay(duration)
            onDismiss()
        }
    }

    val (backgroundColor, contentColor, icon) = when (type) {
        SnackbarType.SUCCESS -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.CheckCircle)
        SnackbarType.ERROR -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Icons.Default.Error)
        SnackbarType.WARNING -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), Icons.Default.Warning)
        SnackbarType.INFO -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface, Icons.Default.Info)
    }

    AnimatedVisibility(
        visible = message.isNotEmpty(),
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = backgroundColor,
                contentColor = contentColor,
                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.action_dismiss), tint = contentColor.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBottomSheet(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                return available 
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return available
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ -> change.consume() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.update_new_version),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    val versionLabel = remember(updateInfo) {
                        if (updateInfo.versionCode > 0) "v${updateInfo.versionName} (Build ${updateInfo.versionCode})"
                        else "v${updateInfo.versionName}"
                    }
                    
                    Text(
                        text = versionLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .nestedScroll(nestedScrollConnection)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ -> change.consume() }
                    }
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Changelog",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        MarkdownText(
                            text = updateInfo.releaseNotes,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { openLink(context, updateInfo.downloadUrl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ -> change.consume() }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = stringResource(R.string.update_download), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, color: Color) {
    val annotatedString = remember(text) { parseMarkdown(text, color) }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = 22.sp
    )
}

fun parseMarkdown(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        var isFirstLine = true

        for (line in lines) {
            if (!isFirstLine) {
                append("\n")
            }
            isFirstLine = false

            var currentLine = line.trim()

            // Headers
            if (currentLine.startsWith("### ")) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = defaultColor)) {
                    append(currentLine.removePrefix("### "))
                }
                continue
            } else if (currentLine.startsWith("## ")) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = defaultColor)) {
                    append(currentLine.removePrefix("## "))
                }
                continue
            } else if (currentLine.startsWith("# ")) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = defaultColor)) {
                    append(currentLine.removePrefix("# "))
                }
                continue
            }

            // Bullet points
            if (currentLine.startsWith("- ") || currentLine.startsWith("* ")) {
                append("• ")
                currentLine = currentLine.substring(2)
            }

            // Inline formatting (Bold **text**)
            var i = 0
            while (i < currentLine.length) {
                if (i + 1 < currentLine.length && currentLine[i] == '*' && currentLine[i + 1] == '*') {
                    val endBold = currentLine.indexOf("**", i + 2)
                    if (endBold != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                            append(currentLine.substring(i + 2, endBold))
                        }
                        i = endBold + 2
                    } else {
                        append(currentLine[i].toString())
                        i++
                    }
                } else if (currentLine[i] == '`') {
                    // Simple inline code
                    val endCode = currentLine.indexOf('`', i + 1)
                    if (endCode != -1) {
                        withStyle(style = SpanStyle(background = Color.Gray.copy(alpha = 0.2f), color = defaultColor)) {
                            append(currentLine.substring(i + 1, endCode))
                        }
                        i = endCode + 1
                    } else {
                        append(currentLine[i].toString())
                        i++
                    }
                } else {
                    withStyle(style = SpanStyle(color = defaultColor)) {
                        append(currentLine[i].toString())
                    }
                    i++
                }
            }
        }
    }
}

private fun openLink(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}