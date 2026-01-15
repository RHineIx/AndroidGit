package com.android.git.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoSelectionScreen(
    onRepoSelected: (Uri) -> Unit,
    onCloneRequest: () -> Unit,
    onGeneralSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    
    var recentProjects by remember { mutableStateOf<List<String>>(emptyList()) }

    // Load recent projects on start
    LaunchedEffect(Unit) {
        recentProjects = prefs.getRecentProjects()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags: Int = (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) { 
                // Ignore if permission persistence fails
            }
            onRepoSelected(uri)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // App Icon (Drawable resource)
                        Icon(
                            painter = painterResource(id = R.drawable.icon),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.selection_title), fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onGeneralSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Action Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionCard(
                    title = stringResource(R.string.selection_open_local),
                    icon = Icons.Default.FolderOpen,
                    modifier = Modifier.weight(1f),
                    onClick = { launcher.launch(null) }
                )
                
                ActionCard(
                    title = stringResource(R.string.selection_clone_repo),
                    icon = Icons.Default.Download,
                    modifier = Modifier.weight(1f),
                    onClick = onCloneRequest
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Recent Projects Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.selection_recent_projects),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                if (recentProjects.isNotEmpty()) {
                    TextButton(onClick = { 
                        scope.launch {
                            recentProjects.forEach { prefs.removeRecentProject(it) }
                            recentProjects = emptyList()
                        }
                    }) {
                        Text(stringResource(R.string.action_clear_all), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Recent Projects List
            if (recentProjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.selection_no_recent),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentProjects) { path ->
                        val file = File(path)
                        val exists = file.exists()
                        RecentProjectItem(
                            file = file,
                            isMissing = !exists,
                            onClick = { if (exists) onRepoSelected(Uri.fromFile(file)) },
                            onRemove = {
                                scope.launch {
                                    prefs.removeRecentProject(path)
                                    recentProjects = prefs.getRecentProjects()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(100.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun RecentProjectItem(file: File, isMissing: Boolean = false, onClick: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isMissing, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isMissing) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp),
        border = if(isMissing) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProjectIcon(projectDir = file, isMissing = isMissing)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if(isMissing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if(isMissing) stringResource(R.string.selection_dir_not_found) else file.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun ProjectIcon(projectDir: File, isMissing: Boolean) {
    var projectBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var projectTypeIcon by remember { mutableStateOf(Icons.Default.Folder) }
    var iconTint by remember { mutableStateOf(Color.Unspecified) }
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(projectDir) {
        if (!isMissing) {
            withContext(Dispatchers.IO) {
                val files = projectDir.list()?.toList() ?: emptyList()
                
                // 1. Determine Project Type
                if (files.any { it.contains("build.gradle") }) {
                    projectTypeIcon = Icons.Default.Android
                    iconTint = Color(0xFF3DDC84) // Android Green
                } else if (files.contains(".git")) {
                    projectTypeIcon = Icons.Default.Code // Represents Source/Git
                    iconTint = primaryColor
                }

                // 2. Try to load real app icon (Heuristic Search)
                val possibleIconPaths = listOf(
                    "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png",
                    "app/src/main/res/mipmap-xxhdpi/ic_launcher.png",
                    "app/src/main/res/mipmap-xhdpi/ic_launcher.png",
                    "src/main/res/mipmap-xxxhdpi/ic_launcher.png",
                    "app/src/main/res/drawable/ic_launcher.png",
                    "icon.png"
                )

                for (path in possibleIconPaths) {
                    val iconFile = File(projectDir, path)
                    if (iconFile.exists()) {
                        try {
                            projectBitmap = BitmapFactory.decodeFile(iconFile.absolutePath)
                            if (projectBitmap != null) break
                        } catch (e: Exception) {
                            // Continue searching if decoding fails
                        }
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isMissing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (projectBitmap == null) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)) else null
    ) {
        if (projectBitmap != null && !isMissing) {
            Image(
                bitmap = projectBitmap!!.asImageBitmap(),
                contentDescription = "App Icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isMissing) Icons.Default.CloudOff else projectTypeIcon,
                    contentDescription = null,
                    tint = if (isMissing) MaterialTheme.colorScheme.onSurfaceVariant else if (iconTint != Color.Unspecified) iconTint else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}