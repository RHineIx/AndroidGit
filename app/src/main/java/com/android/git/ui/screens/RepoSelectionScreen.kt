package com.android.git.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.sp
import com.android.git.R
import com.android.git.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Miuix Library Imports for interactive cards
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.utils.PressFeedbackType

// Global in-memory cache for project icons to prevent scroll jank
object ProjectImageCache {
    val memoryCache = LruCache<String, Bitmap>(30) // Cache up to 30 decoded icons
}

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

    LaunchedEffect(Unit) {
        recentProjects = prefs.getRecentProjects()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags: Int = (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {
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
                        Icon(
                            painter = painterResource(id = R.drawable.icon),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.selection_title), fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onGeneralSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.selection_recent_projects),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                if (recentProjects.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.action_clear_all),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                scope.launch {
                                    recentProjects.forEach { prefs.removeRecentProject(it) }
                                    recentProjects = emptyList()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                            text = stringResource(R.string.selection_no_recent),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentProjects, key = { it }) { path ->
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
        modifier = modifier.height(100.dp),
        insideMargin = PaddingValues(16.dp),
        colors = CardColors(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        pressFeedbackType = PressFeedbackType.Tilt,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RecentProjectItem(file: File, isMissing: Boolean = false, onClick: () -> Unit, onRemove: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isMissing) 0.6f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isMissing, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isMissing) 0.dp else 2.dp)
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if(isMissing) stringResource(R.string.selection_dir_not_found) else file.absolutePath,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProjectIcon(projectDir: File, isMissing: Boolean) {
    val pathKey = projectDir.absolutePath
    var projectBitmap by remember(pathKey) { mutableStateOf(ProjectImageCache.memoryCache.get(pathKey)) }
    var projectTypeIcon by remember { mutableStateOf(Icons.Default.Folder) }
    var iconTint by remember { mutableStateOf(Color.Unspecified) }
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(pathKey) {
        if (!isMissing && projectBitmap == null) {
            withContext(Dispatchers.IO) {
                val fileNames = projectDir.list()?.toList() ?: emptyList()

                // Determine Project Type
                if (fileNames.contains("pubspec.yaml")) {
                    projectTypeIcon = Icons.Default.Smartphone
                    iconTint = Color(0xFF02569B) // Flutter Blue
                } else if (fileNames.any { it.contains("build.gradle") }) {
                    projectTypeIcon = Icons.Default.Android
                    iconTint = Color(0xFF3DDC84) // Android Green
                } else if (fileNames.contains("package.json")) {
                    projectTypeIcon = Icons.Default.Language
                    iconTint = Color(0xFFF7DF1E) // JS Yellow
                } else if (fileNames.contains(".git")) {
                    projectTypeIcon = Icons.Default.Code
                    iconTint = primaryColor
                }

                // Search for an actual project icon
                val extensions = listOf("webp", "png", "jpg", "jpeg", "ico")
                val androidBaseDirs = listOf("app/src/main/res", "android/app/src/main/res", "src/main/res")
                val densities = listOf("mipmap-xxxhdpi", "mipmap-xxhdpi", "mipmap-xhdpi", "drawable-xxxhdpi", "drawable-xxhdpi", "drawable")
                val iconNames = listOf("ic_launcher", "ic_launcher_round", "app_icon")

                val searchPaths = mutableListOf<String>()

                for (base in androidBaseDirs) {
                    for (density in densities) {
                        for (name in iconNames) {
                            for (ext in extensions) {
                                searchPaths.add("$base/$density/$name.$ext")
                            }
                        }
                    }
                }

                val storeIconNames = listOf("ic_launcher-playstore", "playstore-icon")
                val storeBaseDirs = listOf("app/src/main", "android/app/src/main", "src/main", ".")

                for (base in storeBaseDirs) {
                    for (name in storeIconNames) {
                        for (ext in extensions) {
                            val path = if(base == ".") "$name.$ext" else "$base/$name.$ext"
                            searchPaths.add(path)
                        }
                    }
                }

                val rootIconNames = listOf("icon", "logo", "favicon", "assets/icon/icon", "assets/logo")
                for (name in rootIconNames) {
                    for (ext in extensions) {
                        searchPaths.add("$name.$ext")
                    }
                }

                for (path in searchPaths) {
                    val iconFile = File(projectDir, path)
                    if (iconFile.exists()) {
                        try {
                            val decoded = BitmapFactory.decodeFile(iconFile.absolutePath)
                            if (decoded != null) {
                                ProjectImageCache.memoryCache.put(pathKey, decoded)
                                projectBitmap = decoded
                                break
                            }
                        } catch (_: Exception) {
                            // Ignore specific decoding errors and continue searching
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isMissing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (projectBitmap != null && !isMissing) {
            Image(
                bitmap = projectBitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = if (isMissing) Icons.Default.CloudOff else projectTypeIcon,
                contentDescription = null,
                tint = if (isMissing) MaterialTheme.colorScheme.onSurfaceVariant else if (iconTint != Color.Unspecified) iconTint else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}