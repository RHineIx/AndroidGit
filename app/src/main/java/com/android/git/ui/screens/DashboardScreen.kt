package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.R
import com.android.git.data.GitManager
import com.android.git.model.DashboardState
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.viewmodel.MainViewModel
import java.io.File

@Composable
fun DashboardScreen(
    repoFile: File,
    gitManager: GitManager,
    viewModel: MainViewModel,
    dashboardState: DashboardState,
    onRefresh: () -> Unit,
    onViewChanges: () -> Unit,
    onSettings: () -> Unit,
    onViewLog: () -> Unit,
    onManageBranches: () -> Unit,
    onOpenStash: () -> Unit,
    onIgnoreEditor: () -> Unit,
    onCloseProject: () -> Unit,
    onMergeConflicts: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    var showForcePushDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var isForcePushChecked by remember { mutableStateOf(false) }

    val isLoading = viewModel.isLoading
    val statusMessage = viewModel.statusMessage
    val statusType = viewModel.statusType

    BackHandler(enabled = true) { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.dashboard_close_dialog_title)) },
            text = { Text(stringResource(R.string.dashboard_close_dialog_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onCloseProject()
                }) {
                    Text(stringResource(R.string.action_close), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    if (showForcePushDialog) {
        AlertDialog(
            onDismissRequest = { showForcePushDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.dashboard_force_push_title)) },
            text = { Text(stringResource(R.string.dashboard_force_push_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showForcePushDialog = false
                        viewModel.pushChanges(force = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.dashboard_btn_force_push)) }
            },
            dismissButton = { TextButton(onClick = { showForcePushDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = repoFile.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = repoFile.absolutePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row {
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "More Options") }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = DpOffset(0.dp, 8.dp),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = stringResource(R.string.dashboard_tools_title),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dashboard_tool_stash)) },
                                leadingIcon = { Icon(Icons.Default.Archive, null) },
                                onClick = { showMenu = false; onOpenStash() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dashboard_tool_merge)) },
                                leadingIcon = { Icon(Icons.Default.Warning, null) },
                                onClick = { showMenu = false; onMergeConflicts() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dashboard_tool_gitignore)) },
                                leadingIcon = { Icon(Icons.Default.Code, null) },
                                onClick = { showMenu = false; onIgnoreEditor() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dashboard_tool_fetch)) },
                                leadingIcon = { Icon(Icons.Default.Sync, null) },
                                onClick = { showMenu = false; viewModel.fetchAll() }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            when (dashboardState) {
                is DashboardState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is DashboardState.NotInitialized -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.dashboard_not_repo))
                            Button(onClick = { viewModel.initRepo() }) { Text(stringResource(R.string.dashboard_init_git)) }
                        }
                    }
                }
                is DashboardState.Success -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(stringResource(R.string.dashboard_quick_actions), fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { viewModel.pullChanges() }, modifier = Modifier.weight(1f), enabled = !isLoading) {
                                        Icon(Icons.Default.CloudDownload, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.dashboard_action_pull))
                                    }
                                    Button(
                                        onClick = { if (isForcePushChecked) showForcePushDialog = true else viewModel.pushChanges(force = false) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isForcePushChecked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                                        enabled = !isLoading
                                    ) {
                                        Icon(Icons.Default.CloudUpload, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (isForcePushChecked) stringResource(R.string.dashboard_action_force) else stringResource(R.string.dashboard_action_push))
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isForcePushChecked, onCheckedChange = { isForcePushChecked = it }, modifier = Modifier.scale(0.8f))
                                        Text(stringResource(R.string.dashboard_allow_force), fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                modifier = Modifier.weight(1f).clickable { onManageBranches() },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F6FEB).copy(alpha = 0.2f))
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.CallSplit, null, tint = Color(0xFF1F6FEB))
                                    Text(stringResource(R.string.dashboard_card_branch), fontSize = 12.sp)
                                    Text(dashboardState.branch, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f).clickable { onViewChanges() },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF238636).copy(alpha = 0.2f))
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.Refresh, null, tint = Color(0xFF238636))
                                    Text(stringResource(R.string.dashboard_card_changes), fontSize = 12.sp)
                                    Text("${dashboardState.changes} ${stringResource(R.string.dashboard_card_changes_suffix)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onViewLog() },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF8957E5).copy(alpha = 0.2f))
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, null, tint = Color(0xFF8957E5))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.dashboard_card_history), fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.dashboard_card_view_commits), fontSize = 12.sp)
                                }
                            }
                        }

                        if (dashboardState.unpushedCount > 0) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Upload, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(Modifier.width(12.dp))
                                    Text(stringResource(R.string.dashboard_unpushed_commits, dashboardState.unpushedCount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
                is DashboardState.Error -> Text("Error: ${dashboardState.message}", color = MaterialTheme.colorScheme.error)
            }
        }

        Box(Modifier.fillMaxSize().padding(bottom = 32.dp).navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
            AppSnackbar(statusMessage, statusType) { viewModel.clearStatus() }
        }
    }
}