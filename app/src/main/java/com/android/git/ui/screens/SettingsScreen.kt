package com.android.git.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

    // Config State
    var userName by remember { mutableStateOf(prefs.getUserName()) }
    var userEmail by remember { mutableStateOf(prefs.getUserEmail()) }
    var token by remember { mutableStateOf(prefs.getToken()) }
    var autoOpen by remember { mutableStateOf(prefs.isAutoOpenEnabled()) }
    
    // Remote State
    var remoteUrl by remember { mutableStateOf("") }
    var isRemoteLinked by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf("") }
    var statusType by remember { mutableStateOf(SnackbarType.INFO) }

    BackHandler(enabled = true) { onBack() }

    LaunchedEffect(Unit) {
        // Fetch real URL
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
                            Text("Auto-open last project", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Reopen repository on launch.",
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

                // --- SECTION 2: Identity & Auth ---
                SettingsSection(title = "User & Authentication") {
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

                // --- SECTION 3: Remote Origin ---
                SettingsSection(title = "Remote Repository") {
                    
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
                            
                            statusMessage = "All Settings Saved!"
                            statusType = SnackbarType.SUCCESS
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Save All Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                // --- SECTION 4: Developer Info ---
                DeveloperSection(context)
                
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

// --- Developer Section ---
@Composable
fun DeveloperSection(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Developed by",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Avatar
            Surface(
                modifier = Modifier.size(80.dp), // Slightly larger for better image view
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface, // Neutral background for the image
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                // Using Image instead of Icon for JPEG/PNG
                Image(
                    painter = painterResource(id = R.drawable.me), // Assumes me.jpeg exists in drawable
                    contentDescription = "RHineix Developer",
                    contentScale = ContentScale.Crop, // Crop to fill the circle
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "RHine",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "@RHineix",
                style = MaterialTheme.typography.titleMedium, // Slightly bigger for handle
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SocialButton(
                    iconRes = R.drawable.ic_github, 
                    label = "GitHub",
                    onClick = { openUrl(context, "https://github.com/RHineix") }
                )
                
                Spacer(Modifier.width(16.dp))
                
                SocialButton(
                    iconRes = R.drawable.ic_telegram, 
                    label = "Telegram",
                    onClick = { openUrl(context, "https://t.me/RHineix") }
                )
            }
        }
    }
}

@Composable
fun SocialButton(iconRes: Int, label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), // More vertical padding
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface // Ensure icons are visible
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
    } catch (e: Exception) {
    }
}

// --- Components ---

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
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