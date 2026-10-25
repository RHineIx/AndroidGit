package com.android.git.ui.screens

import com.android.git.ui.components.DropdownMenu

import com.android.git.ui.components.CircularProgressIndicator

import com.android.git.ui.components.GitIcons as Icons

import com.android.git.ui.components.OplusScaffold
import com.android.git.ui.components.OplusSectionTitle
import com.android.git.ui.components.Button
import com.android.git.ui.components.Card
import com.android.git.ui.components.DropdownMenuItem
import com.android.git.ui.components.FilledTonalButton
import com.android.git.ui.components.IconButton
import com.android.git.ui.components.OutlinedTextField
import com.android.git.ui.components.Switch

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.R
import com.android.git.data.GitAuthManager
import com.android.git.data.GitAuthMode
import com.android.git.data.PreferencesManager
import com.android.git.data.ThemeMode
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.GitAuthenticationPanel
import com.android.git.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val focusManager = LocalFocusManager.current
    val authManager = remember { GitAuthManager(context.filesDir) }

    val statusMessage = viewModel.statusMessage
    val statusType = viewModel.statusType
    val isLoading = viewModel.isLoading

    var autoOpen by remember { mutableStateOf(prefs.isAutoOpenEnabled()) }
    var themeExpanded by remember { mutableStateOf(false) }

    var userName by remember { mutableStateOf(prefs.getUserName()) }
    var userEmail by remember { mutableStateOf(prefs.getUserEmail()) }
    var authMode by remember { mutableStateOf(prefs.getAuthMode()) }
    var token by remember { mutableStateOf(prefs.getToken()) }
    var sshPrivateKey by remember { mutableStateOf(prefs.getSshPrivateKey()) }
    var sshPublicKey by remember { mutableStateOf(prefs.getSshPublicKey()) }
    var sshPassphrase by remember { mutableStateOf(prefs.getSshPassphrase()) }
    var authSaving by remember { mutableStateOf(false) }
    var authSaved by remember { mutableStateOf(false) }

    var geminiApiKey by remember { mutableStateOf(prefs.getGeminiApiKey()) }
    var geminiModel by remember { mutableStateOf(prefs.getGeminiModel()) }
    var geminiPrompt by remember { mutableStateOf(prefs.getGeminiPrompt()) }
    var aiModelExpanded by remember { mutableStateOf(false) }

    var apiKeyVisible by remember { mutableStateOf(false) }

    val aiModels = listOf(
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-flash-latest",
        "gemini-3.1-flash-preview",
        "gemma-3-27b-it"
    )

    OplusScaffold(
        collapsible = false,
        title = stringResource(R.string.settings_title),
        onBack = onBack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                SettingsSection(title = stringResource(R.string.settings_section_app)) {

                    val themeNames = listOf(
                        stringResource(R.string.settings_theme_system),
                        stringResource(R.string.settings_theme_light),
                        stringResource(R.string.settings_theme_dark)
                    )
                    val currentThemeName = when (viewModel.themeMode) {
                        ThemeMode.SYSTEM -> themeNames[0]
                        ThemeMode.LIGHT -> themeNames[1]
                        ThemeMode.DARK -> themeNames[2]
                    }

                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { themeExpanded = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.settings_theme_title),
                                    fontWeight = FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    currentThemeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }

                        DropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                DropdownMenuItem(
                                    text = { Text(themeNames[index]) },
                                    onClick = {
                                        viewModel.updateThemeMode(mode)
                                        themeExpanded = false
                                    },
                                    trailingIcon = {
                                        if (viewModel.themeMode == mode) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_auto_open),
                                fontWeight = FontWeight.Normal,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                stringResource(R.string.settings_auto_open_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoOpen,
                            onCheckedChange = {
                                autoOpen = it
                                prefs.setAutoOpenEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.background,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                GlobalAuthenticationSection(
                    prefs = prefs,
                    authManager = authManager,
                    userName = userName,
                    onUserNameChange = { userName = it },
                    userEmail = userEmail,
                    onUserEmailChange = { userEmail = it },
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
                    authSaving = authSaving,
                    onAuthSavingChange = { authSaving = it },
                    authSaved = authSaved,
                    onAuthSavedChange = { authSaved = it }
                )

                SettingsSection(title = stringResource(R.string.settings_ai_section)) {
                    OutlinedTextField(
                        value = geminiApiKey,
                        onValueChange = { geminiApiKey = it },
                        label = { Text(stringResource(R.string.settings_gemini_api_key)) },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                        trailingIcon = {
                            val image = if (apiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            val description = stringResource(if (apiKeyVisible) R.string.settings_hide_api_key else R.string.settings_show_api_key)
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    ExposedDropdownMenuBox(expanded = aiModelExpanded, onExpandedChange = { aiModelExpanded = it }) {
                        OutlinedTextField(
                            value = geminiModel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.settings_ai_model)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aiModelExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        ExposedDropdownMenu(expanded = aiModelExpanded, onDismissRequest = { aiModelExpanded = false }, modifier = Modifier.fillMaxWidth(0.85f)) {
                            aiModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = { geminiModel = model; aiModelExpanded = false },
                                    trailingIcon = { if (geminiModel == model) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = geminiPrompt,
                        onValueChange = { geminiPrompt = it },
                        label = { Text(stringResource(R.string.settings_custom_prompt)) },
                        placeholder = { Text(stringResource(R.string.settings_custom_prompt_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        minLines = 3,
                        maxLines = 6
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { focusManager.clearFocus(); viewModel.verifyGeminiSettings(geminiApiKey, geminiModel, geminiPrompt) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_verifying))
                        } else {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_verify_save), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                DeveloperSection(context)

                AppVersionFooter(
                    context = context,
                    onCheckUpdate = onCheckUpdate
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                AppSnackbar(
                    message = statusMessage,
                    type = statusType,
                    onDismiss = { viewModel.clearStatus() }
                )
            }
        }
    }
}

@Composable
private fun GlobalAuthenticationSection(
    prefs: PreferencesManager,
    authManager: GitAuthManager,
    userName: String,
    onUserNameChange: (String) -> Unit,
    userEmail: String,
    onUserEmailChange: (String) -> Unit,
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
    authSaving: Boolean,
    onAuthSavingChange: (Boolean) -> Unit,
    authSaved: Boolean,
    onAuthSavedChange: (Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_auth_section)) {
        OutlinedTextField(
            value = userName,
            onValueChange = onUserNameChange,
            label = { Text(stringResource(R.string.settings_auth_user_name)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            enabled = !authSaving
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = userEmail,
            onValueChange = onUserEmailChange,
            label = { Text(stringResource(R.string.settings_auth_user_email)) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !authSaving
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.settings_auth_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        GitAuthenticationPanel(
            authManager = authManager,
            userEmail = userEmail,
            authMode = authMode,
            onAuthModeChange = { onAuthModeChange(it); onAuthSavedChange(false) },
            token = token,
            onTokenChange = { onTokenChange(it); onAuthSavedChange(false) },
            sshPrivateKey = sshPrivateKey,
            onSshPrivateKeyChange = { onSshPrivateKeyChange(it); onAuthSavedChange(false) },
            sshPublicKey = sshPublicKey,
            onSshPublicKeyChange = { onSshPublicKeyChange(it); onAuthSavedChange(false) },
            sshPassphrase = sshPassphrase,
            onSshPassphraseChange = { onSshPassphraseChange(it); onAuthSavedChange(false) },
            isBusy = authSaving,
            onClearSshData = {
                prefs.clearSshKey()
                onAuthSavedChange(false)
            }
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onAuthSavingChange(true)
                prefs.setUserName(userName)
                prefs.setUserEmail(userEmail)
                prefs.setAuthMode(authMode)
                if (authMode == GitAuthMode.HTTPS) prefs.saveToken(token)
                else prefs.saveSshKey(sshPrivateKey, sshPublicKey, sshPassphrase)
                onAuthSavingChange(false)
                onAuthSavedChange(true)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !authSaving && (authMode == GitAuthMode.HTTPS || sshPrivateKey.isNotBlank()),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (authSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            else {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
            }
        }
        if (authSaved) Text(stringResource(R.string.settings_auth_saved), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun AppVersionFooter(
    context: Context,
    onCheckUpdate: () -> Unit
) {
    val packageInfo = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    val versionName = packageInfo?.versionName ?: "Unknown"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode ?: 0
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode ?: 0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onCheckUpdate,
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            modifier = Modifier.padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.settings_version_fmt, versionName, versionCode),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.settings_check_updates_hint),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun DeveloperSection(context: Context) {
    SettingsSection(title = stringResource(R.string.settings_developer_by)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Image(
                    painter = painterResource(id = R.drawable.me),
                    contentDescription = stringResource(R.string.settings_developer_image_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("RHineix", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("@RHineix", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SocialButton(iconRes = R.drawable.ic_github, label = stringResource(R.string.settings_social_github), onClick = { openUrl(context, "https://github.com/RHineix") })
            SocialButton(iconRes = R.drawable.ic_telegram, label = stringResource(R.string.settings_social_telegram), onClick = { openUrl(context, "https://t.me/RHineix") })
        }
    }
}

@Composable
fun SocialButton(iconRes: Int, label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(10.dp))
        Text(label, fontWeight = FontWeight.Medium)
    }
}

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) { }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OplusSectionTitle(title = title)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}
