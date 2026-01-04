package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete // <--- تمت الإضافة هنا
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.git.data.GitManager
import com.android.git.model.CommitItem
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
    val gitManager = remember { GitManager(repoFile) }
    val scope = rememberCoroutineScope()
    
    var commits by remember { mutableStateOf<List<CommitItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }

    // Dialog State
    var selectedCommit by remember { mutableStateOf<CommitItem?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        onBack()
    }

    fun loadCommits() {
        scope.launch {
            isLoading = true
            commits = gitManager.getCommits()
            isLoading = false
        }
    }

    LaunchedEffect(repoFile) {
        loadCommits()
    }

    // Action Dialog for Commit
    if (showActionDialog && selectedCommit != null) {
        val commit = selectedCommit!!
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text("Commit Actions") },
            text = {
                Column {
                    Text("Selected: ${commit.hash}")
                    Text("Message: ${commit.message}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    
                    // Option 1: Reset Mixed (Keep Changes)
                    TextButton(
                        onClick = {
                            scope.launch {
                                statusMessage = gitManager.resetToCommit(commit.hash, hard = false)
                                showActionDialog = false
                                loadCommits()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset to here (Keep Changes)")
                    }
                    Text("Undoes commits after this, keeping files staged/modified.", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: Reset Hard (Discard Changes)
                    TextButton(
                        onClick = {
                            scope.launch {
                                statusMessage = gitManager.resetToCommit(commit.hash, hard = true)
                                showActionDialog = false
                                loadCommits()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset HARD (Discard All)", color = MaterialTheme.colorScheme.error)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Option 3: Checkout (Detached Head)
                    TextButton(
                        onClick = {
                            scope.launch {
                                statusMessage = gitManager.checkoutCommit(commit.hash)
                                showActionDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Visibility, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checkout (View State)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActionDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Commit History")
                        Text("${commits.size} commits", style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    items(commits) { commit ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            CommitCard(
                                commit = commit,
                                onClick = {
                                    selectedCommit = commit
                                    showActionDialog = true
                                }
                            )
                        }
                    }
                }
                
                if (commits.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No commits yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            // Status Snackbar/Message overlay
            if (statusMessage.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(statusMessage, color = MaterialTheme.colorScheme.inverseOnSurface)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { statusMessage = "" }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommitCard(
    commit: CommitItem,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Message
            Text(
                text = commit.message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Meta data row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Author
                Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = commit.author,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Hash
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = commit.hash,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Date
            Text(
                text = dateFormat.format(commit.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}