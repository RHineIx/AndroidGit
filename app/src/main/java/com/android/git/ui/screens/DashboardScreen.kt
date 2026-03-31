package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.R
import com.android.git.data.GitManager
import com.android.git.model.DashboardState
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.viewmodel.MainViewModel
import java.io.File
// Explicitly import Miuix Checkbox to override Material 3 Checkbox
import top.yukonga.miuix.kmp.basic.Checkbox

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
    onMergeConflicts: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    var showForcePushDialog by remember { mutableStateOf(false) }
    var isForcePushChecked by remember { mutableStateOf(false) }

    val isLoading = viewModel.isLoading
    val statusMessage = viewModel.statusMessage
    val statusType = viewModel.statusType

    val cardShape = RoundedCornerShape(16.dp)

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = repoFile.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = repoFile.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                when (dashboardState) {
                    is DashboardState.Loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is DashboardState.NotInitialized -> {
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = cardShape
                        ) {
                            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.dashboard_not_repo), style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { viewModel.initRepo() }, shape = cardShape) {
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
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { viewModel.pullChanges() },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        enabled = !isLoading,
                                        shape = cardShape
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.dashboard_action_pull))
                                    }
                                    Button(
                                        onClick = { if (isForcePushChecked) showForcePushDialog = true else viewModel.pushChanges(force = false) },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isForcePushChecked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                                        enabled = !isLoading,
                                        shape = cardShape
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (isForcePushChecked) stringResource(R.string.dashboard_action_force) else stringResource(R.string.dashboard_action_push))
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isForcePushChecked,
                                        onCheckedChange = { isForcePushChecked = it },
                                        modifier = Modifier.scale(0.9f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.dashboard_allow_force), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Text(
                            text = "Repository Status",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp, top = 24.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ElevatedCard(
                                modifier = Modifier.weight(1f).clickable { onManageBranches() },
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                shape = cardShape,
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(Modifier.height(8.dp))
                                    Text(stringResource(R.string.dashboard_card_branch), style = MaterialTheme.typography.labelMedium)
                                    Text(dashboardState.branch, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                }
                            }
                            ElevatedCard(
                                modifier = Modifier.weight(1f).clickable { onViewChanges() },
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                shape = cardShape,
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Spacer(Modifier.height(8.dp))
                                    Text(stringResource(R.string.dashboard_card_changes), style = MaterialTheme.typography.labelMedium)
                                    Text("${dashboardState.changes} ${stringResource(R.string.dashboard_card_changes_suffix)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().clickable { onViewLog() },
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = cardShape,
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
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

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DashboardToolCard(
                                title = stringResource(R.string.dashboard_tool_stash),
                                icon = Icons.Default.Archive,
                                modifier = Modifier.weight(1f),
                                onClick = onOpenStash
                            )
                            DashboardToolCard(
                                title = stringResource(R.string.dashboard_tool_merge),
                                icon = Icons.Default.Warning,
                                modifier = Modifier.weight(1f),
                                onClick = onMergeConflicts
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DashboardToolCard(
                                title = stringResource(R.string.dashboard_tool_gitignore),
                                icon = Icons.Default.Code,
                                modifier = Modifier.weight(1f),
                                onClick = onIgnoreEditor
                            )
                            DashboardToolCard(
                                title = stringResource(R.string.dashboard_tool_fetch),
                                icon = Icons.Default.Sync,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.fetchAll() }
                            )
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                    is DashboardState.Error -> {
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth(),
                            shape = cardShape
                        ) {
                            Text(
                                text = dashboardState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp, start = 16.dp, end = 16.dp)) {
                AppSnackbar(statusMessage, statusType) { viewModel.clearStatus() }
            }
        }
    }
}

@Composable
fun DashboardToolCard(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ElevatedCard(
        modifier = modifier.height(90.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}