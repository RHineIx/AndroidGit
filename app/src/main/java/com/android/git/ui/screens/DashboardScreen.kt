package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.data.GitManager
import com.android.git.data.PreferencesManager
import com.android.git.model.DashboardState
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.SnackbarType
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DashboardScreen(
    repoFile: File,
    gitManager: GitManager,
    dashboardState: DashboardState,
    onRefresh: () -> Unit,
    onViewChanges: () -> Unit,
    onSettings: () -> Unit,
    onViewLog: () -> Unit,
    onManageBranches: () -> Unit,
    onOpenStash: () -> Unit,
    onIgnoreEditor: () -> Unit,
    onCloseProject: () -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    
    var statusMessage by remember { mutableStateOf("") }
    var statusType by remember { mutableStateOf(SnackbarType.INFO) }
    var isLoading by remember { mutableStateOf(false) }
    
    var showExitDialog by remember { mutableStateOf(false) }
    var showForcePushDialog by remember { mutableStateOf(false) }
    
    // Menu State
    var showMenu by remember { mutableStateOf(false) }
    
    var isForcePushChecked by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) { showExitDialog = true }

    // --- Dialogs ---
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Close Project") },
            text = { Text("Return to repository selection?") },
            confirmButton = { TextButton(onClick = { showExitDialog = false; onCloseProject() }) { Text("Close", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Cancel") } }
        )
    }

    if (showForcePushDialog) {
        AlertDialog(
            onDismissRequest = { showForcePushDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Force Push Warning") },
            text = { Text("This will overwrite remote history. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = { 
                        showForcePushDialog = false
                        scope.launch {
                            val token = prefsManager.getToken()
                            if (token.isEmpty()) {
                                statusMessage = "Go to Settings to set Token"
                                statusType = SnackbarType.ERROR
                            } else {
                                isLoading = true
                                statusMessage = "Force Pushing..."
                                statusMessage = gitManager.push(token, force = true)
                                isLoading = false
                                onRefresh()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Yes, Force Push") }
            },
            dismissButton = { TextButton(onClick = { showForcePushDialog = false }) { Text("Cancel") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(repoFile.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(repoFile.absolutePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
                    
                    // --- Standard Material 3 Menu (Fixed) ---
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More Options")
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = DpOffset(0.dp, 8.dp),
                            // Fix: Apply styling via Modifier instead of direct params
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            // Optional Header
                            Text(
                                text = "Tools",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Stash Shelf") },
                                leadingIcon = { Icon(Icons.Default.Archive, null) },
                                onClick = { showMenu = false; onOpenStash() }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Edit .gitignore") },
                                leadingIcon = { Icon(Icons.Default.Code, null) },
                                onClick = { showMenu = false; onIgnoreEditor() }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            if (isLoading) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(16.dp)) }

            when (dashboardState) {
                is DashboardState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is DashboardState.NotInitialized -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Not a Git repo.")
                            Button(onClick = { scope.launch { statusMessage = gitManager.initRepo(); onRefresh() } }) { Text("Init Git") }
                        }
                    }
                }
                is DashboardState.Success -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Quick Actions", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val token = prefsManager.getToken()
                                                if (token.isEmpty()) { statusMessage = "Set Token!"; statusType = SnackbarType.ERROR }
                                                else { isLoading = true; statusMessage = gitManager.pull(token); isLoading = false; onRefresh() }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isLoading
                                    ) { Icon(Icons.Default.CloudDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Pull") }
                                    
                                    Button(
                                        onClick = {
                                            if (isForcePushChecked) showForcePushDialog = true
                                            else scope.launch {
                                                val token = prefsManager.getToken()
                                                if (token.isEmpty()) { statusMessage = "Set Token!"; statusType = SnackbarType.ERROR }
                                                else { isLoading = true; statusMessage = gitManager.push(token); isLoading = false; onRefresh() }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = if(isForcePushChecked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                                        enabled = !isLoading
                                    ) { Icon(Icons.Default.CloudUpload, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if(isForcePushChecked) "Force" else "Push") }
                                }
                                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isForcePushChecked, onCheckedChange = { isForcePushChecked = it }, modifier = Modifier.scale(0.8f))
                                        Text("Allow Force", fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { scope.launch { val t = prefsManager.getToken(); if(t.isEmpty()) { statusMessage = "Set Token!"; statusType = SnackbarType.ERROR } else { isLoading=true; statusMessage=gitManager.linkAndRepair(t); isLoading=false; onRefresh() } } }) { Icon(Icons.Default.Build, null, Modifier.size(16.dp)); Text(" Repair Link", fontSize = 12.sp) }
                                }
                            }
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(modifier = Modifier.weight(1f).clickable { onManageBranches() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1F6FEB).copy(alpha = 0.2f))) {
                                Column(Modifier.padding(16.dp)) { Icon(Icons.Default.CallSplit, null, tint = Color(0xFF1F6FEB)); Text("Branch", fontSize = 12.sp); Text(dashboardState.branch, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1) }
                            }
                            Card(modifier = Modifier.weight(1f).clickable { onViewChanges() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF238636).copy(alpha = 0.2f))) {
                                Column(Modifier.padding(16.dp)) { Icon(Icons.Default.Refresh, null, tint = Color(0xFF238636)); Text("Changes", fontSize = 12.sp); Text("${dashboardState.changes} Files", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                            }
                        }
                        
                        Card(modifier = Modifier.fillMaxWidth().clickable { onViewLog() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF8957E5).copy(alpha = 0.2f))) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.History, null, tint = Color(0xFF8957E5)); Spacer(Modifier.width(12.dp)); Column { Text("History", fontWeight = FontWeight.Bold); Text("View commits", fontSize = 12.sp) } }
                        }
                        
                        if (dashboardState.unpushedCount > 0) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Upload, null, tint = MaterialTheme.colorScheme.onSecondaryContainer); Spacer(Modifier.width(12.dp)); Text("${dashboardState.unpushedCount} Unpushed Commits", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer) }
                            }
                        }
                    }
                }
                is DashboardState.Error -> Text("Error: ${dashboardState.message}", color = MaterialTheme.colorScheme.error)
            }
        }
        Box(Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) { AppSnackbar(statusMessage, statusType) { statusMessage = "" } }
    }
}
fun Modifier.scale(scale: Float): Modifier = this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))