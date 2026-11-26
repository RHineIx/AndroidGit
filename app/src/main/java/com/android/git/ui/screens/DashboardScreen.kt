package com.android.git.ui.screens

import com.android.git.ui.components.LinearProgressIndicator

import com.android.git.ui.components.CircularProgressIndicator

import com.android.git.ui.components.GitIcons as Icons

import com.android.git.ui.components.GitScaffold
import com.android.git.ui.components.ActionTile
import com.android.git.ui.components.AlertDialog
import com.android.git.ui.components.Button
import com.android.git.ui.components.Card
import com.android.git.ui.components.IconButton
import com.android.git.ui.components.TextButton

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.android.git.ui.components.ElevatedCard
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

    val cardShape = RoundedCornerShape(18.dp)

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

    GitScaffold(
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
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(paddingValues).padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 32.dp),
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
                        Text(
                            text = stringResource(R.string.dashboard_quick_actions),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                        )

                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth(),
                            shape = cardShape,
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { viewModel.pullChanges() },
                                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                                        enabled = !isLoading,
                                        shape = cardShape
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.dashboard_action_pull))
                                    }
                                    Button(
                                        onClick = { if (isForcePushChecked) showForcePushDialog = true else viewModel.pushChanges(force = false) },
                                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isForcePushChecked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                                        enabled = !isLoading,
                                        shape = cardShape
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (isForcePushChecked) stringResource(R.string.dashboard_btn_force_push) else stringResource(R.string.dashboard_action_push))
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isForcePushChecked,
                                        onCheckedChange = { isForcePushChecked = it },
                                        enabled = !isLoading
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.dashboard_allow_force), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.dashboard_repository_status),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp, top = 24.dp)
                        )

                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ElevatedCard(
                                onClick = onManageBranches,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                shape = cardShape,
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(Modifier.height(8.dp))
                                    Text(stringResource(R.string.dashboard_card_branch), style = MaterialTheme.typography.labelMedium)
                                    Text(dashboardState.branch, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                }
                            }
                            
                            ElevatedCard(
                                onClick = onViewChanges,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                shape = cardShape,
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Spacer(Modifier.height(8.dp))
                                    Text(stringResource(R.string.dashboard_card_changes), style = MaterialTheme.typography.labelMedium)
                                    Text("${dashboardState.changes} ${stringResource(R.string.dashboard_card_changes_suffix)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        ElevatedCard(
                            onClick = onViewLog,
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = cardShape,
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.dashboard_card_history), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(stringResource(R.string.dashboard_card_view_commits), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }

                        if (dashboardState.unpushedCount > 0) {
                            Spacer(Modifier.height(12.dp))
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShape,
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(Modifier.width(16.dp))
                                    Text(stringResource(R.string.dashboard_unpushed_commits, dashboardState.unpushedCount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.dashboard_tools_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp, top = 24.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ActionTile(
                                title = stringResource(R.string.dashboard_tool_stash),
                                icon = Icons.Default.Archive,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                enabled = !isLoading,
                                onClick = onOpenStash
                            )
                            ActionTile(
                                title = stringResource(R.string.dashboard_tool_merge),
                                icon = Icons.Default.Warning,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                enabled = !isLoading,
                                onClick = onMergeConflicts
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ActionTile(
                                title = stringResource(R.string.dashboard_tool_gitignore),
                                icon = Icons.Default.Code,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                enabled = !isLoading,
                                onClick = onIgnoreEditor
                            )
                            ActionTile(
                                title = stringResource(R.string.dashboard_tool_fetch),
                                icon = Icons.Default.Sync,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                enabled = !isLoading,
                                onClick = { viewModel.fetchAll() }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ActionTile(
                                title = stringResource(R.string.dashboard_tool_workflows),
                                icon = Icons.Default.PlayCircle,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                enabled = !isLoading,
                                onClick = onOpenWorkflows
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                    is DashboardState.Error -> {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                            Text(text = dashboardState.message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = paddingValues.calculateBottomPadding() + 16.dp, start = 16.dp, end = 16.dp)) {
                AppSnackbar(statusMessage, statusType) { viewModel.clearStatus() }
            }
        }
    }
}
