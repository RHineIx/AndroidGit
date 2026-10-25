package com.android.git.ui.screens

import com.android.git.ui.components.GitIcons as Icons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.R
import com.android.git.ui.components.CircularProgressIndicator
import com.android.git.ui.components.DropdownMenu
import io.github.suqi8.coui.kmp.basic.FloatingActionButton
import io.github.suqi8.coui.kmp.basic.TabRowWithContour
import com.android.git.ui.components.AlertDialog
import com.android.git.ui.components.Button
import com.android.git.ui.components.Card
import com.android.git.ui.components.DropdownMenuItem
import com.android.git.ui.components.ElevatedCard
import com.android.git.ui.components.IconButton
import com.android.git.ui.components.OutlinedTextField
import com.android.git.ui.components.TextButton
import com.android.git.ui.components.OplusScaffold
import com.android.git.model.BranchModel
import com.android.git.model.BranchType
import com.android.git.model.DashboardState
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.viewmodel.MainViewModel

private sealed interface BranchConfirmation {
    data class Delete(val branch: BranchModel) : BranchConfirmation
    data class Merge(val branch: BranchModel) : BranchConfirmation
    data class Rebase(val branch: BranchModel) : BranchConfirmation
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchManagerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val branches = viewModel.branchList
    val isLoading = viewModel.isLoading
    val statusMessage = viewModel.statusMessage
    val statusType = viewModel.statusType

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var branchToRename by remember { mutableStateOf<BranchModel?>(null) }
    var confirmation by remember { mutableStateOf<BranchConfirmation?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadBranches()
    }

    val currentBranch = (viewModel.dashboardState as? DashboardState.Success)?.branch
    val localCount = branches.count { it.type == BranchType.LOCAL }
    val remoteCount = branches.count { it.type == BranchType.REMOTE }

    BranchDialogs(
        showCreateDialog = showCreateDialog,
        showRenameDialog = showRenameDialog,
        branchToRename = branchToRename,
        isBusy = isLoading,
        onDismissCreate = { showCreateDialog = false },
        onDismissRename = {
            showRenameDialog = false
            branchToRename = null
        },
        onCreate = { name ->
            showCreateDialog = false
            viewModel.createBranch(name)
        },
        onRename = { name ->
            showRenameDialog = false
            branchToRename = null
            viewModel.renameBranch(name)
        }
    )

    confirmation?.let { pending ->
        BranchConfirmationDialog(
            confirmation = pending,
            isBusy = isLoading,
            onDismiss = { if (!isLoading) confirmation = null },
            onConfirm = {
                confirmation = null
                when (pending) {
                    is BranchConfirmation.Delete -> viewModel.deleteBranch(pending.branch.fullPath)
                    is BranchConfirmation.Merge -> viewModel.mergeBranch(pending.branch.fullPath)
                    is BranchConfirmation.Rebase -> viewModel.rebaseBranch(pending.branch.fullPath)
                }
            },
            onForceDelete = {
                val branch = (pending as? BranchConfirmation.Delete)?.branch
                if (branch != null) {
                    confirmation = null
                    viewModel.forceDeleteBranch(branch.fullPath)
                }
            }
        )
    }

    OplusScaffold(
        title = stringResource(R.string.branch_title),
        onBack = onBack,
        backEnabled = !isLoading,
        actions = {
            IconButton(onClick = { viewModel.fetchAll() }, enabled = !isLoading) {
                Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.branch_fetch))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!isLoading) showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.branch_create_action), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
                BranchListContent(
                    padding = padding,
                    header = {
                        BranchHeader(
                            currentBranch = currentBranch,
                            localCount = localCount,
                            remoteCount = remoteCount,
                            isLoading = isLoading
                        )
                    },
                    branches = branches,
                    searchQuery = searchQuery,
                    selectedTab = selectedTab,
                    isLoading = isLoading,
                    onSearchChange = { searchQuery = it },
                    onTabChange = { selectedTab = it },
                    onAction = { action, branch ->
                        when (action) {
                            BranchAction.CHECKOUT -> viewModel.checkoutBranch(branch.fullPath)
                            BranchAction.DELETE -> confirmation = BranchConfirmation.Delete(branch)
                            BranchAction.MERGE -> confirmation = BranchConfirmation.Merge(branch)
                            BranchAction.REBASE -> confirmation = BranchConfirmation.Rebase(branch)
                            BranchAction.RENAME -> {
                                branchToRename = branch
                                showRenameDialog = true
                            }
                        }
                    }
                )

            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                AppSnackbar(message = statusMessage, type = statusType, onDismiss = { viewModel.clearStatus() })
            }
        }
    }
}

@Composable
private fun BranchHeader(
    currentBranch: String?,
    localCount: Int,
    remoteCount: Int,
    isLoading: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (currentBranch.isNullOrBlank()) {
                    stringResource(R.string.branch_no_current)
                } else {
                    stringResource(R.string.branch_current_fmt, currentBranch)
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.branch_local_count, localCount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.branch_remote_count, remoteCount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun BranchDialogs(
    showCreateDialog: Boolean,
    showRenameDialog: Boolean,
    branchToRename: BranchModel?,
    isBusy: Boolean,
    onDismissCreate: () -> Unit,
    onDismissRename: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String) -> Unit
) {
    if (showCreateDialog) {
        InputBranchDialog(
            title = stringResource(R.string.branch_dialog_create_title),
            confirmText = stringResource(R.string.action_create),
            initialValue = "",
            isBusy = isBusy,
            onDismiss = onDismissCreate,
            onConfirm = onCreate
        )
    }

    if (showRenameDialog && branchToRename != null) {
        InputBranchDialog(
            title = stringResource(R.string.branch_dialog_rename_title),
            textPrefix = stringResource(R.string.branch_dialog_rename_prefix, branchToRename.name),
            confirmText = stringResource(R.string.action_rename),
            initialValue = branchToRename.name,
            isBusy = isBusy,
            onDismiss = onDismissRename,
            onConfirm = onRename
        )
    }
}

@Composable
private fun BranchConfirmationDialog(
    confirmation: BranchConfirmation,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onForceDelete: () -> Unit
) {
    val branch = when (confirmation) {
        is BranchConfirmation.Delete -> confirmation.branch
        is BranchConfirmation.Merge -> confirmation.branch
        is BranchConfirmation.Rebase -> confirmation.branch
    }
    val isDelete = confirmation is BranchConfirmation.Delete
    val title = when (confirmation) {
        is BranchConfirmation.Delete -> stringResource(R.string.branch_delete_title)
        is BranchConfirmation.Merge -> stringResource(R.string.branch_merge_confirm_title)
        is BranchConfirmation.Rebase -> stringResource(R.string.branch_rebase_confirm_title)
    }
    val message = when (confirmation) {
        is BranchConfirmation.Delete -> stringResource(R.string.branch_delete_msg, branch.name)
        is BranchConfirmation.Merge -> stringResource(R.string.branch_merge_confirm_msg, branch.name)
        is BranchConfirmation.Rebase -> stringResource(R.string.branch_rebase_confirm_msg, branch.name)
    }
    val confirmText = when (confirmation) {
        is BranchConfirmation.Delete -> stringResource(R.string.branch_delete_confirm)
        is BranchConfirmation.Merge -> stringResource(R.string.branch_merge_confirm)
        is BranchConfirmation.Rebase -> stringResource(R.string.branch_rebase_confirm)
    }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text(title) },
        text = { Text(message) },
        icon = {
            Icon(
                imageVector = if (isDelete) Icons.Default.Delete else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isBusy,
                colors = if (isDelete) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else ButtonDefaults.buttonColors()
            ) { Text(confirmText) }
        },
        neutralButton = if (isDelete) {
            {
                TextButton(onClick = onForceDelete, enabled = !isBusy) {
                    Text(stringResource(R.string.branch_force_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        } else null,
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun InputBranchDialog(
    title: String,
    textPrefix: String? = null,
    confirmText: String,
    initialValue: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember(title, initialValue) { mutableStateOf(initialValue) }
    val trimmedValue = textValue.trim()

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text(title) },
        text = {
            Column {
                if (textPrefix != null) {
                    Text(textPrefix, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(stringResource(R.string.branch_dialog_create_label)) },
                    supportingText = { Text(stringResource(R.string.branch_name_help)) },
                    singleLine = true,
                    enabled = !isBusy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(trimmedValue) },
                enabled = trimmedValue.isNotEmpty() && !isBusy
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun BranchListContent(
    padding: PaddingValues,
    header: @Composable () -> Unit,
    branches: List<BranchModel>,
    searchQuery: String,
    selectedTab: Int,
    isLoading: Boolean,
    onSearchChange: (String) -> Unit,
    onTabChange: (Int) -> Unit,
    onAction: (BranchAction, BranchModel) -> Unit
) {
    val targetType = if (selectedTab == 0) BranchType.LOCAL else BranchType.REMOTE
    val normalizedQuery = searchQuery.trim()
    val filteredBranches = branches.filter { branch ->
        branch.type == targetType && (
            normalizedQuery.isBlank() ||
                branch.name.contains(normalizedQuery, ignoreCase = true) ||
                branch.fullPath.contains(normalizedQuery, ignoreCase = true) ||
                branch.remoteName.orEmpty().contains(normalizedQuery, ignoreCase = true)
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 96.dp
        )
    ) {
        item(key = "branch-summary") { header() }
        item(key = "branch-search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.branch_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Text("×", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !isLoading
            )
        }
        item(key = "branch-tabs") {
            TabRowWithContour(
                tabs = listOf(stringResource(R.string.branch_tab_local), stringResource(R.string.branch_tab_remote)),
                selectedTabIndex = selectedTab,
                onTabSelected = onTabChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        when {
            isLoading && branches.isEmpty() -> item(key = "branch-loading") {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            filteredBranches.isEmpty() -> item(key = "branch-empty") {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (normalizedQuery.isBlank()) {
                            stringResource(R.string.branch_empty_tab)
                        } else {
                            stringResource(R.string.branch_empty_search)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> items(filteredBranches, key = { it.fullPath }) { branch ->
                BranchItemRich(
                    branch = branch,
                    enabled = !isLoading,
                    onAction = { action -> onAction(action, branch) }
                )
            }
        }
    }
}

enum class BranchAction { CHECKOUT, MERGE, REBASE, RENAME, DELETE }

@Composable
fun BranchItemRich(
    branch: BranchModel,
    enabled: Boolean = true,
    onAction: (BranchAction) -> Unit
) {
    var showMenu by remember(branch.fullPath) { mutableStateOf(false) }
    val isRemote = branch.type == BranchType.REMOTE

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { if (enabled) showMenu = true },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isRemote) Icons.Default.Cloud else Icons.Default.Computer,
                contentDescription = if (isRemote) stringResource(R.string.branch_remote) else stringResource(R.string.branch_local),
                modifier = Modifier.size(22.dp),
                tint = if (branch.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = branch.name,
                    fontWeight = if (branch.isCurrent) FontWeight.Medium else FontWeight.Normal,
                    color = if (branch.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val remoteName = branch.remoteName
                val trackingName = branch.trackingName
                val secondary = when {
                    isRemote && !remoteName.isNullOrBlank() ->
                        stringResource(R.string.branch_remote_fmt, remoteName)
                    !trackingName.isNullOrBlank() ->
                        stringResource(R.string.branch_tracking_fmt, trackingName)
                    else -> null
                }
                if (secondary != null) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (branch.isCurrent) {
                    Text(
                        text = stringResource(R.string.branch_current_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (branch.isUpstreamGone) {
                    Text(
                        text = stringResource(R.string.branch_upstream_gone),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (!isRemote && !branch.isUpstreamGone && (branch.aheadCount > 0 || branch.behindCount > 0)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (branch.aheadCount > 0) {
                            BranchCount(
                                icon = Icons.Default.ArrowUpward,
                                text = stringResource(R.string.branch_ahead_fmt, branch.aheadCount),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (branch.behindCount > 0) {
                            BranchCount(
                                icon = Icons.Default.ArrowDownward,
                                text = stringResource(R.string.branch_behind_fmt, branch.behindCount),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            IconButton(onClick = { if (enabled) showMenu = true }, enabled = enabled) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.branch_actions))
            }

            BranchActionMenu(
                expanded = showMenu && enabled,
                branch = branch,
                isRemote = isRemote,
                onDismiss = { showMenu = false },
                onAction = onAction
            )
        }
    }
}

@Composable
private fun BranchCount(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = tint)
        Spacer(Modifier.width(2.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
private fun BranchActionMenu(
    expanded: Boolean,
    branch: BranchModel,
    isRemote: Boolean,
    onDismiss: () -> Unit,
    onAction: (BranchAction) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = branch.name,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = branch.fullPath,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        if (!branch.isCurrent) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.branch_menu_checkout)) },
                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                onClick = { onDismiss(); onAction(BranchAction.CHECKOUT) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.branch_menu_merge)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.CallMerge, contentDescription = null) },
                onClick = { onDismiss(); onAction(BranchAction.MERGE) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.branch_menu_rebase)) },
                leadingIcon = { Icon(Icons.Default.SyncAlt, contentDescription = null) },
                onClick = { onDismiss(); onAction(BranchAction.REBASE) }
            )
        }

        if (!isRemote) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = { onDismiss(); onAction(BranchAction.RENAME) }
            )
        }

        if (!branch.isCurrent && !isRemote) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = { onDismiss(); onAction(BranchAction.DELETE) }
            )
        }
    }
}
