@file:Suppress("SpellCheckingInspection")

package com.android.git.ui.components

import com.android.git.ui.components.GitIcons as Icons

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
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
import androidx.core.net.toUri
import com.android.git.R
import com.android.git.model.UpdateInfo
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

enum class SnackbarType {
    SUCCESS, ERROR, INFO, WARNING
}

@Composable
fun AppSnackbar(
    message: String,
    type: SnackbarType = SnackbarType.INFO,
    onDismiss: () -> Unit
) {
    val state = remember { io.github.suqi8.coui.kmp.basic.SnackbarHostState() }
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(message, type) {
        if (message.isNotEmpty()) {
            state.showSnackbar(
                message = message,
                withDismissAction = true,
                duration = io.github.suqi8.coui.kmp.basic.SnackbarDuration.Custom(
                    if (type == SnackbarType.ERROR) 4000L else 2500L
                )
            )
            dismiss()
        }
    }
    val (tint, icon) = when (type) {
        SnackbarType.SUCCESS -> MaterialTheme.colorScheme.primary to Icons.Default.CheckCircle
        SnackbarType.ERROR -> MaterialTheme.colorScheme.error to Icons.Default.Error
        SnackbarType.WARNING -> MaterialTheme.colorScheme.error to Icons.Default.Warning
        SnackbarType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.Info
    }
    io.github.suqi8.coui.kmp.basic.SnackbarHost(
        state = state,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
    ) { data ->
        io.github.suqi8.coui.kmp.basic.Snackbar(
            data = data,
            icon = { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp)) }
        )
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
                .padding(horizontal = 12.dp)
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
                        if (updateInfo.versionCode > 0) "v${updateInfo.versionName} (Build${updateInfo.versionCode})"
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .nestedScroll(nestedScrollConnection)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ -> change.consume() }
                    }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
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
                        onClick = {
                            downloadApk(context, updateInfo.downloadUrl, updateInfo.versionName)
                            onDismiss()
                        },
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

private fun downloadApk(context: Context, url: String, versionName: String) {
    try {
        val request = DownloadManager.Request(url.toUri()).apply {
            setTitle("AndroidGit v$versionName")
            setDescription("Downloading update...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AndroidGit-v$versionName.apk")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        // Fallback to browser if DownloadManager fails or is disabled
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}