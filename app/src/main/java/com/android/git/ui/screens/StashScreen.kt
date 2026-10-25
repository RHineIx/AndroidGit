package com.android.git.ui.screens

import com.android.git.ui.components.GitIcons as Icons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.R
import com.android.git.ui.components.CircularProgressIndicator
import io.github.suqi8.coui.kmp.basic.FloatingActionButton
import com.android.git.ui.components.AlertDialog
import com.android.git.ui.components.Button
import com.android.git.ui.components.Card
import com.android.git.ui.components.IconButton
import com.android.git.ui.components.OutlinedButton
import com.android.git.ui.components.OutlinedTextField
import com.android.git.ui.components.TextButton
import com.android.git.ui.components.OplusScaffold
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

    // Fetch the string resource outside of the Coroutine to satisfy Lint requirements
    val errorMsgTemplate = stringResource(R.string.stash_load_error)

    fun loadStashes() {
        scope.launch {
            isLoading = true
            try {
                stashes = gitManager.getStashList()
                statusMessage = ""
            } catch (e: Exception) {
                stashes = emptyList()
                statusMessage = errorMsgTemplate.format(e.message ?: "Unknown error")
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

    OplusScaffold(
        title = stringResource(R.string.stash_title),
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = { if (!isLoading && !isActionRunning) showCreateDialog = true }) {
                Icon(Icons.Default.Add, stringResource(R.string.stash_dialog_create_title), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (stashes.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Archive, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.stash_empty), color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stashes) { stash ->
                            Card(modifier = Modifier.fillMaxWidth()) {
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
                            }
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
        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stash.message.ifEmpty { "${stringResource(R.string.stash_wip_prefix)} ${stash.hash}" },
                fontWeight = FontWeight.Medium,
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
