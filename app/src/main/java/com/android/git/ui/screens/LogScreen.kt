package com.android.git.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.R
import com.android.git.data.GitManager
import com.android.git.model.CommitItem
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.SnackbarType
import com.android.git.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    gitManager: GitManager,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val commits = viewModel.logList
    val isLogLoading = viewModel.isLogLoading

    var statusMessage by remember { mutableStateOf("") }
    var statusType by remember { mutableStateOf(SnackbarType.INFO) }

    var selectedCommit by remember { mutableStateOf<CommitItem?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Safely extract string resources here (State Hoisting) to avoid Lint errors
    val copiedHashMsg = stringResource(R.string.log_copied_hash)

    BackHandler(enabled = true) { onBack() }

    LaunchedEffect(Unit) {
        if (commits.isEmpty()) {
            viewModel.loadLogs(reset = true)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            totalItems > 0 && lastVisibleItemIndex >= (totalItems - 5)
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                viewModel.loadLogs(reset = false)
            }
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Commit Hash", text)
        clipboard.setPrimaryClip(clip)
        statusMessage = copiedHashMsg
        statusType = SnackbarType.SUCCESS
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.log_title), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.log_subtitle, commits.size), style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (isLogLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(onClick = { viewModel.loadLogs(reset = true) }) {
                            Icon(Icons.Default.Refresh, stringResource(R.string.action_refresh))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (commits.isEmpty() && !isLogLoading) {
                    EmptyStateView(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        itemsIndexed(commits) { index, commit ->
                            val isLast = index == commits.lastIndex
                            TimelineCommitItem(
                                commit = commit,
                                isLast = isLast,
                                onClick = {
                                    selectedCommit = commit
                                    showBottomSheet = true
                                }
                            )
                        }

                        if (isLogLoading && commits.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }

                if (commits.isEmpty() && isLogLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
                    AppSnackbar(
                        message = statusMessage,
                        type = statusType,
                        onDismiss = { statusMessage = "" }
                    )
                }
            }
        }
    )

    if (showBottomSheet && selectedCommit != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            CommitDetailsSheet(
                commit = selectedCommit!!,
                gitManager = gitManager,
                onAction = { msg, type ->
                    statusMessage = msg
                    statusType = type
                    viewModel.loadLogs(reset = true)
                    showBottomSheet = false
                },
                onCopy = { copyToClipboard(it) }
            )
        }
    }
}

@Composable
fun TimelineCommitItem(
    commit: CommitItem,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val authorColor = remember(commit.author) { generateColorFromString(commit.author) }
    val cardShape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp).fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(
                    color = if (commit.isPushed) Color(0xFF2E7D32) else Color(0xFFE65100),
                    radius = size.minDimension / 2
                )
                drawCircle(
                    color = Color.White,
                    radius = size.minDimension / 2,
                    style = Stroke(width = 2f)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Commit Content Card
        ElevatedCard(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
                .clip(cardShape)
                .clickable(onClick = onClick),
            shape = cardShape,
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = authorColor,
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = commit.author.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = commit.author,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = getRelativeTime(commit.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = commit.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = commit.hash,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!commit.isPushed) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Unpushed",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommitDetailsSheet(
    commit: CommitItem,
    gitManager: GitManager,
    onAction: (String, SnackbarType) -> Unit,
    onCopy: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val fullDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.getDefault())

    Column(
        modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Commit, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.log_details_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onCopy(commit.hash) }) {
                Icon(Icons.Default.ContentCopy, stringResource(R.string.action_copy))
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = commit.message,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(24.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        LabelValueItem(stringResource(R.string.log_label_author), commit.author, Icons.Default.Person)
                        Spacer(Modifier.height(16.dp))
                        LabelValueItem(stringResource(R.string.log_label_hash), commit.hash, Icons.Default.Tag)
                    }
                    Column(Modifier.weight(1f)) {
                        LabelValueItem(stringResource(R.string.log_label_date), fullDateFormat.format(commit.date), Icons.Default.CalendarToday)
                        Spacer(Modifier.height(16.dp))
                        LabelValueItem(
                            stringResource(R.string.log_label_status),
                            if (commit.isPushed) stringResource(R.string.log_status_pushed) else stringResource(R.string.log_status_local),
                            if (commit.isPushed) Icons.Default.CloudDone else Icons.Default.CloudOff
                        )
                    }
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Text(
            text = stringResource(R.string.log_danger_zone),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedButton(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val res = gitManager.revertCommit(commit.hash)
                    withContext(Dispatchers.Main) {
                        val type = if (res.contains("failed", true)) SnackbarType.ERROR else SnackbarType.SUCCESS
                        onAction(res, type)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.Undo, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.log_btn_revert))
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val res = gitManager.resetToCommit(commit.hash, hard = false)
                    withContext(Dispatchers.Main) {
                        onAction(res, SnackbarType.WARNING)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
        ) {
            Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.log_btn_reset))
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun LabelValueItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp).padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun EmptyStateView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.log_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun getRelativeTime(date: Date): String {
    val now = Date().time
    val diff = now - date.time

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> stringResource(R.string.log_time_now)
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
    }
}

fun generateColorFromString(input: String): Color {
    val hash = abs(input.hashCode())
    val colors = listOf(
        Color(0xFFE57373), // Red
        Color(0xFFBA68C8), // Purple
        Color(0xFF7986CB), // Indigo
        Color(0xFF4FC3F7), // Light Blue
        Color(0xFF4DB6AC), // Teal
        Color(0xFFAED581), // Light Green
        Color(0xFFFFB74D), // Orange
        Color(0xFFA1887F)  // Brown
    )
    return colors[hash % colors.size]
}