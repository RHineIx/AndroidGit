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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.model.ChangeType
import com.android.git.model.GitFile
import com.android.git.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onViewDiff: (String) -> Unit
) {
    val files = viewModel.changedFiles
    val isLoading = viewModel.isLoading
    val statusMessage = viewModel.statusMessage

    var selectedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var commitMessage by remember { mutableStateOf("") }
    var isAmend by remember { mutableStateOf(false) }

    var showFilterMenu by remember { mutableStateOf(false) }
    var showExtensionDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = !isLoading) { onBack() }

    LaunchedEffect(Unit) { viewModel.loadChangedFiles() }
    
    LaunchedEffect(files) {
        if (selectedFiles.isEmpty() && files.isNotEmpty()) {
            selectedFiles = files.map { it.path }.toSet()
        }
    }

    LaunchedEffect(isAmend) {
        if (isAmend) {
            viewModel.getLastCommitMessage { msg -> if (msg.isNotEmpty()) commitMessage = msg }
        }
    }

    if (showExtensionDialog) {
        val extensions = files.map { it.path.substringAfterLast('.', "") }.filter { it.isNotEmpty() }.distinct()
        AlertDialog(
            onDismissRequest = { showExtensionDialog = false },
            title = { Text(stringResource(R.string.changes_filter_extension)) },
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
                        ) { Text(".$ext") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showExtensionDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stringResource(R.string.changes_title))
                        Text(stringResource(R.string.changes_selected_count, selectedFiles.size, files.size), style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) { Icon(Icons.Default.FilterList, "Filter") }
                        DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.changes_filter_select_all)) }, onClick = { selectedFiles = files.map { it.path }.toSet(); showFilterMenu = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.changes_filter_deselect_all)) }, onClick = { selectedFiles = emptySet(); showFilterMenu = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.changes_filter_invert)) }, onClick = { selectedFiles = files.map { it.path }.toSet() - selectedFiles; showFilterMenu = false })
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text(stringResource(R.string.changes_filter_extension)) }, onClick = { showFilterMenu = false; showExtensionDialog = true })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp).animateContentSize()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Checkbox(checked = isAmend, onCheckedChange = { isAmend = it }, enabled = !isLoading)
                    Text(stringResource(R.string.changes_amend_checkbox))
                }

                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = { Text(if(isAmend) stringResource(R.string.changes_amend_msg_label) else stringResource(R.string.changes_commit_msg_label)) },
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
                        Text(stringResource(R.string.changes_status_committing))
                    } else {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAmend) stringResource(R.string.changes_btn_amend) else stringResource(R.string.changes_btn_commit))
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading && files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    val isSelected = selectedFiles.contains(file.path)
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                        FileChangeItem(file = file, isSelected = isSelected, onToggle = { selectedFiles = if (isSelected) selectedFiles - file.path else selectedFiles + file.path }, onViewDiff = { onViewDiff(file.path) })
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
            if (files.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.changes_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun FileChangeItem(file: GitFile, isSelected: Boolean, onToggle: () -> Unit, onViewDiff: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }.animateContentSize()
    ) {
        Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
            StatusIcon(file.type)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = file.path, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = getFriendlyStatusName(file.type), style = MaterialTheme.typography.labelSmall, color = getStatusColor(file.type))
            }
            IconButton(onClick = onViewDiff) { Icon(imageVector = Icons.Default.Visibility, contentDescription = "View Diff", tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun getFriendlyStatusName(type: ChangeType): String {
    return when (type) {
        ChangeType.ADDED -> stringResource(R.string.changes_status_added)
        ChangeType.MODIFIED -> stringResource(R.string.changes_status_modified)
        ChangeType.DELETED -> stringResource(R.string.changes_status_deleted)
        ChangeType.UNTRACKED -> stringResource(R.string.changes_status_new)
        ChangeType.MISSING -> stringResource(R.string.changes_status_missing)
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
    Surface(color = color.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small, modifier = Modifier.size(32.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(16.dp)) }
    }
}

fun getStatusColor(type: ChangeType): Color {
    return when (type) {
        ChangeType.ADDED, ChangeType.UNTRACKED -> Color(0xFF238636)
        ChangeType.MODIFIED -> Color(0xFFE3B341)
        ChangeType.DELETED, ChangeType.MISSING -> Color(0xFFDA3633)
    }
}