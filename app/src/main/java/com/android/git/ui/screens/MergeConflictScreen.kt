package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.git.data.GitManager
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.SnackbarType
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeConflictScreen(
    repoFile: File,
    onBack: () -> Unit,
    onResolveFile: (String) -> Unit // Navigate to detail resolver
) {
    val gitManager = remember { GitManager(repoFile) }
    val scope = rememberCoroutineScope()

    var conflicts by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    
    BackHandler(enabled = true) { onBack() }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            conflicts = gitManager.getConflictingFiles()
            isLoading = false
            if (conflicts.isEmpty()) {
                statusMessage = "No conflicts found. You are safe!"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merge Conflicts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (conflicts.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text("No conflicts detected.", color = MaterialTheme.colorScheme.secondary)
                        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Go Back")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "The following files have merge conflicts. Tap a file to resolve.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        items(conflicts) { filePath ->
                            ConflictItem(filePath = filePath, onClick = { onResolveFile(filePath) })
                        }
                    }
                }
            }
            
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                if (statusMessage.isNotEmpty()) {
                    AppSnackbar(message = statusMessage, type = SnackbarType.SUCCESS) { statusMessage = "" }
                }
            }
        }
    }
}

@Composable
fun ConflictItem(filePath: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(16.dp))
            Text(filePath, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}