package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.data.GitManager
import com.android.git.model.StashItem
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.SnackbarType
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StashScreen(
    repoFile: File,
    onBack: () -> Unit
) {
    val gitManager = remember { GitManager(repoFile) }
    val scope = rememberCoroutineScope()

    var stashes by remember { mutableStateOf<List<StashItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var statusMessage by remember { mutableStateOf("") }
    var statusType by remember { mutableStateOf(SnackbarType.INFO) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var stashMessage by remember { mutableStateOf("") }
    
    var showActionDialog by remember { mutableStateOf(false) }
    var selectedStash by remember { mutableStateOf<StashItem?>(null) }

    BackHandler(enabled = true) { onBack() }

    fun loadStashes() {
        scope.launch {
            isLoading = true
            stashes = gitManager.getStashList()
            isLoading = false
        }
    }

    LaunchedEffect(repoFile) { loadStashes() }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Stash Changes") },
            text = {
                OutlinedTextField(
                    value = stashMessage,
                    onValueChange = { stashMessage = it },
                    label = { Text("Message (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val msg = gitManager.stashChanges(stashMessage)
                        statusMessage = msg
                        statusType = if (msg.contains("No changes")) SnackbarType.WARNING else SnackbarType.SUCCESS
                        showCreateDialog = false
                        stashMessage = ""
                        loadStashes()
                    }
                }) { Text("Stash") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }

    if (showActionDialog && selectedStash != null) {
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text("Stash Actions") },
            text = { Text("What do you want to do with this stash?") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val res = gitManager.applyStash(selectedStash!!.index, drop = true)
                        statusMessage = res
                        statusType = SnackbarType.SUCCESS
                        showActionDialog = false
                        loadStashes()
                    }
                }) { Text("Pop (Apply & Drop)") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    scope.launch {
                        val res = gitManager.applyStash(selectedStash!!.index, drop = false)
                        statusMessage = res
                        showActionDialog = false
                    }
                }) { Text("Apply (Keep)") }
            },
            icon = { Icon(Icons.Default.Archive, null) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stash Shelf") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (stashes.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Archive, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text("Stash list is empty", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(stashes) { stash ->
                            StashItemView(
                                stash = stash,
                                onClick = {
                                    selectedStash = stash
                                    showActionDialog = true
                                },
                                onDrop = {
                                    scope.launch {
                                        val res = gitManager.dropStash(stash.index)
                                        statusMessage = res
                                        loadStashes()
                                    }
                                }
                            )
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                AppSnackbar(message = statusMessage, type = statusType, onDismiss = { statusMessage = "" })
            }
        }
    }
}

@Composable
fun StashItemView(
    stash: StashItem,
    onClick: () -> Unit,
    onDrop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stash.message.ifEmpty { "WIP on ${stash.hash}" },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "stash@{${stash.index}} • ${stash.hash}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        IconButton(onClick = onDrop) {
            Icon(Icons.Default.Delete, "Drop", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
        }
    }
}