package com.android.git.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.git.ui.screens.*
import com.android.git.ui.viewmodel.MainViewModel
import com.android.git.utils.FileUtils
import java.net.URLDecoder

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: MainViewModel = viewModel(),
    startDestination: String = Screen.Selection.route
) {
    val context = LocalContext.current
    val manager = viewModel.gitManager

    // Handle redirection logic
    LaunchedEffect(viewModel.currentRepoFile) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        
        if (viewModel.currentRepoFile != null && currentRoute == Screen.Selection.route) {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Selection.route) { inclusive = true }
            }
        } else if (viewModel.currentRepoFile == null) {
            if (currentRoute != Screen.Clone.route && currentRoute != Screen.GeneralSettings.route) {
                navController.navigate(Screen.Selection.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally { it } + fadeIn() },
        exitTransition = { slideOutHorizontally { -it } + fadeOut() },
        popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
        popExitTransition = { slideOutHorizontally { it } + fadeOut() }
    ) {
        
        composable(Screen.Selection.route) {
            RepoSelectionScreen(
                onRepoSelected = { uri ->
                    FileUtils.getFileFromUri(uri)?.let { viewModel.openProject(it) }
                },
                onCloneRequest = { navController.navigate(Screen.Clone.route) },
                onGeneralSettingsClick = { navController.navigate(Screen.GeneralSettings.route) }
            )
        }

        composable(Screen.Clone.route) {
            CloneScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCloneSuccess = { file ->
                    viewModel.openProject(file)
                }
            )
        }

        composable(Screen.GeneralSettings.route) {
            GeneralSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            if (manager != null && viewModel.currentRepoFile != null) {
                DashboardScreen(
                    repoFile = viewModel.currentRepoFile!!,
                    gitManager = manager,
                    viewModel = viewModel,
                    dashboardState = viewModel.dashboardState,
                    onRefresh = { viewModel.loadDashboard() },
                    onViewChanges = { navController.navigate(Screen.ChangesList.route) },
                    onSettings = { navController.navigate(Screen.RepoSettings.route) },
                    onViewLog = { navController.navigate(Screen.Log.route) },
                    onManageBranches = { navController.navigate(Screen.BranchManager.route) },
                    onOpenStash = { navController.navigate(Screen.Stash.route) },
                    onIgnoreEditor = { navController.navigate(Screen.IgnoreEditor.route) },
                    onCloseProject = { viewModel.closeProject() },
                    onMergeConflicts = { navController.navigate(Screen.MergeConflicts.route) }
                )
            } else {
                LaunchedEffect(Unit) { navController.navigate(Screen.Selection.route) }
            }
        }

        composable(Screen.ChangesList.route) {
            manager?.let {
                ChangesScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.loadDashboard()
                        navController.popBackStack()
                    },
                    // NEW: Navigate to Diff Viewer
                    onViewDiff = { path ->
                        navController.navigate(Screen.DiffViewer.createRoute(path))
                    }
                )
            }
        }

        // NEW: Diff Viewer Composable
        composable(
            route = Screen.DiffViewer.route,
            arguments = listOf(navArgument("filePath") { type = NavType.StringType })
        ) { backStackEntry ->
            manager?.let { mgr ->
                val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                val decodedPath = URLDecoder.decode(encodedPath, "UTF-8")
                
                DiffViewerScreen(
                    gitManager = mgr,
                    filePath = decodedPath,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.BranchManager.route) {
            manager?.let {
                BranchManagerScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.loadDashboard()
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Screen.RepoSettings.route) {
            manager?.let {
                RepoSettingsScreen(
                    gitManager = it,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Log.route) {
            manager?.let {
                LogScreen(
                    gitManager = it,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Stash.route) {
            manager?.let {
                StashScreen(
                    gitManager = it,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.IgnoreEditor.route) {
            manager?.let {
                IgnoreEditorScreen(
                    gitManager = it,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.MergeConflicts.route) {
            manager?.let {
                MergeConflictScreen(
                    gitManager = it,
                    onBack = {
                        viewModel.loadDashboard()
                        navController.popBackStack()
                    },
                    onResolveFile = { path ->
                        navController.navigate(Screen.ConflictResolver.createRoute(path))
                    }
                )
            }
        }

        composable(
            route = Screen.ConflictResolver.route,
            arguments = listOf(navArgument("filePath") { type = NavType.StringType })
        ) { backStackEntry ->
            manager?.let { mgr ->
                val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                val decodedPath = URLDecoder.decode(encodedPath, "UTF-8")
                
                ConflictResolverScreen(
                    gitManager = mgr,
                    filePath = decodedPath,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}