package com.android.git.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.data.GitAuthManager
import com.android.git.data.GitAuthMode
import com.android.git.data.GitManager
import com.android.git.data.PreferencesManager
import com.android.git.ui.components.GitAuthenticationPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoSettingsScreen(
    gitManager: GitManager?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val authManager = remember { GitAuthManager(context.filesDir) }
    val scope = rememberCoroutineScope()

    var userName by remember { mutableStateOf(prefs.getUserName()) }
    var userEmail by remember { mutableStateOf(prefs.getUserEmail()) }
    var token by remember { mutableStateOf(prefs.getToken()) }
    var authMode by remember { mutableStateOf(prefs.getAuthMode()) }
    var sshPrivateKey by remember { mutableStateOf(prefs.getSshPrivateKey()) }
    var sshPublicKey by remember { mutableStateOf(prefs.getSshPublicKey()) }
    var sshPassphrase by remember { mutableStateOf(prefs.getSshPassphrase()) }
    var isSaving by remember { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(16.dp)
    val textFieldShape = RoundedCornerShape(16.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.repo_settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.repo_settings_section_auth),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShape,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text(stringResource(R.string.repo_settings_label_user)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        singleLine = true,
                        enabled = !isSaving
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = { userEmail = it },
                        label = { Text(stringResource(R.string.repo_settings_label_email)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = !isSaving
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Text(
                        text = stringResource(R.string.repo_settings_auth_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    GitAuthenticationPanel(
                        authManager = authManager,
                        userEmail = userEmail,
                        authMode = authMode,
                        onAuthModeChange = { authMode = it },
                        token = token,
                        onTokenChange = { token = it },
                        sshPrivateKey = sshPrivateKey,
                        onSshPrivateKeyChange = { sshPrivateKey = it },
                        sshPublicKey = sshPublicKey,
                        onSshPublicKeyChange = { sshPublicKey = it },
                        sshPassphrase = sshPassphrase,
                        onSshPassphraseChange = { sshPassphrase = it },
                        isBusy = isSaving,
                        onClearSshData = { prefs.clearSshKey() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isSaving = true
                    prefs.setUserName(userName)
                    prefs.setUserEmail(userEmail)
                    prefs.setAuthMode(authMode)
                    if (authMode == GitAuthMode.HTTPS) {
                        prefs.saveToken(token)
                    } else {
                        prefs.saveSshKey(sshPrivateKey, sshPublicKey, sshPassphrase)
                    }

                    scope.launch(Dispatchers.IO) {
                        gitManager?.configureUser(userName, userEmail)
                        withContext(Dispatchers.Main) {
                            isSaving = false
                            onBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = cardShape,
                enabled = !isSaving && (authMode == GitAuthMode.HTTPS || sshPrivateKey.isNotBlank())
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.repo_settings_btn_save), style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}