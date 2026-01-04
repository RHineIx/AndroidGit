package com.android.git.ui.screens

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.android.git.data.GitManager
import com.android.git.data.PreferencesManager
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneScreen(
    onBack: () -> Unit,
    onCloneSuccess: (File) -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    
    // Form State
    var repoUrl by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf("") }
    var token by remember { mutableStateOf(prefsManager.getToken()) }
    var saveToken by remember { mutableStateOf(token.isNotEmpty()) }
    
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    // Auto-fill folder name from URL
    LaunchedEffect(repoUrl) {
        if (repoUrl.endsWith(".git")) {
            val name = repoUrl.substringAfterLast("/").substringBefore(".git")
            if (folderName.isEmpty()) folderName = name
        }
    }

    BackHandler(enabled = !isLoading) {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clone Repository") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) {
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    
                    // 1. URL
                    Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = repoUrl,
                        onValueChange = { repoUrl = it },
                        label = { Text("Repository URL") },
                        placeholder = { Text("https://github.com/user/project.git") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !isLoading
                    )
                    
                    Spacer(Modifier.height(16.dp))

                    // 2. Folder Name
                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Local Folder Name") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !isLoading
                    )
                    Text(
                        "Will be created in /Internal Storage/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    // 3. Token
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("Token (Optional for Public)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        enabled = !isLoading
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Checkbox(
                            checked = saveToken,
                            onCheckedChange = { isChecked ->
                                saveToken = isChecked
                                if (!isChecked) prefsManager.clearToken()
                            },
                            enabled = !isLoading
                        )
                        Text("Remember Token")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Clone Button
            Button(
                onClick = {
                    scope.launch {
                        if (saveToken && token.isNotEmpty()) prefsManager.saveToken(token)
                        
                        isLoading = true
                        statusMessage = "Cloning..."
                        
                        // Default Clone Path: Root of Internal Storage
                        val parentDir = Environment.getExternalStorageDirectory()
                        
                        val result = GitManager.cloneRepo(repoUrl, parentDir, folderName, token)
                        
                        if (result.first != null) {
                            statusMessage = "Success!"
                            onCloneSuccess(result.first!!)
                        } else {
                            statusMessage = result.second
                            isLoading = false
                        }
                    }
                },
                enabled = repoUrl.isNotEmpty() && folderName.isNotEmpty() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(16.dp))
                    Text("Cloning...")
                } else {
                    Icon(Icons.Default.CloudDownload, null)
                    Spacer(Modifier.width(16.dp))
                    Text("Clone Repository")
                }
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = statusMessage,
                    color = if (statusMessage.contains("Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}