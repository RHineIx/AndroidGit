package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
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
import com.android.git.ui.components.AppSnackbar
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repoFile: File,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val gitManager = remember { GitManager(repoFile) }
    val scope = rememberCoroutineScope()

    // Git Config State
    var userName by remember { mutableStateOf(prefs.getUserName()) }
    var userEmail by remember { mutableStateOf(prefs.getUserEmail()) }
    var token by remember { mutableStateOf(prefs.getToken()) }
    var remoteUrl by remember { mutableStateOf("") }
    
    // App Config State
    var autoOpen by remember { mutableStateOf(prefs.isAutoOpenEnabled()) }

    var statusMessage by remember { mutableStateOf("") }

    BackHandler(enabled = true) { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                
                // --- SECTION 1: App Preferences ---
                SettingsSection(title = "App Preferences") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-open last project", fontWeight = FontWeight.Bold)
                            Text(
                                "Automatically open the last used repository when the app starts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoOpen,
                            onCheckedChange = { 
                                autoOpen = it
                                prefs.setAutoOpenEnabled(it)
                            }
                        )
                    }
                }

                // --- SECTION 2: Repository Configuration (Consolidated) ---
                SettingsSection(title = "Repository Configuration") {
                    Text("Identity (User Info)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = userName, onValueChange = { userName = it },
                        label = { Text("User Name") },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userEmail, onValueChange = { userEmail = it },
                        label = { Text("User Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))

                    Text("Authentication (Token)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = token, onValueChange = { token = it },
                        label = { Text("Personal Access Token") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))

                    Text("Remote Origin", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = remoteUrl, onValueChange = { remoteUrl = it },
                        label = { Text("Set/Update Remote URL") },
                        leadingIcon = { Icon(Icons.Default.Link, null) },
                        placeholder = { Text("https://github.com/user/repo.git") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                // Save Prefs
                                prefs.saveGitIdentity(userName, userEmail)
                                if (token.isNotEmpty()) prefs.saveToken(token) else prefs.clearToken()
                                
                                // Apply to Repo
                                gitManager.configureUser(userName, userEmail)
                                if (remoteUrl.isNotEmpty()) {
                                    gitManager.addRemote(remoteUrl)
                                }
                                statusMessage = "Configuration Saved Successfully!"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save All Configurations")
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                AppSnackbar(message = statusMessage, onDismiss = { statusMessage = "" })
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}