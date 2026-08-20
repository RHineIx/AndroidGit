package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.android.git.R
import com.android.git.model.ChangeType
import com.android.git.model.GitFile
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.basic.Checkbox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val files = viewModel.changedFiles
    val isLoading = viewModel.isLoading
    val statusMessage = viewModel.statusMessage
    val statusType = viewModel.statusType // Added to support local snackbar
    val isAIGenerating = viewModel.isAIGenerating

    var selectedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var commitMessage by remember { mutableStateOf("") }
    var isAmend by remember { mutableStateOf(false) }

    var showFilterMenu by remember { mutableStateOf(false) }
    var showExtensionDialog by remember { mutableStateOf(false) }

    var isMessageExpanded by remember { mutableStateOf(false) }

    val commonShape = RoundedCornerShape(16.dp)

    BackHandler(enabled = isLoading || isMessageExpanded || isAIGenerating) {
        if (isMessageExpanded) {
            isMessageExpanded = false
        }
    }

    LaunchedEffect(Unit) { viewModel.loadChangedFiles() }

    LaunchedEffect(files) {
        val currentPaths = files.map { it.path }.toSet()
        selectedFiles = if (selectedFiles.isEmpty() && files.isNotEmpty()) {
            currentPaths
        } else {
            selectedFiles intersect currentPaths
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
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.changes_filter_extension), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (extensions.isEmpty()) {
                        Text(stringResource(R.string.changes_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
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
                }
            },
            confirmButton = {
                TextButton(onClick = { showExtensionDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.changes_title), fontWeight = FontWeight.Bold)
                            Text(
                                text = stringResource(R.string.changes_selected_count, selectedFiles.size, files.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, enabled = !isLoading && !isAIGenerating) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false },
                                shape = commonShape
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.changes_filter_select_all)) },
                                    onClick = { selectedFiles = files.map { it.path }.toSet(); showFilterMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.changes_filter_deselect_all)) },
                                    onClick = { selectedFiles = emptySet(); showFilterMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.changes_filter_invert)) },
                                    onClick = { selectedFiles = files.map { it.path }.toSet() - selectedFiles; showFilterMenu = false }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.changes_filter_extension)) },
                                    onClick = { showFilterMenu = false; showExtensionDialog = true }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .animateContentSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Checkbox(
                                    checked = isAmend,
                                    onCheckedChange = { isAmend = it }, 
                                    enabled = !isLoading && !isAIGenerating
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.changes_amend_checkbox),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        OutlinedTextField(
                            value = commitMessage,
                            onValueChange = { commitMessage = it },
                            label = { Text(if(isAmend) stringResource(R.string.changes_amend_msg_label) else stringResource(R.string.changes_commit_msg_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            shape = commonShape,
                            enabled = !isLoading && !isAIGenerating,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isAIGenerating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp).padding(end = 4.dp), 
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        IconButton(
                                            onClick = { 
                                                viewModel.generateAICommitMessage(selectedFiles) { message -> 
                                                    commitMessage = message 
                                                }
                                            },
                                            enabled = selectedFiles.isNotEmpty()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome, 
                                                contentDescription = stringResource(R.string.changes_ai_generate),
                                                tint = if (selectedFiles.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                    IconButton(onClick = { isMessageExpanded = true }) {
                                        Icon(Icons.Default.Fullscreen, contentDescription = stringResource(R.string.changes_expand_message))
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.commitChanges(
                                    message = commitMessage,
                                    isAmend = isAmend,
                                    selectedPaths = selectedFiles,
                                    onSuccess = {
                                        commitMessage = ""
                                        isAmend = false
                                        selectedFiles = emptySet()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = commonShape,
                            enabled = (selectedFiles.isNotEmpty() || isAmend) && commitMessage.isNotBlank() && !isLoading && !isAIGenerating
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(R.string.changes_status_committing), style = MaterialTheme.typography.titleMedium)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isAmend) stringResource(R.string.changes_btn_amend) else stringResource(R.string.changes_btn_commit),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(files, key = { it.path }) { file ->
                        val isSelected = selectedFiles.contains(file.path)
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            FileChangeItem(
                                file = file,
                                isSelected = isSelected,
                                onToggle = {
                                    selectedFiles = if (isSelected) selectedFiles - file.path else selectedFiles + file.path
                                }
                            )
                        }
                    }
                }
                if (files.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.changes_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isMessageExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Text(
                                text = if(isAmend) stringResource(R.string.changes_amend_msg_label) else stringResource(R.string.changes_commit_msg_label),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { isMessageExpanded = false }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                            }
                        },
                        actions = {
                            if (isAIGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp).padding(end = 16.dp), 
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                IconButton(
                                    onClick = { 
                                        viewModel.generateAICommitMessage(selectedFiles) { message -> 
                                            commitMessage = message 
                                        }
                                    },
                                    enabled = selectedFiles.isNotEmpty()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome, 
                                        contentDescription = stringResource(R.string.changes_ai_generate),
                                        tint = if (selectedFiles.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            TextButton(onClick = { isMessageExpanded = false }) {
                                Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )

                    TextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        placeholder = { Text(stringResource(R.string.changes_commit_msg_label)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }

        // AppSnackbar is now correctly placed in the screen's main Box
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .zIndex(10f)
        ) {
            AppSnackbar(
                message = statusMessage,
                type = statusType,
                onDismiss = { viewModel.clearStatus() }
            )
        }
    }
}

@Composable
fun FileChangeItem(file: GitFile, isSelected: Boolean, onToggle: () -> Unit) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { _ -> onToggle() }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusIcon(file.type)
            Spacer(modifier = Modifier.width(16.dp))
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
                    color = getStatusColor(file.type),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
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

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun getStatusColor(type: ChangeType): Color {
    return when (type) {
        ChangeType.ADDED, ChangeType.UNTRACKED -> Color(0xFF238636)
        ChangeType.MODIFIED -> Color(0xFFE3B341)
        ChangeType.DELETED, ChangeType.MISSING -> Color(0xFFDA3633)
    }
}