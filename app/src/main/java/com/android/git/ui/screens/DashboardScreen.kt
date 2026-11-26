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
import androidx.compose.ui.text.font.FontWeight
import com.android.git.ui.components.ElevatedCard
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.android.git.ui.components.SectionTitle
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

    val cardShape = MaterialTheme.shapes.medium

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
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
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
                        SectionTitle(stringResource(R.string.dashboard_quick_actions))
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                            shape = cardShape
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { viewModel.pullChanges() }, modifier = Modifier.weight(1f),
                                        enabled = !isLoading, shape = cardShape) {
                                        Icon(Icons.Default.CloudDownload, null, Modifier.size(20.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.dashboard_action_pull))
                                    }
                                    Button(
                                        onClick = { if (isForcePushChecked) showForcePushDialog = true else viewModel.pushChanges(force = false) },
                                        modifier = Modifier.weight(1f), enabled = !isLoading, shape = cardShape,
                                        colors = if (isForcePushChecked) ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError
                                        ) else ButtonDefaults.buttonColors()
                                    ) {
                                        Icon(Icons.Default.CloudUpload, null, Modifier.size(20.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(if (isForcePushChecked) R.string.dashboard_btn_force_push else R.string.dashboard_action_push))
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isForcePushChecked, onCheckedChange = { isForcePushChecked = it }, enabled = !isLoading)
                                    Text(stringResource(R.string.dashboard_allow_force), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        SectionTitle(stringResource(R.string.dashboard_repository_status))
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RepositoryStatusCard(
                                title = stringResource(R.string.dashboard_card_branch),
                                value = dashboardState.branch,
                                icon = Icons.AutoMirrored.Filled.CallSplit,
                                onClick = onManageBranches,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            RepositoryStatusCard(
                                title = stringResource(R.string.dashboard_card_changes),
                                value = "${dashboardState.changes} ${stringResource(R.string.dashboard_card_changes_suffix)}",
                                icon = Icons.Default.Refresh,
                                onClick = onViewChanges,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                        RepositoryStatusCard(
                            title = stringResource(R.string.dashboard_card_history),
                            value = stringResource(R.string.dashboard_card_view_commits),
                            icon = Icons.Default.History,
                            onClick = onViewLog,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (dashboardState.unpushedCount > 0) {
                            Card(modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Upload, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.dashboard_unpushed_commits, dashboardState.unpushedCount),
                                        color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        SectionTitle(stringResource(R.string.dashboard_tools_title))
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionTile(stringResource(R.string.dashboard_tool_stash), Icons.Default.Archive, onOpenStash,
                                Modifier.weight(1f).fillMaxHeight(), enabled = !isLoading)
                            ActionTile(stringResource(R.string.dashboard_tool_merge), Icons.Default.Warning, onMergeConflicts,
                                Modifier.weight(1f).fillMaxHeight(), enabled = !isLoading)
                        }
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionTile(stringResource(R.string.dashboard_tool_gitignore), Icons.Default.Code, onIgnoreEditor,
                                Modifier.weight(1f).fillMaxHeight(), enabled = !isLoading)
                            ActionTile(stringResource(R.string.dashboard_tool_fetch), Icons.Default.Sync, { viewModel.fetchAll() },
                                Modifier.weight(1f).fillMaxHeight(), enabled = !isLoading)
                        }
                        ActionTile(stringResource(R.string.dashboard_tool_workflows), Icons.Default.PlayCircle, onOpenWorkflows,
                            Modifier.fillMaxWidth(), enabled = !isLoading)
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


@Composable
private fun RepositoryStatusCard(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    ElevatedCard(
        onClick = onClick, modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(22.dp), tint = colors.onPrimaryContainer)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = colors.onPrimaryContainer)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onPrimaryContainer)
            }
        }
    }
}
