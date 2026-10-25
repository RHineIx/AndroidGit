package com.android.git.ui.screens

import com.android.git.ui.components.LinearProgressIndicator

import com.android.git.ui.components.CircularProgressIndicator

import com.android.git.ui.components.GitIcons as Icons

import com.android.git.ui.components.OplusScaffold
import com.android.git.ui.components.OplusSectionTitle
import com.android.git.ui.components.OplusPreferenceGroup
import com.android.git.ui.components.OplusPreferenceRow
import com.android.git.ui.components.OplusPreferenceDivider
import com.android.git.ui.components.AlertDialog
import com.android.git.ui.components.Button
import com.android.git.ui.components.Card
import com.android.git.ui.components.IconButton
import com.android.git.ui.components.Switch
import com.android.git.ui.components.TextButton

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.data.GitManager
import com.android.git.model.DashboardState
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
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
    onMergeConflicts: () -> Unit,
    onOpenWorkflows: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    var showForcePushDialog by remember { mutableStateOf(false) }
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
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
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

    OplusScaffold(
        title = repoFile.name,
        onBack = { showExitDialog = true },
        actions = {
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(paddingValues).padding(horizontal = 12.dp).padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = repoFile.absolutePath,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                when (dashboardState) {
                    is DashboardState.Loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is DashboardState.NotInitialized -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.dashboard_not_repo), style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { viewModel.initRepo() }, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.dashboard_init_git))
                                }
                            }
                        }
                    }
                    is DashboardState.Success -> {
                        OplusSectionTitle(title = stringResource(R.string.dashboard_quick_actions))
                        OplusPreferenceGroup {
                            OplusPreferenceRow(
                                title = stringResource(R.string.dashboard_action_pull),
                                icon = Icons.Default.CloudDownload,
                                enabled = !isLoading,
                                onClick = { viewModel.pullChanges() },
                                trailing = {}
                            )
                            OplusPreferenceDivider()
                            OplusPreferenceRow(
                                title = if (isForcePushChecked) stringResource(R.string.dashboard_btn_force_push) else stringResource(R.string.dashboard_action_push),
                                icon = Icons.Default.CloudUpload,
                                enabled = !isLoading,
                                onClick = { if (isForcePushChecked) showForcePushDialog = true else viewModel.pushChanges(force = false) },
                                trailing = {
                                    if (isForcePushChecked) Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                            OplusPreferenceDivider()
                            OplusPreferenceRow(
                                title = stringResource(R.string.dashboard_allow_force),
                                icon = Icons.Default.Warning,
                                enabled = !isLoading,
                                onClick = { isForcePushChecked = !isForcePushChecked },
                                trailing = {
                                    Switch(checked = isForcePushChecked, onCheckedChange = { isForcePushChecked = it }, enabled = !isLoading)
                                }
                            )
                        }
                        OplusSectionTitle(title = stringResource(R.string.dashboard_repository_status))
                        OplusPreferenceGroup {
                            OplusPreferenceRow(
                                title = stringResource(R.string.dashboard_card_branch),
                                summary = dashboardState.branch,
                                icon = Icons.AutoMirrored.Filled.CallSplit,
                                onClick = onManageBranches
                            )
                            OplusPreferenceDivider()
                            OplusPreferenceRow(
                                title = stringResource(R.string.dashboard_card_changes),
                                summary = "${dashboardState.changes} ${stringResource(R.string.dashboard_card_changes_suffix)}",
                                icon = Icons.Default.Refresh,
                                onClick = onViewChanges
                            )
                            OplusPreferenceDivider()
                            OplusPreferenceRow(
                                title = stringResource(R.string.dashboard_card_history),
                                summary = stringResource(R.string.dashboard_card_view_commits),
                                icon = Icons.Default.History,
                                onClick = onViewLog
                            )
                        }
                        if (dashboardState.unpushedCount > 0) {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.dashboard_unpushed_commits, dashboardState.unpushedCount), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        OplusSectionTitle(title = stringResource(R.string.dashboard_tools_title))
                        OplusPreferenceGroup {
                            OplusPreferenceRow(title = stringResource(R.string.dashboard_tool_stash), icon = Icons.Default.Archive, enabled = !isLoading, onClick = onOpenStash)
                            OplusPreferenceDivider()
                            OplusPreferenceRow(title = stringResource(R.string.dashboard_tool_merge), icon = Icons.Default.Warning, enabled = !isLoading, onClick = onMergeConflicts)
                            OplusPreferenceDivider()
                            OplusPreferenceRow(title = stringResource(R.string.dashboard_tool_gitignore), icon = Icons.Default.Code, enabled = !isLoading, onClick = onIgnoreEditor)
                            OplusPreferenceDivider()
                            OplusPreferenceRow(title = stringResource(R.string.dashboard_tool_fetch), icon = Icons.Default.Sync, enabled = !isLoading, onClick = { viewModel.fetchAll() })
                            OplusPreferenceDivider()
                            OplusPreferenceRow(title = stringResource(R.string.dashboard_tool_workflows), icon = Icons.Default.PlayCircle, enabled = !isLoading, onClick = onOpenWorkflows)
                        }
                    }
                    is DashboardState.Error -> {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                            Text(text = dashboardState.message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = paddingValues.calculateBottomPadding() + 16.dp, start = 12.dp, end = 12.dp)) {
                AppSnackbar(statusMessage, statusType) { viewModel.clearStatus() }
            }
        }
    }
}
