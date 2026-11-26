package com.android.git.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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
    val manager = viewModel.gitManager

    // State-driven routing: Navigate automatically based on repository state
    LaunchedEffect(viewModel.currentRepoFile) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route ?: return@LaunchedEffect
        
        if (viewModel.currentRepoFile != null) {
            if (currentRoute == Screen.Selection.route || currentRoute == Screen.Clone.route) {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else {
            if (currentRoute != Screen.Selection.route && currentRoute != Screen.Clone.route && currentRoute != Screen.GeneralSettings.route) {
                navController.navigate(Screen.Selection.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
    ) {
        
        composable(Screen.Selection.route) {
            RepoSelectionScreen(
                onRepoSelected = { uri ->
                    FileUtils.getFileFromUri(uri)?.let { viewModel.openProject(it) }
                },
                onCloneRequest = { 
                    navController.navigate(Screen.Clone.route) { launchSingleTop = true }
                },
                onGeneralSettingsClick = { 
                    navController.navigate(Screen.GeneralSettings.route) { launchSingleTop = true }
                }
            )
        }

        composable(Screen.Clone.route) {
            CloneScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCloneSuccess = { file -> viewModel.openProject(file) }
            )
        }

        composable(Screen.GeneralSettings.route) {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCheckUpdate = { viewModel.checkForUpdates(isManual = true) }
            )
        }

        composable(Screen.Workflows.route) {
            WorkflowsScreen(
                viewModel = viewModel,
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
                    onViewChanges = { navController.navigate(Screen.ChangesList.route) { launchSingleTop = true } },
                    onSettings = { navController.navigate(Screen.RepoSettings.route) { launchSingleTop = true } },
                    onViewLog = { navController.navigate(Screen.Log.route) { launchSingleTop = true } },
                    onManageBranches = { navController.navigate(Screen.BranchManager.route) { launchSingleTop = true } },
                    onOpenStash = { navController.navigate(Screen.Stash.route) { launchSingleTop = true } },
                    onIgnoreEditor = { navController.navigate(Screen.IgnoreEditor.route) { launchSingleTop = true } },
                    onCloseProject = { viewModel.closeProject() },
                    onMergeConflicts = { navController.navigate(Screen.MergeConflicts.route) { launchSingleTop = true } },
                    onOpenWorkflows = { navController.navigate(Screen.Workflows.route) { launchSingleTop = true } }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        }

        composable(Screen.ChangesList.route) {
            manager?.let {
                ChangesScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.loadDashboard()
                        navController.popBackStack()
                    }
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
                    viewModel = viewModel,
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
                        navController.navigate(Screen.ConflictResolver.createRoute(path)) { launchSingleTop = true }
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
