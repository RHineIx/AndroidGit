package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.git.model.ChangeType
import com.android.git.model.GitFile
import com.android.git.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onViewDiff: (String) -> Unit // NEW Callback
) {
    // Observing ViewModel State
    val files = viewModel.changedFiles
    val isLoading = viewModel.isLoading
    val statusMessage = viewModel.statusMessage

    // Local UI State
    var selectedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var commitMessage by remember { mutableStateOf("") }
    var isAmend by remember { mutableStateOf(false) }

    // Selection Logic Helpers
    var showFilterMenu by remember { mutableStateOf(false) }
    var showExtensionDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = !isLoading) { onBack() }

    // Initial Load & Auto-Select
    LaunchedEffect(Unit) {
        viewModel.loadChangedFiles()
    }
    
    // Auto-select all files when loaded first time
    LaunchedEffect(files) {
        if (selectedFiles.isEmpty() && files.isNotEmpty()) {
            selectedFiles = files.map { it.path }.toSet()
        }
    }

    // Amend Logic: Load last message
    LaunchedEffect(isAmend) {
        if (isAmend) {
            viewModel.getLastCommitMessage { msg ->
                if (msg.isNotEmpty()) commitMessage = msg
            }
        }
    }

    // Filter Dialog
    if (showExtensionDialog) {
        val extensions = files.map { it.path.substringAfterLast('.', "") }.filter { it.isNotEmpty() }.distinct()
        
        AlertDialog(
            onDismissRequest = { showExtensionDialog = false },
            title = { Text("Select by Extension") },
            text = {
                Column {
                    extensions.forEach { ext ->
                        TextButton(
                            onClick = {
                                val matching = files.filter { it.path.endsWith(".$ext") }.map { it.path }
                                selectedFiles = selectedFiles + matching
                                showExtensionDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(".$ext")
                        }
                    }
                    if (extensions.isEmpty()) Text("No extensions found.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showExtensionDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Changes")
                        Text(
                            "${selectedFiles.size} of ${files.size} selected", 
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Select All") },
                                onClick = { 
                                    selectedFiles = files.map { it.path }.toSet()
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Deselect All") },
                                onClick = { 
                                    selectedFiles = emptySet()
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Invert Selection") },
                                onClick = { 
                                    val allPaths = files.map { it.path }.toSet()
                                    selectedFiles = allPaths - selectedFiles
                                    showFilterMenu = false
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Select by Extension...") },
                                onClick = { 
                                    showFilterMenu = false
                                    showExtensionDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp)
                    .animateContentSize()
            ) {
                // Amend Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Checkbox(checked = isAmend, onCheckedChange = { isAmend = it }, enabled = !isLoading)
                    Text("Amend last commit")
                }

                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = { Text(if(isAmend) "Amend Message" else "Commit Message") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.commitChanges(commitMessage, isAmend, selectedFiles)
                        commitMessage = ""
                        isAmend = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = (selectedFiles.isNotEmpty() || isAmend) && commitMessage.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Committing...")
                    } else {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAmend) "Amend Commit" else "Commit Changes")
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading && files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    val isSelected = selectedFiles.contains(file.path)
                    
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(),
                    ) {
                        FileChangeItem(
                            file = file,
                            isSelected = isSelected,
                            onToggle = {
                                selectedFiles = if (isSelected) selectedFiles - file.path else selectedFiles + file.path
                            },
                            // NEW: Pass click to view diff
                            onViewDiff = { onViewDiff(file.path) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
            
            if (files.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No changes detected", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (statusMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(statusMessage, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileChangeItem(
    file: GitFile,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onViewDiff: () -> Unit // NEW Callback
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
                           else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() } // Main click selects/deselects
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            
            StatusIcon(file.type)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = getFriendlyStatusName(file.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = getStatusColor(file.type)
                )
            }

            // NEW: View Diff Button
            IconButton(onClick = onViewDiff) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "View Diff",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun getFriendlyStatusName(type: ChangeType): String {
    return when (type) {
        ChangeType.ADDED -> "ADDED"
        ChangeType.MODIFIED -> "MODIFIED"
        ChangeType.DELETED -> "DELETED"
        ChangeType.UNTRACKED -> "NEW FILE"
        ChangeType.MISSING -> "MISSING"
    }
}

@Composable
fun StatusIcon(type: ChangeType) {
    val icon: ImageVector = when (type) {
        ChangeType.ADDED -> Icons.Default.AddCircleOutline
        ChangeType.MODIFIED -> Icons.Default.Create
        ChangeType.DELETED -> Icons.Default.Delete
        ChangeType.UNTRACKED -> Icons.Default.Add
        ChangeType.MISSING -> Icons.Default.Delete
    }

    val color = getStatusColor(type)

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}

fun getStatusColor(type: ChangeType): Color {
    return when (type) {
        ChangeType.ADDED -> Color(0xFF238636) 
        ChangeType.MODIFIED -> Color(0xFFE3B341) 
        ChangeType.DELETED -> Color(0xFFDA3633) 
        ChangeType.UNTRACKED -> Color(0xFF238636)
        ChangeType.MISSING -> Color(0xFFDA3633)
    }
}