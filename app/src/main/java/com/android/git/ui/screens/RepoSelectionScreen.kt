package com.android.git.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onGeneralSettingsClick: () -> Unit // Callback added
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    
    var recentProjects by remember { mutableStateOf<List<String>>(emptyList()) }

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
            } catch (e: Exception) { }
            
            onRepoSelected(uri)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("AndroidGit", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onGeneralSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "General Settings")
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionCard(
                    title = "Open Local",
                    icon = Icons.Default.FolderOpen,
                    modifier = Modifier.weight(1f),
                    onClick = { launcher.launch(null) }
                )
                
                ActionCard(
                    title = "Clone Repo",
                    icon = Icons.Default.Download,
                    modifier = Modifier.weight(1f),
                    onClick = onCloneRequest
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Projects",
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
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (recentProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No recent projects",
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
                        if (file.exists()) {
                            RecentProjectItem(
                                file = file,
                                onClick = { onRepoSelected(Uri.fromFile(file)) },
                                onRemove = {
                                    scope.launch {
                                        prefs.removeRecentProject(path)
                                        recentProjects = prefs.getRecentProjects()
                                    }
                                }
                            )
                        } else {
                            RecentProjectItem(
                                file = file,
                                isMissing = true,
                                onClick = { },
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
}

@Composable
fun ActionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
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
fun RecentProjectItem(
    file: File,
    isMissing: Boolean = false,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isMissing, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isMissing) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp),
        border = if(isMissing) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
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
                    text = if(isMissing) "Directory not found" else file.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun ProjectIcon(projectDir: File, isMissing: Boolean) {
    var projectBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var projectTypeIcon by remember { mutableStateOf(Icons.Default.Folder) }
    var iconTint by remember { mutableStateOf(Color.Unspecified) }

    LaunchedEffect(projectDir) {
        if (isMissing) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val iconPaths = listOf(
                "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png",
                "app/src/main/res/mipmap-xxhdpi/ic_launcher.png",
                "app/src/main/res/mipmap-xhdpi/ic_launcher.png",
                "src/main/res/mipmap-xxxhdpi/ic_launcher.png"
            )
            
            var foundBitmap: Bitmap? = null
            for (path in iconPaths) {
                val iconFile = File(projectDir, path)
                if (iconFile.exists()) {
                    try {
                        foundBitmap = BitmapFactory.decodeFile(iconFile.absolutePath)
                        break
                    } catch (e: Exception) { }
                }
            }

            if (foundBitmap != null) {
                projectBitmap = foundBitmap
            } else {
                val files = projectDir.list()?.toList() ?: emptyList()
                
                when {
                    files.contains("index.html") || files.contains("package.json") -> {
                        projectTypeIcon = Icons.Default.Language
                        iconTint = Color(0xFFE65100)
                    }
                    files.contains("pubspec.yaml") -> {
                        projectTypeIcon = Icons.Default.Smartphone
                        iconTint = Color(0xFF0277BD)
                    }
                    files.contains("build.gradle") || files.contains("build.gradle.kts") -> {
                        projectTypeIcon = Icons.Default.Android
                        iconTint = Color(0xFF2E7D32)
                    }
                    files.contains("pom.xml") -> {
                        projectTypeIcon = Icons.Default.Code
                        iconTint = Color(0xFFC62828)
                    }
                    files.contains("requirements.txt") || files.any { it.endsWith(".py") } -> {
                        projectTypeIcon = Icons.Default.Terminal
                        iconTint = Color(0xFFF9A825)
                    }
                    else -> {
                        projectTypeIcon = Icons.Default.Folder
                        iconTint = Color.Gray
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isMissing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (projectBitmap == null) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)) else null
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
                if (isMissing) {
                    Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Icon(
                        imageVector = projectTypeIcon,
                        contentDescription = "Project Type",
                        tint = if (iconTint != Color.Unspecified) iconTint else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

val androidx.compose.material.icons.Icons.Filled.Download: ImageVector
    get() = androidx.compose.material.icons.Icons.Filled.CloudDownload