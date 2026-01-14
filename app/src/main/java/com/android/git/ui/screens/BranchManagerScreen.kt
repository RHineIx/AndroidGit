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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.model.BranchModel
import com.android.git.model.BranchType
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BranchManagerScreen(
    viewModel: MainViewModel, // Injected ViewModel
    onBack: () -> Unit
) {
    // Observing ViewModel State
    val branches = viewModel.branchList
    val isLoading = viewModel.isLoading
    val statusMessage = viewModel.statusMessage
    val statusType = viewModel.statusType

    // Local UI State
    var selectedTab by remember { mutableStateOf(0) } // 0 = Local, 1 = Remote
    var searchQuery by remember { mutableStateOf("") }
    
    // Dialogs
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newBranchName by remember { mutableStateOf("") }
    var branchToRename by remember { mutableStateOf<BranchModel?>(null) }

    // Initial Load
    LaunchedEffect(Unit) {
        viewModel.loadBranches()
    }

    BackHandler(enabled = !isLoading) { onBack() }

    // --- Dialogs ---
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Branch") },
            text = {
                OutlinedTextField(
                    value = newBranchName,
                    onValueChange = { newBranchName = it },
                    label = { Text("Branch Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newBranchName.isNotEmpty()) {
                        viewModel.createBranch(newBranchName)
                        showCreateDialog = false
                        newBranchName = ""
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }

    if (showRenameDialog && branchToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Branch") },
            text = {
                Column {
                    Text("Renaming: ${branchToRename!!.name}")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newBranchName,
                        onValueChange = { newBranchName = it },
                        label = { Text("New Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newBranchName.isNotEmpty()) {
                        viewModel.renameBranch(newBranchName)
                        showRenameDialog = false
                        branchToRename = null
                        newBranchName = ""
                    }
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Branches") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.fetchAll() },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.CloudDownload, "Fetch All")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "New Branch")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // 1. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search branches...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // 2. Tabs (Local / Remote)
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Local") },
                    icon = { Icon(Icons.Default.Computer, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Remote") },
                    icon = { Icon(Icons.Default.Cloud, null) }
                )
            }

            // 3. List
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    val targetType = if (selectedTab == 0) BranchType.LOCAL else BranchType.REMOTE
                    val filteredBranches = branches.filter { 
                        it.type == targetType && it.name.contains(searchQuery, ignoreCase = true) 
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (filteredBranches.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No branches found", color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                        
                        items(filteredBranches) { branch ->
                            BranchItemRich(
                                branch = branch,
                                onAction = { action ->
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
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
                
                // Snackbar Area
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    AppSnackbar(message = statusMessage, type = statusType, onDismiss = { viewModel.clearStatus() })
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
                    text = "Current",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, "Actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // --- Context Menu ---
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(branch.name, fontWeight = FontWeight.Bold) },
                onClick = { },
                enabled = false
            )
            HorizontalDivider()

            if (!branch.isCurrent) {
                DropdownMenuItem(
                    text = { Text("Checkout") },
                    leadingIcon = { Icon(Icons.Default.Check, null) },
                    onClick = { showMenu = false; onAction(BranchAction.CHECKOUT) }
                )
            }

            if (!branch.isCurrent) {
                DropdownMenuItem(
                    text = { Text("Merge into Current") },
                    leadingIcon = { Icon(Icons.Default.CallMerge, null) },
                    onClick = { showMenu = false; onAction(BranchAction.MERGE) }
                )
            }

            if (!branch.isCurrent) {
                DropdownMenuItem(
                    text = { Text("Rebase Current onto this") },
                    leadingIcon = { Icon(Icons.Default.SyncAlt, null) },
                    onClick = { showMenu = false; onAction(BranchAction.REBASE) }
                )
            }

            if (!isRemote && branch.isCurrent) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    onClick = { showMenu = false; onAction(BranchAction.RENAME) }
                )
            }

            if (!branch.isCurrent && !isRemote) { 
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onAction(BranchAction.DELETE) }
                )
            }
        }
    }
}