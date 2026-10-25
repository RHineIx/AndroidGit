package com.android.git.ui.components

import com.android.git.ui.components.GitIcons as Icons

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.data.GitAuthManager
import com.android.git.data.GitAuthMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitAuthenticationPanel(
    authManager: GitAuthManager,
    userEmail: String,
    authMode: GitAuthMode,
    onAuthModeChange: (GitAuthMode) -> Unit,
    token: String,
    onTokenChange: (String) -> Unit,
    sshPrivateKey: String,
    onSshPrivateKeyChange: (String) -> Unit,
    sshPublicKey: String,
    onSshPublicKeyChange: (String) -> Unit,
    sshPassphrase: String,
    onSshPassphraseChange: (String) -> Unit,
    isBusy: Boolean,
    onClearSshData: () -> Unit,
    modifier: Modifier = Modifier,
    httpsFooter: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val modernShape = RoundedCornerShape(12.dp)

    // Internal states managed entirely by the component
    var tokenVisible by remember { mutableStateOf(false) }
    var generatedAlgorithm by remember { mutableStateOf("") }
    var sshGenerationError by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

    // SSH Key Deletion Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.repo_settings_ssh_clear)) },
            text = { Text("Are you sure you want to remove the SSH keys from this device? This action cannot be undone.") },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onSshPrivateKeyChange("")
                        onSshPublicKeyChange("")
                        onSshPassphraseChange("")
                        onAuthModeChange(GitAuthMode.HTTPS)
                        onClearSshData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {

        // Use the same native segmented control as the reference's binding pages.
        val authTabs = listOf(
            stringResource(R.string.auth_mode_https),
            stringResource(R.string.auth_mode_ssh)
        )
        io.github.suqi8.coui.kmp.basic.TabRowWithContour(
            tabs = authTabs,
            selectedTabIndex = if (authMode == GitAuthMode.HTTPS) 0 else 1,
            onTabSelected = { if (!isBusy) onAuthModeChange(if (it == 0) GitAuthMode.HTTPS else GitAuthMode.SSH) },
            modifier = Modifier.fillMaxWidth().then(if (isBusy) Modifier.alpha(0.4f) else Modifier)
        )

        Spacer(Modifier.height(16.dp))

        AnimatedContent(
            targetState = authMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "AuthModeTransition"
        ) { mode ->
            if (mode == GitAuthMode.HTTPS) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = token,
                        onValueChange = onTokenChange,
                        label = { Text(stringResource(R.string.repo_settings_label_token)) },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                        trailingIcon = {
                            val image = if (tokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(imageVector = image, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = modernShape,
                        colors = textFieldColors,
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        enabled = !isBusy,
                        singleLine = true
                    )

                    httpsFooter()
                }

            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.ssh_clone_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    ExpandableSshTextField(
                        value = sshPrivateKey,
                        onValueChange = {
                            onSshPrivateKeyChange(it)
                            generatedAlgorithm = ""
                            sshGenerationError = ""
                        },
                        label = { Text(stringResource(R.string.ssh_private_key_label)) },
                        leadingIcon = Icons.Default.Key,
                        enabled = !isBusy,
                        isSecret = true,
                        showDescription = stringResource(R.string.ssh_show_value),
                        hideDescription = stringResource(R.string.ssh_hide_value),
                        maxExpandedLines = 10,
                        shape = modernShape
                    )

                    Spacer(Modifier.height(12.dp))

                    ExpandableSshTextField(
                        value = sshPassphrase,
                        onValueChange = onSshPassphraseChange,
                        label = { Text(stringResource(R.string.ssh_passphrase_label)) },
                        leadingIcon = Icons.Default.Lock,
                        enabled = !isBusy,
                        isSecret = true,
                        showDescription = stringResource(R.string.ssh_show_value),
                        hideDescription = stringResource(R.string.ssh_hide_value),
                        maxExpandedLines = 3,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = modernShape
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExpandableSshTextField(
                            value = sshPublicKey,
                            onValueChange = {
                                onSshPublicKeyChange(it)
                                generatedAlgorithm = ""
                                sshGenerationError = ""
                            },
                            label = { Text(stringResource(R.string.ssh_public_key_label)) },
                            leadingIcon = Icons.Default.Key,
                            enabled = !isBusy,
                            showDescription = stringResource(R.string.ssh_show_value),
                            hideDescription = stringResource(R.string.ssh_hide_value),
                            modifier = Modifier.weight(1f),
                            maxExpandedLines = 4,
                            shape = modernShape
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("AndroidGit SSH public key", sshPublicKey)
                                )
                            },
                            enabled = !isBusy && sshPublicKey.isNotBlank(),
                            modifier = Modifier.size(50.dp),
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.repo_settings_ssh_copy_public)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    FilledTonalButton(
                        onClick = {
                            val result = runCatching {
                                authManager.generateKeyPair(
                                    passphrase = sshPassphrase,
                                    email = userEmail
                                )
                            }
                            result.onSuccess { generated ->
                                onSshPrivateKeyChange(generated.privateKey)
                                onSshPublicKeyChange(generated.publicKey)
                                sshGenerationError = ""
                                generatedAlgorithm = generated.algorithm
                            }.onFailure { error ->
                                generatedAlgorithm = ""
                                sshGenerationError = buildString {
                                    append(error::class.java.simpleName)
                                    error.message?.takeIf { it.isNotBlank() }?.let {
                                        append(": ")
                                        append(it)
                                    }
                                }
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = modernShape
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ssh_generate_key), fontWeight = FontWeight.Bold)
                    }

                    if (generatedAlgorithm.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.ssh_generated_fmt, generatedAlgorithm),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (sshGenerationError.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = sshGenerationError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (sshPrivateKey.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = modernShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.repo_settings_ssh_clear), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}