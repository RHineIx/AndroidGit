package com.android.git

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AndroidGitTheme {
                MainAppContent(
                    checkPermission = { checkPermission() },
                    requestPermission = { requestPermission() }
                )
            }
        }
    }

    private fun checkPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // [Refactor] Used KTX extension .toUri() for cleaner syntax
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, "package:$packageName".toUri()))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: MainViewModel = viewModel(),
    checkPermission: () -> Boolean,
    requestPermission: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val prefs = remember { PreferencesManager(context) }

    var hasPermission by remember { mutableStateOf(checkPermission()) }

    LaunchedEffect(Unit) {
        if (hasPermission && prefs.isAutoOpenEnabled() && viewModel.currentRepoFile == null) {
            prefs.getLastProjectPath()?.let { path ->
                File(path).takeIf { it.exists() }?.let { viewModel.openProject(it) }
            }
        }
    }

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