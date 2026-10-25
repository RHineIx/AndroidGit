package com.android.git.ui.screens

import com.android.git.ui.components.DropdownMenu

import com.android.git.ui.components.CircularProgressIndicator

import com.android.git.ui.components.GitIcons as Icons

import com.android.git.ui.components.OplusScaffold
import com.android.git.ui.components.OplusSectionTitle
import com.android.git.ui.components.OplusPreferenceGroup
import com.android.git.ui.components.OplusPreferenceRow
import com.android.git.ui.components.OplusPreferenceDivider
import com.android.git.ui.components.Button
import com.android.git.ui.components.DropdownMenuItem
import com.android.git.ui.components.IconButton
import com.android.git.ui.components.OutlinedTextField
import com.android.git.ui.components.Switch

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import io.github.suqi8.coui.kmp.basic.PreciseClickState
import io.github.suqi8.coui.kmp.basic.preciseClickAnchor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.data.GitAuthManager
import com.android.git.data.GitAuthMode
import com.android.git.data.PreferencesManager
import com.android.git.data.ThemeMode
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.GitAuthenticationPanel
import com.android.git.ui.viewmodel.MainViewModel
import io.github.lyxnx.compose.ui.tablericons.TablerIcons
import io.github.lyxnx.compose.ui.tablericons.outline.BrightnessAuto
import io.github.lyxnx.compose.ui.tablericons.outline.BrandTelegram
import io.github.lyxnx.compose.ui.tablericons.outline.Moon
import io.github.lyxnx.compose.ui.tablericons.outline.Selector
import io.github.lyxnx.compose.ui.tablericons.outline.Sun
import io.github.lyxnx.compose.ui.tablericons.outline.Versions

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
    val themeAnchor = remember { PreciseClickState() }

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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                SettingsSection(title = stringResource(R.string.settings_section_app), padded = false) {
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
                    val themeIcons = listOf(
                        TablerIcons.Outline.BrightnessAuto,
                        TablerIcons.Outline.Sun,
                        TablerIcons.Outline.Moon
                    )
                    Box {
                        OplusPreferenceRow(
                            title = stringResource(R.string.settings_theme_title),
                            modifier = Modifier.preciseClickAnchor(themeAnchor),
                            summary = currentThemeName,
                            icon = TablerIcons.Outline.BrightnessAuto,
                            onClick = { themeExpanded = true },
                            trailing = {
                                Icon(TablerIcons.Outline.Selector, contentDescription = null,
                                    modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )
                        DropdownMenu(
                            expanded = themeExpanded,
                            width = 176.dp,
                            preciseClickState = themeAnchor,
                            onDismissRequest = { themeExpanded = false }
                        ) {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                val selected = viewModel.themeMode == mode
                                DropdownMenuItem(
                                    showDivider = index > 0,
                                    text = {
                                        Text(themeNames[index], color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (selected) Icons.Default.Check else themeIcons[index],
                                            contentDescription = null,
                                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateThemeMode(mode)
                                        themeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OplusPreferenceDivider()
                    OplusPreferenceRow(
                        title = stringResource(R.string.settings_auto_open),
                        summary = stringResource(R.string.settings_auto_open_desc),
                        icon = Icons.Default.FolderOpen,
                        onClick = {
                            autoOpen = !autoOpen
                            prefs.setAutoOpenEnabled(autoOpen)
                        },
                        trailing = {
                            Switch(
                                checked = autoOpen,
                                onCheckedChange = {
                                    autoOpen = it
                                    prefs.setAutoOpenEnabled(it)
                                }
                            )
                        }
                    )
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

                SettingsSection(title = stringResource(R.string.settings_ai_section), padded = false) {
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = geminiApiKey,
                            onValueChange = { geminiApiKey = it },
                            label = { Text(stringResource(R.string.settings_gemini_api_key)) },
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(22.dp)) },
                            trailingIcon = {
                                val image = if (apiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                val description = stringResource(if (apiKeyVisible) R.string.settings_hide_api_key else R.string.settings_show_api_key)
                                IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                    Icon(imageVector = image, contentDescription = description)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )

                    }
                    OplusPreferenceDivider()
                    Box {
                        OplusPreferenceRow(
                            title = stringResource(R.string.settings_ai_model),
                            summary = geminiModel,
                            icon = Icons.Default.AutoAwesome,
                            onClick = { aiModelExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            trailing = {
                                Icon(TablerIcons.Outline.Selector, contentDescription = null,
                                    modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )
                        DropdownMenu(expanded = aiModelExpanded, onDismissRequest = { aiModelExpanded = false }, width = 272.dp) {
                            aiModels.forEach { model ->
                                val selected = geminiModel == model
                                DropdownMenuItem(
                                    text = { Text(model, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                                    onClick = { geminiModel = model; aiModelExpanded = false },
                                    leadingIcon = {
                                        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        else Spacer(Modifier.size(22.dp))
                                    }
                                )
                            }
                        }
                    }

                    OplusPreferenceDivider()
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = geminiPrompt,
                            onValueChange = { geminiPrompt = it },
                            label = { Text(stringResource(R.string.settings_custom_prompt)) },
                            placeholder = { Text(stringResource(R.string.settings_custom_prompt_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 3,
                            maxLines = 6
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { focusManager.clearFocus(); viewModel.verifyGeminiSettings(geminiApiKey, geminiModel, geminiPrompt) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
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
                                Text(stringResource(R.string.settings_verify_save), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                DeveloperSection(context = context, onCheckUpdate = onCheckUpdate)

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
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OplusSectionTitle(title = stringResource(R.string.settings_auth_section))
        OplusPreferenceGroup {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = onUserNameChange,
                    label = { Text(stringResource(R.string.settings_auth_user_name)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(22.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !authSaving
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = userEmail,
                    onValueChange = onUserEmailChange,
                    label = { Text(stringResource(R.string.settings_auth_user_email)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(22.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !authSaving
                )
            }
        }
        Text(
            stringResource(R.string.settings_auth_help),
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OplusPreferenceGroup {
            Column(Modifier.padding(16.dp)) {
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
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                    enabled = !authSaving && (authMode == GitAuthMode.HTTPS || sshPrivateKey.isNotBlank()),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (authSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_save), fontWeight = FontWeight.Medium)
                    }
                }
                if (authSaved) Text(stringResource(R.string.settings_auth_saved), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
        }
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

    OplusPreferenceRow(
        title = stringResource(R.string.settings_version_fmt, versionName, versionCode),
        summary = stringResource(R.string.settings_check_updates_hint),
        icon = TablerIcons.Outline.Versions,
        onClick = onCheckUpdate
    )
}

@Composable
fun DeveloperSection(context: Context, onCheckUpdate: () -> Unit) {
    SettingsSection(title = stringResource(R.string.settings_developer_by), padded = false) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.me),
                contentDescription = stringResource(R.string.settings_developer_image_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("RHineix", style = MaterialTheme.typography.titleMedium)
                Text("@RHineix", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OplusPreferenceDivider()
        OplusPreferenceRow(
            title = stringResource(R.string.settings_social_github),
            icon = Icons.Default.Source,
            onClick = { openUrl(context, "https://github.com/RHineix") }
        )
        OplusPreferenceDivider()
        OplusPreferenceRow(
            title = stringResource(R.string.settings_social_telegram),
            icon = TablerIcons.Outline.BrandTelegram,
            onClick = { openUrl(context, "https://t.me/RHineix") }
        )
        OplusPreferenceDivider()
        AppVersionFooter(context = context, onCheckUpdate = onCheckUpdate)
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
fun SettingsSection(
    title: String,
    padded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OplusSectionTitle(title = title)
        OplusPreferenceGroup {
            if (padded) Column(Modifier.padding(16.dp), content = content)
            else content()
        }
    }
}
