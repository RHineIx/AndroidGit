package com.android.git.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.git.R
import com.android.git.data.GitManager
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.Button
import com.android.git.ui.components.Card
import com.android.git.ui.components.CircularProgressIndicator
import com.android.git.ui.components.GitIcons as Icons
import com.android.git.ui.components.OplusScaffold
import com.android.git.ui.components.SnackbarType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeConflictScreen(
    gitManager: GitManager,
    onBack: () -> Unit,
    onResolveFile: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var conflicts by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    var statusType by remember { mutableStateOf(SnackbarType.INFO) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Fetch the string resource outside of the Coroutine to satisfy Lint requirements
    val errorMsgTemplate = stringResource(R.string.conflict_load_error)

    fun loadConflicts() {
        scope.launch {
            isLoading = true
            try {
                conflicts = gitManager.getConflictingFiles()
                statusMessage = ""
            } catch (e: Exception) {
                conflicts = emptyList()
                statusMessage = errorMsgTemplate.format(e.message ?: "Unknown error")
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

    OplusScaffold(
        title = stringResource(R.string.conflict_title),
        onBack = onBack,

    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (conflicts.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.conflict_none), color = MaterialTheme.colorScheme.secondary)

                        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                            Text(stringResource(R.string.action_back))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp, end = 12.dp,
                            top = padding.calculateTopPadding() + 8.dp,
                            bottom = padding.calculateBottomPadding() + 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(filePath, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
