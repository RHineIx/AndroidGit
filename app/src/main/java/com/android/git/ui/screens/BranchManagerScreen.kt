package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.R
import com.android.git.model.BranchModel
import com.android.git.model.BranchType
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.viewmodel.MainViewModel

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

    LaunchedEffect(Unit) {
        viewModel.loadBranches()
    }

    BackHandler(enabled = !isLoading) { onBack() }

    BranchDialogs(
        showCreateDialog = showCreateDialog,
        showRenameDialog = showRenameDialog,
        branchToRename = branchToRename,
        onDismissCreate = { showCreateDialog = false },
        onDismissRename = { showRenameDialog = false; branchToRename = null },
        onCreate = { name -> viewModel.createBranch(name) },
        onRename = { name -> viewModel.renameBranch(name) }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.branch_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.fetchAll() },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.dashboard_tool_fetch))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.branch_dialog_create_title))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            BranchListContent(
                branches = branches,
                searchQuery = searchQuery,
                selectedTab = selectedTab,
                isLoading = isLoading,
                onSearchChange = { searchQuery = it },
                onTabChange = { selectedTab = it },
                onAction = { action, branch ->
                    when (action) {
                        BranchAction.CHECKOUT -> viewModel.checkoutBranch(branch.name)
                        BranchAction.DELETE -> viewModel.deleteBranch(branch.name)
                        BranchAction.MERGE -> viewModel.mergeBranch(branch.fullPath)
                        BranchAction.REBASE -> viewModel.rebaseBranch(branch.fullPath)
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
private fun BranchDialogs(
    showCreateDialog: Boolean,
    showRenameDialog: Boolean,
    branchToRename: BranchModel?,
    onDismissCreate: () -> Unit,
    onDismissRename: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String) -> Unit
) {
    if (showCreateDialog) {
        InputBranchDialog(
            title = stringResource(R.string.branch_dialog_create_title),
            confirmText = stringResource(R.string.action_create),
            onDismiss = onDismissCreate,
            onConfirm = onCreate
        )
    }

    if (showRenameDialog && branchToRename != null) {
        InputBranchDialog(
            title = stringResource(R.string.branch_dialog_rename_title),
            textPrefix = stringResource(R.string.branch_dialog_rename_prefix, branchToRename.name),
            confirmText = stringResource(R.string.action_rename),
            onDismiss = onDismissRename,
            onConfirm = onRename
        )
    }
}

@Composable
private fun InputBranchDialog(
    title: String,
    textPrefix: String? = null,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (textPrefix != null) {
                    Text(textPrefix)
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(stringResource(R.string.branch_dialog_create_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (textValue.isNotEmpty()) {
                    onConfirm(textValue)
                    onDismiss()
                    textValue = ""
                }
            }) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BranchListContent(
    branches: List<BranchModel>,
    searchQuery: String,
    selectedTab: Int,
    isLoading: Boolean,
    onSearchChange: (String) -> Unit,
    onTabChange: (Int) -> Unit,
    onAction: (BranchAction, BranchModel) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text(stringResource(R.string.branch_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabChange(0) },
                text = { Text(stringResource(R.string.branch_tab_local)) },
                icon = { Icon(Icons.Default.Computer, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabChange(1) },
                text = { Text(stringResource(R.string.branch_tab_remote)) },
                icon = { Icon(Icons.Default.Cloud, contentDescription = null) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val targetType = if (selectedTab == 0) BranchType.LOCAL else BranchType.REMOTE
                val filteredBranches = branches.filter {
                    it.type == targetType && it.name.contains(searchQuery, ignoreCase = true)
                }

                if (filteredBranches.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.branch_empty_search), color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredBranches) { branch ->
                            BranchItemRich(
                                branch = branch,
                                onAction = { action -> onAction(action, branch) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

enum class BranchAction { CHECKOUT, MERGE, REBASE, RENAME, DELETE }

@Composable
fun BranchItemRich(
    branch: BranchModel,
    onAction: (BranchAction) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isRemote = branch.type == BranchType.REMOTE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMenu = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isRemote) Icons.Default.Cloud else Icons.Default.Computer,
            contentDescription = null,
            tint = if (branch.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = branch.name,
                fontWeight = if (branch.isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (branch.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )
            if (branch.isCurrent) {
                Text(
                    text = stringResource(R.string.branch_current_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        BranchActionMenu(
            expanded = showMenu,
            branch = branch,
            isRemote = isRemote,
            onDismiss = { showMenu = false },
            onAction = onAction
        )
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
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(branch.name, fontWeight = FontWeight.Bold) },
            onClick = { },
            enabled = false
        )
        HorizontalDivider()

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

        if (!isRemote && branch.isCurrent) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = { onDismiss(); onAction(BranchAction.RENAME) }
            )
        }

        if (!branch.isCurrent && !isRemote) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = { onDismiss(); onAction(BranchAction.DELETE) }
            )
        }
    }
}