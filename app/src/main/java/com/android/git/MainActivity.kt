package com.android.git

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat // ضروري جداً
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.android.git.data.PreferencesManager
import com.android.git.navigation.AppNavGraph
import com.android.git.ui.screens.PermissionScreen
import com.android.git.ui.theme.AndroidGitTheme
import com.android.git.ui.viewmodel.MainViewModel
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- FIX: تفعيل وضع Edge-to-Edge لكامل التطبيق ---
        // هذا السطر يجبر المحتوى على الامتداد خلف شريط الحالة وشريط التنقل السفلي
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // ------------------------------------------------

        setContent {
            AndroidGitTheme {
                val viewModel: MainViewModel = viewModel()
                val navController = rememberNavController()
                val prefs = remember { PreferencesManager(this) }
                
                var hasPermission by remember { mutableStateOf(checkPermission()) }

                // Auto-Open Logic
                LaunchedEffect(Unit) {
                    if (hasPermission && prefs.isAutoOpenEnabled() && viewModel.currentRepoFile == null) {
                        prefs.getLastProjectPath()?.let { path ->
                            File(path).takeIf { it.exists() }?.let { viewModel.openProject(it) }
                        }
                    }
                }

                // Lifecycle Observer
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasPermission = checkPermission()
                            if (viewModel.currentRepoFile != null) viewModel.loadDashboard()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!hasPermission) {
                        PermissionScreen { requestPermission() }
                    } else {
                        AppNavGraph(navController = navController, viewModel = viewModel)
                    }
                }
            }
        }
    }

    private fun checkPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 
        Environment.isExternalStorageManager() else true
    
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }
}