package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.data.PreferencesManager
import com.android.git.ui.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onCloneSuccess: (File) -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    
    var repoUrl by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf("") }
    var token by remember { mutableStateOf(prefsManager.getToken()) }
    var saveToken by remember { mutableStateOf(token.isNotEmpty()) }
    
    val isLoading = viewModel.isLoading
    val statusMessage = viewModel.statusMessage
    val statusType = viewModel.statusType

    LaunchedEffect(repoUrl) {
        if (repoUrl.endsWith(".git")) {
            val name = repoUrl.substringAfterLast("/").substringBefore(".git")
            if (folderName.isEmpty()) folderName = name
        }
    }

    BackHandler(enabled = !isLoading) {
        viewModel.clearStatus()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.clone_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = repoUrl,
                        onValueChange = { repoUrl = it },
                        label = { Text(stringResource(R.string.clone_url_label)) },
                        placeholder = { Text(stringResource(R.string.clone_url_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !isLoading
                    )
                    
                    Spacer(Modifier.height(16.dp))

                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text(stringResource(R.string.clone_folder_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !isLoading
                    )
                    Text(
                        stringResource(R.string.clone_folder_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text(stringResource(R.string.clone_token_label)) },
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
                        Text(stringResource(R.string.clone_remember_token))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (saveToken && token.isNotEmpty()) {
                        prefsManager.saveToken(token)
                    }
                    viewModel.cloneRepository(repoUrl, folderName, token, onCloneSuccess)
                },
                enabled = repoUrl.isNotEmpty() && folderName.isNotEmpty() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.clone_status_running))
                } else {
                    Icon(Icons.Default.CloudDownload, null)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.clone_btn_action))
                }
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = statusMessage,
                    color = if (statusType == com.android.git.ui.components.SnackbarType.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}