package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.android.git.data.GitManager
import com.android.git.data.PreferencesManager
import com.android.git.model.CommitItem
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    repoFile: File,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gitManager = remember { GitManager(repoFile) }
    val prefsManager = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    
    var hasRemote by remember { mutableStateOf(false) }
    var remoteUrl by remember { mutableStateOf("") }
    
    var token by remember { mutableStateOf(prefsManager.getToken()) }
    var saveToken by remember { mutableStateOf(token.isNotEmpty()) }
    
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    
    var unpushedCommits by remember { mutableStateOf<List<CommitItem>>(emptyList()) }

    BackHandler(enabled = true) {
        onBack()
    }

    fun loadData() {
        scope.launch {
            isLoading = true
            hasRemote = gitManager.hasRemote()
            if (hasRemote) {
                unpushedCommits = gitManager.getUnpushedCommits()
            }
            isLoading = false
        }
    }

    LaunchedEffect(repoFile) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    
                    // 1. Remote URL Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Remote Repository", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                            
                            if (!hasRemote) {
                                OutlinedTextField(
                                    value = remoteUrl,
                                    onValueChange = { remoteUrl = it },
                                    label = { Text("HTTPS URL") },
                                    placeholder = { Text("https://github.com/user/repo.git") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val result = gitManager.addRemote(remoteUrl)
                                            statusMessage = result
                                            loadData()
                                        }
                                    },
                                    enabled = remoteUrl.startsWith("http"),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Link Repository")
                                }
                            } else {
                                Text("Linked to origin", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 2. Authentication Section
                    if (hasRemote) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Authentication", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(12.dp))
                                
                                OutlinedTextField(
                                    value = token,
                                    onValueChange = { token = it },
                                    label = { Text("Personal Access Token") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Checkbox(
                                        checked = saveToken,
                                        onCheckedChange = { isChecked ->
                                            saveToken = isChecked
                                            if (isChecked) {
                                                prefsManager.saveToken(token)
                                            } else {
                                                prefsManager.clearToken()
                                            }
                                        }
                                    )
                                    Text("Remember Token")
                                }
                                
                                // Auto-save token on change if checked
                                LaunchedEffect(token) {
                                    if (saveToken) prefsManager.saveToken(token)
                                }
                            }
                        }
                    }
                    
                    if (statusMessage.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = statusMessage,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Outgoing Commits (${unpushedCommits.size})", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (unpushedCommits.isEmpty()) {
                        item {
                            Text(
                                "No unpushed commits.", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        items(unpushedCommits) { commit ->
                            UnpushedCommitCard(commit)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnpushedCommitCard(commit: CommitItem) {
    val dateFormat = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = commit.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = commit.author,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = dateFormat.format(commit.date),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}