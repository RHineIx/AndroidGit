package com.android.git.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.data.GitManager
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.SnackbarType
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeConflictScreen(
    gitManager: GitManager,
    onBack: () -> Unit,
    onResolveFile: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var conflicts by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    var statusType by remember { mutableStateOf(SnackbarType.INFO) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun loadConflicts() {
        scope.launch {
            isLoading = true
            try {
                conflicts = gitManager.getConflictingFiles()
                statusMessage = ""
            } catch (e: Exception) {
                conflicts = emptyList()
                statusMessage = context.getString(R.string.conflict_load_error, e.message ?: "Unknown error")
                statusType = SnackbarType.ERROR
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(gitManager) { loadConflicts() }

    DisposableEffect(lifecycleOwner, gitManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) loadConflicts()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.conflict_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                        Text(stringResource(R.string.conflict_none), color = MaterialTheme.colorScheme.secondary)
                        
                        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                            Text(stringResource(R.string.action_back))
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
                                stringResource(R.string.conflict_list_header),
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
                    AppSnackbar(message = statusMessage, type = statusType) { statusMessage = "" }
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
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