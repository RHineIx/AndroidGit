package com.android.git.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.data.GitManager
import com.android.git.model.CommitItem
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.SnackbarType
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    repoFile: File,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gitManager = remember { GitManager(repoFile) }
    val scope = rememberCoroutineScope()
    
    var commits by remember { mutableStateOf<List<CommitItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // UI Feedback
    var statusMessage by remember { mutableStateOf("") }
    var statusType by remember { mutableStateOf(SnackbarType.INFO) }

    // Bottom Sheet State
    var selectedCommit by remember { mutableStateOf<CommitItem?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = true) { onBack() }

    fun loadCommits() {
        scope.launch {
            isLoading = true
            commits = gitManager.getCommits()
            isLoading = false
        }
    }

    LaunchedEffect(repoFile) { loadCommits() }

    // Helper to copy text
    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Commit Hash", text)
        clipboard.setPrimaryClip(clip)
        statusMessage = "Hash copied to clipboard"; statusType = SnackbarType.SUCCESS
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Commit History", fontWeight = FontWeight.Bold)
                        Text("${commits.size} commits", style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadCommits() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(commits) { commit ->
                        CommitCard(
                            commit = commit,
                            onClick = {
                                selectedCommit = commit
                                showBottomSheet = true
                            }
                        )
                    }
                }
                
                if (commits.isEmpty() && !isLoading) {
                    Text("No commits found.", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.secondary)
                }
            }

            // Enhanced Toast (Snackbar)
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
                AppSnackbar(
                    message = statusMessage,
                    type = statusType,
                    onDismiss = { statusMessage = "" }
                )
            }
        }
    }

    // --- Advanced Commit Actions Sheet ---
    if (showBottomSheet && selectedCommit != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            val commit = selectedCommit!!
            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault())

            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                
                // Header: Hash & Copy
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = commit.hash,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { copyToClipboard(commit.hash) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, "Copy", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Message
                Text(
                    text = commit.message,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Author & Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(commit.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(16.dp))
                    Text(dateFormat.format(commit.date), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Divider(modifier = Modifier.padding(vertical = 24.dp))

                Text("Actions", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))

                // Actions Grid
                ActionItem(
                    icon = Icons.Default.Visibility,
                    label = "Checkout (View)",
                    desc = "Switch to this state (Detached HEAD)",
                    onClick = {
                        scope.launch {
                            val res = gitManager.checkoutCommit(commit.hash)
                            statusMessage = res; statusType = if(res.contains("failed")) SnackbarType.ERROR else SnackbarType.SUCCESS
                            showBottomSheet = false
                        }
                    }
                )
                
                ActionItem(
                    icon = Icons.Default.Undo,
                    label = "Revert Commit",
                    desc = "Create a new commit that undoes this one",
                    onClick = {
                        scope.launch {
                            val res = gitManager.revertCommit(commit.hash)
                            statusMessage = res; statusType = if(res.contains("failed")) SnackbarType.ERROR else SnackbarType.SUCCESS
                            loadCommits()
                            showBottomSheet = false
                        }
                    }
                )

                ActionItem(
                    icon = Icons.Default.CallMerge,
                    label = "Cherry-Pick",
                    desc = "Apply this commit to current branch",
                    onClick = {
                        scope.launch {
                            val res = gitManager.cherryPickCommit(commit.hash)
                            statusMessage = res; statusType = if(res.contains("failed")) SnackbarType.ERROR else SnackbarType.SUCCESS
                            showBottomSheet = false
                        }
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                ActionItem(
                    icon = Icons.Default.Restore,
                    label = "Reset (Soft/Mixed)",
                    desc = "Undo commits, keep file changes",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        scope.launch {
                            val res = gitManager.resetToCommit(commit.hash, hard = false)
                            statusMessage = res; statusType = SnackbarType.WARNING
                            loadCommits()
                            showBottomSheet = false
                        }
                    }
                )

                ActionItem(
                    icon = Icons.Default.Delete,
                    label = "Reset HARD",
                    desc = "Discard all changes after this point",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        scope.launch {
                            val res = gitManager.resetToCommit(commit.hash, hard = true)
                            statusMessage = res; statusType = SnackbarType.ERROR
                            loadCommits()
                            showBottomSheet = false
                        }
                    }
                )
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ActionItem(
    icon: ImageVector,
    label: String,
    desc: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CommitCard(commit: CommitItem, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Timeline line visual could go here, for now simple layout
            Column {
                Text(
                    text = commit.message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                    Text(commit.author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Text(dateFormat.format(commit.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        }
    }
}