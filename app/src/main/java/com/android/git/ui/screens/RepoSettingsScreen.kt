package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.R
import com.android.git.data.GitManager
import com.android.git.data.PreferencesManager
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.SnackbarType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoSettingsScreen(gitManager: GitManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    var userName by remember { mutableStateOf(prefs.getUserName()) }
    var userEmail by remember { mutableStateOf(prefs.getUserEmail()) }
    var token by remember { mutableStateOf(prefs.getToken()) }
    var remoteUrl by remember { mutableStateOf("") }
    var isRemoteLinked by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var statusType by remember { mutableStateOf(SnackbarType.INFO) }

    BackHandler(enabled = true) { onBack() }

    LaunchedEffect(gitManager) {
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
                title = { Text(stringResource(R.string.repo_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                RepoSettingsSection(title = stringResource(R.string.repo_settings_section_auth)) {
                    ModernInput(value = userName, onValueChange = { userName = it }, label = stringResource(R.string.repo_settings_label_user), icon = Icons.Default.AccountCircle)
                    Spacer(Modifier.height(16.dp))
                    ModernInput(value = userEmail, onValueChange = { userEmail = it }, label = stringResource(R.string.repo_settings_label_email), icon = Icons.Default.Email)
                    Spacer(Modifier.height(16.dp))
                    ModernInput(value = token, onValueChange = { token = it }, label = stringResource(R.string.repo_settings_label_token), icon = Icons.Default.Lock, visualTransformation = PasswordVisualTransformation())
                }

                RepoSettingsSection(title = stringResource(R.string.repo_settings_section_remote)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (isRemoteLinked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isRemoteLinked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = if (isRemoteLinked) Icons.Default.CloudSync else Icons.Default.CloudOff, contentDescription = null, tint = if (isRemoteLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (isRemoteLinked) stringResource(R.string.repo_settings_remote_connected) else stringResource(R.string.repo_settings_remote_disconnected),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isRemoteLinked) stringResource(R.string.repo_settings_remote_hint_connected) else stringResource(R.string.repo_settings_remote_hint_disconnected),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    ModernInput(value = remoteUrl, onValueChange = { remoteUrl = it }, label = stringResource(R.string.repo_settings_label_url), icon = Icons.Default.Link, placeholder = "https://github.com/user/repo.git")
                    Spacer(Modifier.height(16.dp))
                    if (remoteUrl.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    if (remoteUrl.startsWith("http")) {
                                        gitManager.addRemote(remoteUrl)
                                        statusMessage = context.getString(R.string.repo_settings_msg_remote_updated)
                                        statusType = SnackbarType.SUCCESS
                                        isRemoteLinked = true
                                    } else {
                                        statusMessage = context.getString(R.string.repo_settings_err_invalid_url)
                                        statusType = SnackbarType.ERROR
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isRemoteLinked) stringResource(R.string.repo_settings_btn_update_url) else stringResource(R.string.repo_settings_btn_link_remote))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            prefs.saveGitIdentity(userName, userEmail)
                            if (token.isNotEmpty()) prefs.saveToken(token) else prefs.clearToken()
                            gitManager.configureUser(userName, userEmail)
                            statusMessage = context.getString(R.string.repo_settings_msg_saved)
                            statusType = SnackbarType.SUCCESS
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.repo_settings_btn_save), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(32.dp))
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
                AppSnackbar(message = statusMessage, type = statusType, onDismiss = { statusMessage = "" })
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
            Column(modifier = Modifier.padding(20.dp)) { content() }
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