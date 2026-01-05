package com.android.git

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.android.git.data.GitManager
import com.android.git.data.PreferencesManager
import com.android.git.model.DashboardState
import com.android.git.ui.screens.*
import com.android.git.ui.theme.AndroidGitTheme
import com.android.git.utils.FileUtils
import kotlinx.coroutines.launch
import java.io.File

enum class AppScreen(val order: Int) {
    SELECTION(0),
    CLONE(1),
    GENERAL_SETTINGS(1), // New screen
    DASHBOARD(2),
    CHANGES_LIST(3),
    REPO_SETTINGS(3),    // Renamed from SETTINGS
    LOG(3),
    IGNORE_EDITOR(3),
    BRANCH_MANAGER(3),
    STASH(3)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidGitTheme {
                var hasPermission by remember { mutableStateOf(checkPermission()) }
                var selectedRepoFile by remember { mutableStateOf<File?>(null) }
                
                var currentScreen by remember { mutableStateOf(AppScreen.SELECTION) }
                
                var activeGitManager by remember { mutableStateOf<GitManager?>(null) }
                var dashboardState by remember { mutableStateOf<DashboardState>(DashboardState.Loading) }
                
                val scope = rememberCoroutineScope()
                val context = this
                val prefs = remember { PreferencesManager(context) }

                fun openProject(file: File) {
                    selectedRepoFile = file
                    activeGitManager = GitManager(file)
                    dashboardState = DashboardState.Loading
                    prefs.addRecentProject(file.absolutePath)
                    currentScreen = AppScreen.DASHBOARD
                    
                    scope.launch {
                        if (activeGitManager!!.isGitRepo()) {
                            activeGitManager!!.configureUser(prefs.getUserName(), prefs.getUserEmail())
                            activeGitManager!!.openRepo()
                            dashboardState = activeGitManager!!.getDashboardStats()
                        } else {
                            dashboardState = DashboardState.NotInitialized
                        }
                    }
                }

                fun closeProject() {
                    selectedRepoFile = null
                    activeGitManager = null
                    dashboardState = DashboardState.Loading
                    currentScreen = AppScreen.SELECTION
                }

                LaunchedEffect(Unit) {
                    if (hasPermission && prefs.isAutoOpenEnabled()) {
                        val lastPath = prefs.getLastProjectPath()
                        if (lastPath != null) {
                            val file = File(lastPath)
                            if (file.exists()) openProject(file)
                        }
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasPermission = checkPermission()
                            if (currentScreen == AppScreen.DASHBOARD && activeGitManager != null) {
                                scope.launch {
                                    if (activeGitManager!!.isGitRepo()) {
                                        dashboardState = activeGitManager!!.getDashboardStats()
                                    }
                                }
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!hasPermission) {
                        PermissionScreen { requestPermission() }
                    } else {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                if (targetState.order > initialState.order) {
                                    slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut()
                                } else {
                                    slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut()
                                }
                            },
                            label = "ScreenTransition"
                        ) { targetScreen ->
                            when (targetScreen) {
                                AppScreen.SELECTION -> {
                                    RepoSelectionScreen(
                                        onRepoSelected = { uri ->
                                            val file = FileUtils.getFileFromUri(context, uri)
                                            if (file != null) openProject(file)
                                        },
                                        onCloneRequest = { currentScreen = AppScreen.CLONE },
                                        onGeneralSettingsClick = { currentScreen = AppScreen.GENERAL_SETTINGS }
                                    )
                                }
                                AppScreen.GENERAL_SETTINGS -> {
                                    GeneralSettingsScreen(onBack = { currentScreen = AppScreen.SELECTION })
                                }
                                AppScreen.CLONE -> {
                                    CloneScreen(
                                        onBack = { currentScreen = AppScreen.SELECTION },
                                        onCloneSuccess = { newRepoDir -> openProject(newRepoDir) }
                                    )
                                }
                                AppScreen.DASHBOARD -> {
                                    if (selectedRepoFile != null && activeGitManager != null) {
                                        DashboardScreen(
                                            repoFile = selectedRepoFile!!,
                                            gitManager = activeGitManager!!,
                                            dashboardState = dashboardState,
                                            onRefresh = { scope.launch { dashboardState = activeGitManager!!.getDashboardStats() } },
                                            onViewChanges = { currentScreen = AppScreen.CHANGES_LIST },
                                            onSettings = { currentScreen = AppScreen.REPO_SETTINGS },
                                            onViewLog = { currentScreen = AppScreen.LOG },
                                            onManageBranches = { currentScreen = AppScreen.BRANCH_MANAGER },
                                            onOpenStash = { currentScreen = AppScreen.STASH },
                                            onIgnoreEditor = { currentScreen = AppScreen.IGNORE_EDITOR },
                                            onCloseProject = { closeProject() }
                                        )
                                    } else LaunchedEffect(Unit) { currentScreen = AppScreen.SELECTION }
                                }
                                AppScreen.REPO_SETTINGS -> {
                                    if (selectedRepoFile != null) RepoSettingsScreen(repoFile = selectedRepoFile!!, onBack = { currentScreen = AppScreen.DASHBOARD })
                                }
                                AppScreen.STASH -> {
                                    if (selectedRepoFile != null) {
                                        StashScreen(repoFile = selectedRepoFile!!, onBack = { currentScreen = AppScreen.DASHBOARD })
                                    }
                                }
                                AppScreen.BRANCH_MANAGER -> {
                                    if (selectedRepoFile != null) {
                                        BranchManagerScreen(
                                            repoFile = selectedRepoFile!!,
                                            onBack = { 
                                                scope.launch { if(activeGitManager != null) dashboardState = activeGitManager!!.getDashboardStats() }
                                                currentScreen = AppScreen.DASHBOARD 
                                            }
                                        )
                                    }
                                }
                                AppScreen.CHANGES_LIST -> {
                                    if (selectedRepoFile != null) ChangesScreen(repoFile = selectedRepoFile!!, onBack = { scope.launch { if(activeGitManager != null) dashboardState = activeGitManager!!.getDashboardStats() }; currentScreen = AppScreen.DASHBOARD })
                                }
                                AppScreen.LOG -> {
                                    if (selectedRepoFile != null) LogScreen(repoFile = selectedRepoFile!!, onBack = { currentScreen = AppScreen.DASHBOARD })
                                }
                                AppScreen.IGNORE_EDITOR -> {
                                    if (selectedRepoFile != null) IgnoreEditorScreen(repoFile = selectedRepoFile!!, onBack = { currentScreen = AppScreen.DASHBOARD })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true

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