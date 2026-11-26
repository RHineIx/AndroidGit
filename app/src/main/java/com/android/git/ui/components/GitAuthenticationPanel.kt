package com.android.git.ui.components

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
import com.android.git.data.GitAuthConfig
import com.android.git.data.PreferencesManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GitCredentialFields(
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
    onGeneratingChange: (Boolean) -> Unit,
    onClearSshData: () -> Unit,
    modifier: Modifier = Modifier,
    httpsFooter: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val modernShape = RoundedCornerShape(16.dp)

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
            icon = { Icon(GitIcons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        generatedAlgorithm = ""
                        sshGenerationError = ""
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

        // Modern Segmented Control for Auth Mode
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, modernShape)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val httpsSelected = authMode == GitAuthMode.HTTPS
            val sshSelected = authMode == GitAuthMode.SSH

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (httpsSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable(enabled = !isBusy) { onAuthModeChange(GitAuthMode.HTTPS) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        GitIcons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (httpsSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.auth_mode_https),
                        color = if (httpsSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (sshSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable(enabled = !isBusy) { onAuthModeChange(GitAuthMode.SSH) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        GitIcons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (sshSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.auth_mode_ssh),
                        color = if (sshSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

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
                        leadingIcon = { Icon(GitIcons.Default.VpnKey, contentDescription = null) },
                        trailingIcon = {
                            val image = if (tokenVisible) GitIcons.Default.Visibility else GitIcons.Default.VisibilityOff
                            IconButton(onClick = { tokenVisible = !tokenVisible }, enabled = !isBusy) {
                                Icon(imageVector = image, contentDescription = stringResource(if (tokenVisible) R.string.ssh_hide_value else R.string.ssh_show_value))
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
                            Icon(GitIcons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
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
                        leadingIcon = GitIcons.Default.Key,
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
                        leadingIcon = GitIcons.Default.Password,
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
                            leadingIcon = GitIcons.Default.Key,
                            enabled = !isBusy,
                            showDescription = stringResource(R.string.ssh_show_value),
                            hideDescription = stringResource(R.string.ssh_hide_value),
                            modifier = Modifier.weight(1f),
                            maxExpandedLines = 4,
                            shape = modernShape
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledTonalIconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("AndroidGit SSH public key", sshPublicKey)
                                )
                            },
                            enabled = !isBusy && sshPublicKey.isNotBlank(),
                            modifier = Modifier.size(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                GitIcons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.repo_settings_ssh_copy_public)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    FilledTonalButton(
                        onClick = {
                            onGeneratingChange(true)
                            scope.launch {
                                try {
                                    val generated = withContext(Dispatchers.IO) {
                                        authManager.generateKeyPair(
                                            passphrase = sshPassphrase,
                                            email = userEmail
                                        )
                                    }
                                    onSshPrivateKeyChange(generated.privateKey)
                                    onSshPublicKeyChange(generated.publicKey)
                                    sshGenerationError = ""
                                    generatedAlgorithm = generated.algorithm
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (error: Exception) {
                                    generatedAlgorithm = ""
                                    sshGenerationError = error::class.java.simpleName + ": " +
                                        (error.message ?: "Key generation failed")
                                } finally {
                                    onGeneratingChange(false)
                                }
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = modernShape
                    ) {
                        Icon(GitIcons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ssh_generate_key), fontWeight = FontWeight.Bold)
                    }

                    if (generatedAlgorithm.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(GitIcons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.ssh_generated_fmt, generatedAlgorithm),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2E7D32),
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
                                Icon(GitIcons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
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

                    if (sshPrivateKey.isNotBlank() || sshPublicKey.isNotBlank() || sshPassphrase.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = modernShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(GitIcons.Default.DeleteOutline, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.repo_settings_ssh_clear), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** A screen-local draft; credentials are never written to a saved-state Bundle. */
internal data class AuthenticationDraft(
    val userName: String,
    val userEmail: String,
    val mode: GitAuthMode,
    val token: String,
    val privateKey: String,
    val publicKey: String,
    val passphrase: String,
    val rememberToken: Boolean
) {
    val isValid: Boolean get() = mode == GitAuthMode.HTTPS || privateKey.isNotBlank()
    fun config() = GitAuthConfig(mode, token, privateKey, passphrase)
}

@Stable
internal class AuthenticationFormState(private val prefs: PreferencesManager, rememberTokenDefault: Boolean) {
    var draft by mutableStateOf(
        AuthenticationDraft(
            prefs.getUserName(), prefs.getUserEmail(), prefs.getAuthMode(),
            prefs.getToken(), prefs.getSshPrivateKey(), prefs.getSshPublicKey(),
            prefs.getSshPassphrase(), rememberTokenDefault || prefs.getToken().isNotEmpty()
        )
    )
    var generating by mutableStateOf(false)
    var lastToken by mutableStateOf(prefs.getLastValidToken())
        private set

    fun save(value: AuthenticationDraft = draft) {
        prefs.saveAuthentication(
            value.userName, value.userEmail, value.mode, value.token,
            value.privateKey, value.publicKey, value.passphrase, value.rememberToken
        )
        lastToken = prefs.getLastValidToken()
    }

    fun removeSshKey() {
        prefs.clearSshKey()
        draft = draft.copy(mode = GitAuthMode.HTTPS, privateKey = "", publicKey = "", passphrase = "")
    }
}

@Composable
internal fun rememberAuthenticationForm(
    prefs: PreferencesManager,
    rememberTokenDefault: Boolean = true
): AuthenticationFormState = remember(prefs) { AuthenticationFormState(prefs, rememberTokenDefault) }

/** Shared identity, authentication modes and credential controls for all three screens. */
@Composable
internal fun AuthenticationFields(
    state: AuthenticationFormState,
    busy: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manager = remember(context) { GitAuthManager(context.filesDir) }
    val draft = state.draft
    val locked = busy || state.generating
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = draft.userName,
            onValueChange = { state.draft = state.draft.copy(userName = it) },
            label = { Text(stringResource(R.string.settings_auth_user_name)) },
            leadingIcon = { Icon(GitIcons.Default.Person, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            enabled = !locked
        )
        OutlinedTextField(
            value = draft.userEmail,
            onValueChange = { state.draft = state.draft.copy(userEmail = it) },
            label = { Text(stringResource(R.string.settings_auth_user_email)) },
            leadingIcon = { Icon(GitIcons.Default.Email, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !locked
        )
        HorizontalDivider()
        Text(
            stringResource(R.string.settings_auth_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        GitCredentialFields(
            authManager = manager,
            userEmail = draft.userEmail,
            authMode = draft.mode,
            onAuthModeChange = { state.draft = state.draft.copy(mode = it) },
            token = draft.token,
            onTokenChange = { state.draft = state.draft.copy(token = it) },
            sshPrivateKey = draft.privateKey,
            onSshPrivateKeyChange = {
                state.draft = state.draft.copy(privateKey = it, publicKey = "")
            },
            sshPublicKey = draft.publicKey,
            onSshPublicKeyChange = { state.draft = state.draft.copy(publicKey = it) },
            sshPassphrase = draft.passphrase,
            onSshPassphraseChange = { state.draft = state.draft.copy(passphrase = it) },
            isBusy = locked,
            onGeneratingChange = { state.generating = it },
            onClearSshData = state::removeSshKey,
            httpsFooter = {
                if (draft.token.isBlank() && state.lastToken.isNotBlank()) {
                    SuggestionChip(
                        onClick = { state.draft = state.draft.copy(token = state.lastToken) },
                        label = { Text(stringResource(R.string.auth_restore_token)) },
                        enabled = !locked
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = draft.rememberToken,
                        onCheckedChange = { state.draft = state.draft.copy(rememberToken = it) },
                        enabled = !locked
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.clone_remember_token))
                }
            }
        )
        if (state.generating) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

/** Both settings screens use the exact same card, validation and save flow. */
@Composable
internal fun AuthenticationSettings(
    state: AuthenticationFormState,
    beforeSave: suspend (AuthenticationDraft) -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<AuthenticationDraft?>(null) }
    var error by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.settings_auth_section),
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
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AuthenticationFields(state, saving)
                Button(
                    onClick = {
                        val snapshot = state.draft
                        saving = true
                        error = false
                        scope.launch {
                            try {
                                beforeSave(snapshot)
                                state.save(snapshot)
                                saved = snapshot
                                onSaved()
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                error = true
                            } finally {
                                saving = false
                            }
                        }
                    },
                    enabled = !saving && !state.generating && state.draft.isValid,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(GitIcons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                    }
                }
                if (error) Text(stringResource(R.string.auth_save_failed), color = MaterialTheme.colorScheme.error)
                else if (saved == state.draft) Text(
                    stringResource(R.string.settings_auth_saved),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
