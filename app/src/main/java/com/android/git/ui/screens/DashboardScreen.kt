package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.android.git.data.GitManager
import com.android.git.data.PreferencesManager
import com.android.git.model.DashboardState
import com.android.git.ui.components.AppSnackbar
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DashboardScreen(
    repoFile: File,
    onViewChanges: () -> Unit,
    onSync: () -> Unit,
    onViewLog: () -> Unit,
    onIgnoreEditor: () -> Unit, // New Callback
    onCloseProject: () -> Unit
) {
    val context = LocalContext.current
    val gitManager = remember { GitManager(repoFile) }
    val prefsManager = remember { PreferencesManager(context) }
    
    var uiState by remember { mutableStateOf<DashboardState>(DashboardState.Loading) }
    var statusMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // Branch Dialog
    var showBranchDialog by remember { mutableStateOf(false) }
    var branches by remember { mutableStateOf<List<String>>(emptyList()) }
    var newBranchName by remember { mutableStateOf("") }

    // Dialogs
    var showExitDialog by remember { mutableStateOf(false) }
    var showForcePushDialog by remember { mutableStateOf(false) }
    
    // Menu
    var showMenu by remember { mutableStateOf(false) }

    // States
    var isForcePushChecked by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Helper: Refresh Data
    fun refreshDashboard() {
        scope.launch {
            if (gitManager.isGitRepo()) {
                gitManager.openRepo()
                uiState = gitManager.getDashboardStats()
            } else {
                uiState = DashboardState.NotInitialized
            }
        }
    }

    // Lifecycle Observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshDashboard()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(repoFile) {
        refreshDashboard()
    }

    BackHandler(enabled = true) {
        showExitDialog = true
    }

    // --- Dialogs ---

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Close Project") },
            text = { Text("Return to repository selection?") },
            confirmButton = {
                TextButton(onClick = { 
                    showExitDialog = false
                    onCloseProject() 
                }) { Text("Close", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showForcePushDialog) {
        AlertDialog(
            onDismissRequest = { showForcePushDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Force Push Warning") },
            text = { 
                Text("Force pushing will overwrite the remote history with your local history. This is destructive and cannot be undone.\n\nAre you sure?") 
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showForcePushDialog = false
                        scope.launch {
                            val token = prefsManager.getToken()
                            if (token.isEmpty()) {
                                statusMessage = "Please configure token in Settings (Sync)"
                            } else {
                                isLoading = true
                                statusMessage = "Force Pushing..."
                                statusMessage = gitManager.push(token, force = true)
                                isLoading = false
                                refreshDashboard()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Yes, Force Push") }
            },
            dismissButton = {
                TextButton(onClick = { showForcePushDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBranchDialog) {
        AlertDialog(
            onDismissRequest = { showBranchDialog = false },
            title = { Text("Switch Branch") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newBranchName,
                        onValueChange = { newBranchName = it },
                        label = { Text("New Branch Name") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    if (newBranchName.isNotEmpty()) {
                                        statusMessage = gitManager.createBranch(newBranchName)
                                        gitManager.checkoutBranch(newBranchName)
                                        refreshDashboard()
                                        showBranchDialog = false
                                        newBranchName = ""
                                    }
                                }
                            }) { Icon(Icons.Default.Add, "Create") }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(branches) { branch ->
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        statusMessage = gitManager.checkoutBranch(branch)
                                        refreshDashboard()
                                        showBranchDialog = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = branch, 
                                    fontWeight = if (uiState is DashboardState.Success && (uiState as DashboardState.Success).branch == branch) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBranchDialog = false }) { Text("Close") }
            }
        )
    }

    // --- Main Layout ---
    Box(modifier = Modifier.fillMaxSize()) {
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = repoFile.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = repoFile.absolutePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    // Settings/Sync Icon
                    IconButton(
                        onClick = onSync,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    
                    // More Menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit .gitignore") },
                                onClick = {
                                    showMenu = false
                                    onIgnoreEditor()
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }

            when (val state = uiState) {
                is DashboardState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                
                is DashboardState.NotInitialized -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("This folder is not a Git repository yet.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                scope.launch {
                                    statusMessage = gitManager.initRepo()
                                    refreshDashboard()
                                }
                            }) { Text("Initialize Git Here") }
                        }
                    }
                }

                is DashboardState.Success -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        
                        // Quick Actions Row (Push/Pull)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // PULL
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val token = prefsManager.getToken()
                                                if (token.isEmpty()) {
                                                    statusMessage = "Please configure token in Settings"
                                                } else {
                                                    isLoading = true
                                                    statusMessage = "Pulling..."
                                                    statusMessage = gitManager.pull(token)
                                                    isLoading = false
                                                    refreshDashboard()
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        enabled = !isLoading
                                    ) {
                                        Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Pull")
                                    }

                                    // PUSH
                                    Button(
                                        onClick = {
                                            if (isForcePushChecked) {
                                                showForcePushDialog = true
                                            } else {
                                                scope.launch {
                                                    val token = prefsManager.getToken()
                                                    if (token.isEmpty()) {
                                                        statusMessage = "Please configure token in Settings"
                                                    } else {
                                                        isLoading = true
                                                        statusMessage = "Pushing..."
                                                        statusMessage = gitManager.push(token, force = false)
                                                        isLoading = false
                                                        refreshDashboard()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if(isForcePushChecked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        ),
                                        enabled = !isLoading
                                    ) {
                                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(if(isForcePushChecked) "Force Push" else "Push")
                                    }
                                }

                                // Extra Options Row
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Force Push Checkbox
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isForcePushChecked,
                                            onCheckedChange = { isForcePushChecked = it },
                                            modifier = Modifier.scale(0.8f)
                                        )
                                        Text("Allow Force Push", fontSize = 12.sp)
                                    }

                                    // Force Fetch (Repair) Button
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                val token = prefsManager.getToken()
                                                if (token.isEmpty()) {
                                                    statusMessage = "Please configure token in Settings"
                                                } else {
                                                    isLoading = true
                                                    statusMessage = "Force Fetching..."
                                                    statusMessage = gitManager.linkAndRepair(token)
                                                    isLoading = false
                                                    refreshDashboard()
                                                }
                                            }
                                        },
                                        enabled = !isLoading
                                    ) {
                                        Icon(Icons.Default.Build, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Repair Link", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // --- Cards Grid ---
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                modifier = Modifier.weight(1f).clickable { 
                                    scope.launch {
                                        branches = gitManager.getBranches()
                                        showBranchDialog = true
                                    }
                                },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F6FEB).copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.CallSplit, null, tint = Color(0xFF1F6FEB))
                                    Text("Branch", fontSize = 12.sp)
                                    Text(state.branch, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f).clickable { onViewChanges() },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF238636).copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.Refresh, null, tint = Color(0xFF238636))
                                    Text("Changes", fontSize = 12.sp)
                                    Text("${state.changes} Files", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onViewLog() },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF8957E5).copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, null, tint = Color(0xFF8957E5))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Commit History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("View past changes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        
                        if (state.unpushedCount > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Upload, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("${state.unpushedCount} Unpushed Commits", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Text("Ready to Push", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }

                is DashboardState.Error -> {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        // Floating Status Snackbar
        Box(
            modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AppSnackbar(
                message = statusMessage,
                onDismiss = { statusMessage = "" }
            )
        }
    }
}

fun Modifier.scale(scale: Float): Modifier = this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))