package com.android.git.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.R
import com.android.git.data.GitManager
import com.android.git.model.StashItem
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.SnackbarType
import com.android.git.utils.isGitFailureMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StashScreen(
    gitManager: GitManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var stashes by remember { mutableStateOf<List<StashItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var statusMessage by remember { mutableStateOf("") }
    var statusType by remember { mutableStateOf(SnackbarType.INFO) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var stashMessage by remember { mutableStateOf("") }
    
    var showActionDialog by remember { mutableStateOf(false) }
    var showDropDialog by remember { mutableStateOf(false) }
    var selectedStash by remember { mutableStateOf<StashItem?>(null) }
    var isActionRunning by remember { mutableStateOf(false) }

    fun loadStashes() {
        scope.launch {
            isLoading = true
            try {
                stashes = gitManager.getStashList()
                statusMessage = ""
            } catch (e: Exception) {
                stashes = emptyList()
                statusMessage = context.getString(R.string.stash_load_error, e.message ?: "Unknown error")
                statusType = SnackbarType.ERROR
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(gitManager) { loadStashes() }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.stash_dialog_create_title)) },
            text = {
                OutlinedTextField(
                    value = stashMessage,
                    onValueChange = { stashMessage = it },
                    label = { Text(stringResource(R.string.stash_dialog_msg_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isActionRunning) return@Button
                        isActionRunning = true
                        showCreateDialog = false
                        scope.launch {
                            val msg = gitManager.stashChanges(stashMessage)
                            statusMessage = msg
                            statusType = stashSnackbarType(msg)
                            stashMessage = ""
                            isActionRunning = false
                            loadStashes()
                        }
                    },
                    enabled = !isActionRunning
                ) { Text(stringResource(R.string.stash_dialog_create_title)) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    if (showActionDialog && selectedStash != null) {
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text(stringResource(R.string.stash_dialog_action_title)) },
            text = { Text(stringResource(R.string.stash_dialog_action_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        if (isActionRunning) return@Button
                        isActionRunning = true
                        scope.launch {
                            val res = gitManager.applyStash(selectedStash!!.index, drop = true)
                            statusMessage = res
                            statusType = stashSnackbarType(res)
                            showActionDialog = false
                            isActionRunning = false
                            loadStashes()
                        }
                    },
                    enabled = !isActionRunning
                ) { Text(stringResource(R.string.stash_btn_pop)) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        if (isActionRunning) return@OutlinedButton
                        isActionRunning = true
                        scope.launch {
                            val res = gitManager.applyStash(selectedStash!!.index, drop = false)
                            statusMessage = res
                            statusType = stashSnackbarType(res)
                            showActionDialog = false
                            isActionRunning = false
                            loadStashes()
                        }
                    },
                    enabled = !isActionRunning
                ) { Text(stringResource(R.string.stash_btn_apply)) }
            },
            icon = { Icon(Icons.Default.Archive, null) }
        )
    }

    if (showDropDialog && selectedStash != null) {
        AlertDialog(
            onDismissRequest = { if (!isActionRunning) showDropDialog = false },
            title = { Text(stringResource(R.string.stash_drop_title)) },
            text = { Text(stringResource(R.string.stash_drop_msg, selectedStash!!.index)) },
            confirmButton = {
                Button(
                    onClick = {
                        if (isActionRunning) return@Button
                        isActionRunning = true
                        showDropDialog = false
                        scope.launch {
                            val res = gitManager.dropStash(selectedStash!!.index)
                            statusMessage = res
                            statusType = stashSnackbarType(res)
                            isActionRunning = false
                            loadStashes()
                        }
                    },
                    enabled = !isActionRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.stash_drop_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDropDialog = false }, enabled = !isActionRunning) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stash_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { if (!isLoading && !isActionRunning) showCreateDialog = true }) {
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
                        Text(stringResource(R.string.stash_empty), color = MaterialTheme.colorScheme.secondary)
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
                                    selectedStash = stash
                                    showDropDialog = true
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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

private fun stashSnackbarType(message: String): SnackbarType {
    val normalized = message.trim().lowercase()
    return when {
        isGitFailureMessage(message) -> SnackbarType.ERROR
        normalized.contains("no changes") -> SnackbarType.WARNING
        else -> SnackbarType.SUCCESS
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
                text = stash.message.ifEmpty { "${stringResource(R.string.stash_wip_prefix)} ${stash.hash}" },
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
            Icon(Icons.Default.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
        }
    }
}