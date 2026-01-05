package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.data.GitManager
import com.android.git.data.PreferencesManager
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.SnackbarType
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoSettingsScreen(
    repoFile: File,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val gitManager = remember { GitManager(repoFile) }
    val scope = rememberCoroutineScope()

    // Config State
    var userName by remember { mutableStateOf(prefs.getUserName()) }
    var userEmail by remember { mutableStateOf(prefs.getUserEmail()) }
    var token by remember { mutableStateOf(prefs.getToken()) }
    
    // Remote State
    var remoteUrl by remember { mutableStateOf("") }
    var isRemoteLinked by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf("") }
    var statusType by remember { mutableStateOf(SnackbarType.INFO) }

    BackHandler(enabled = true) { onBack() }

    LaunchedEffect(Unit) {
        val currentUrl = gitManager.getRemoteUrl()
        if (currentUrl.isNotEmpty()) {
            remoteUrl = currentUrl
            isRemoteLinked = true
        } else {
            isRemoteLinked = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repository Settings") },
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
                // --- SECTION 1: Identity & Auth ---
                RepoSettingsSection(title = "User & Authentication") {
                    ModernInput(
                        value = userName,
                        onValueChange = { userName = it },
                        label = "User Name",
                        icon = Icons.Default.AccountCircle
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    ModernInput(
                        value = userEmail,
                        onValueChange = { userEmail = it },
                        label = "User Email",
                        icon = Icons.Default.Email
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    ModernInput(
                        value = token,
                        onValueChange = { token = it },
                        label = "Personal Access Token",
                        icon = Icons.Default.Lock,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                // --- SECTION 2: Remote Origin ---
                RepoSettingsSection(title = "Remote Repository") {
                    
                    // Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (isRemoteLinked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isRemoteLinked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isRemoteLinked) Icons.Default.CloudSync else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = if (isRemoteLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = if (isRemoteLinked) "Connected to Origin" else "No Remote Linked",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isRemoteLinked) "Syncing is available." else "Add a URL to enable push/pull.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))

                    ModernInput(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        label = "Remote URL (HTTPS)",
                        icon = Icons.Default.Link,
                        placeholder = "https://github.com/user/repo.git"
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    if (remoteUrl.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    if (remoteUrl.startsWith("http")) {
                                        gitManager.addRemote(remoteUrl)
                                        statusMessage = "Remote Updated!"
                                        statusType = SnackbarType.SUCCESS
                                        isRemoteLinked = true
                                    } else {
                                        statusMessage = "Invalid URL (Must start with http)"
                                        statusType = SnackbarType.ERROR
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isRemoteLinked) "Update URL" else "Link Remote")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Save Button
                Button(
                    onClick = {
                        scope.launch {
                            prefs.saveGitIdentity(userName, userEmail)
                            if (token.isNotEmpty()) prefs.saveToken(token) else prefs.clearToken()
                            
                            gitManager.configureUser(userName, userEmail)
                            
                            statusMessage = "Repository Settings Saved!"
                            statusType = SnackbarType.SUCCESS
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Save Repo Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.height(32.dp))
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
fun RepoSettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ModernInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        placeholder = { Text(placeholder) },
        visualTransformation = visualTransformation,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}