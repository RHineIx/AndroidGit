package com.android.git

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.android.git.ui.screens.*
import com.android.git.ui.theme.AndroidGitTheme
import com.android.git.utils.FileUtils
import java.io.File

enum class AppScreen {
    SELECTION, DASHBOARD, CHANGES_LIST, SYNC, LOG, CLONE, IGNORE_EDITOR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidGitTheme {
                var hasPermission by remember { mutableStateOf(checkPermission()) }
                var selectedRepoFile by remember { mutableStateOf<File?>(null) }
                
                var currentScreen by remember { mutableStateOf(AppScreen.SELECTION) }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasPermission = checkPermission()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!hasPermission) {
                        PermissionScreen { requestPermission() }
                    } else {
                        when (currentScreen) {
                            AppScreen.SELECTION -> {
                                RepoSelectionScreen(
                                    onRepoSelected = { uri ->
                                        val file = FileUtils.getFileFromUri(this, uri)
                                        if (file != null) {
                                            selectedRepoFile = file
                                            currentScreen = AppScreen.DASHBOARD
                                        }
                                    },
                                    onCloneRequest = {
                                        currentScreen = AppScreen.CLONE
                                    }
                                )
                            }
                            AppScreen.CLONE -> {
                                CloneScreen(
                                    onBack = {
                                        currentScreen = AppScreen.SELECTION
                                    },
                                    onCloneSuccess = { newRepoDir ->
                                        selectedRepoFile = newRepoDir
                                        currentScreen = AppScreen.DASHBOARD
                                    }
                                )
                            }
                            AppScreen.DASHBOARD -> {
                                if (selectedRepoFile != null) {
                                    DashboardScreen(
                                        repoFile = selectedRepoFile!!,
                                        onViewChanges = {
                                            currentScreen = AppScreen.CHANGES_LIST
                                        },
                                        onSync = {
                                            currentScreen = AppScreen.SYNC
                                        },
                                        onViewLog = {
                                            currentScreen = AppScreen.LOG
                                        },
                                        onIgnoreEditor = {
                                            currentScreen = AppScreen.IGNORE_EDITOR
                                        },
                                        onCloseProject = {
                                            selectedRepoFile = null
                                            currentScreen = AppScreen.SELECTION
                                        }
                                    )
                                } else {
                                    currentScreen = AppScreen.SELECTION
                                }
                            }
                            AppScreen.CHANGES_LIST -> {
                                if (selectedRepoFile != null) {
                                    ChangesScreen(
                                        repoFile = selectedRepoFile!!,
                                        onBack = {
                                            currentScreen = AppScreen.DASHBOARD
                                        }
                                    )
                                }
                            }
                            AppScreen.SYNC -> {
                                if (selectedRepoFile != null) {
                                    SyncScreen(
                                        repoFile = selectedRepoFile!!,
                                        onBack = {
                                            currentScreen = AppScreen.DASHBOARD
                                        }
                                    )
                                }
                            }
                            AppScreen.LOG -> {
                                if (selectedRepoFile != null) {
                                    LogScreen(
                                        repoFile = selectedRepoFile!!,
                                        onBack = {
                                            currentScreen = AppScreen.DASHBOARD
                                        }
                                    )
                                }
                            }
                            AppScreen.IGNORE_EDITOR -> {
                                if (selectedRepoFile != null) {
                                    IgnoreEditorScreen(
                                        repoFile = selectedRepoFile!!,
                                        onBack = {
                                            currentScreen = AppScreen.DASHBOARD
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", packageName))
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Required Permission", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "To manage Git repositories (and read .git folders), this app needs full file access.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRequest) {
            Text("Grant Permission")
        }
    }
}