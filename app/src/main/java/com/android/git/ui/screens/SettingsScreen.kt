package com.android.git.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.Save
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
import com.android.git.ui.components.SnackbarType
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

    var userName by remember { mutableStateOf(prefs.getUserName()) }
    var userEmail by remember { mutableStateOf(prefs.getUserEmail()) }
    var token by remember { mutableStateOf(prefs.getToken()) }
    var remoteUrl by remember { mutableStateOf("") }
    var autoOpen by remember { mutableStateOf(prefs.isAutoOpenEnabled()) }

    var statusMessage by remember { mutableStateOf("") }
    var statusType by remember { mutableStateOf(SnackbarType.INFO) }

    BackHandler(enabled = true) { onBack() }

    LaunchedEffect(Unit) {
        if (gitManager.hasRemote()) {
            remoteUrl = "Remote Linked"
        }
    }

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
                                "Automatically reopen the last used repository on app launch.",
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

                // --- SECTION 2: Repository Configuration ---
                SettingsSection(title = "Repository Configuration") {
                    
                    Text("Identity (Required)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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

                    Text("Authentication (HTTPS Token)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                        label = { Text("Remote URL") },
                        placeholder = { Text("https://github.com/user/repo.git") },
                        leadingIcon = { Icon(Icons.Default.Link, null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                prefs.saveGitIdentity(userName, userEmail)
                                if (token.isNotEmpty()) prefs.saveToken(token) else prefs.clearToken()
                                
                                gitManager.configureUser(userName, userEmail)
                                if (remoteUrl.isNotEmpty() && remoteUrl.startsWith("http")) {
                                    gitManager.addRemote(remoteUrl)
                                }
                                statusMessage = "Configuration Saved!"
                                statusType = SnackbarType.SUCCESS
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Configuration")
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
                AppSnackbar(
                    message = statusMessage,
                    type = statusType,
                    onDismiss = { statusMessage = "" }
                )
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