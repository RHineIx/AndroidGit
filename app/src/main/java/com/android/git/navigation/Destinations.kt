package com.android.git.navigation

/**
 * Type-safe definitions for navigation routes.
 * Instead of raw strings, we use these objects throughout the app.
 */
sealed class Screen(val route: String) {
    data object Selection : Screen("selection")
    data object Clone : Screen("clone")
    data object GeneralSettings : Screen("general_settings")
    data object Dashboard : Screen("dashboard")
    data object ChangesList : Screen("changes_list")
    data object RepoSettings : Screen("repo_settings")
    data object Log : Screen("log")
    data object IgnoreEditor : Screen("ignore_editor")
    data object BranchManager : Screen("branch_manager")
    data object Stash : Screen("stash")
    data object MergeConflicts : Screen("merge_conflicts")
    
    // Route with arguments: "conflict_resolver/{encoded_path}"
    data object ConflictResolver : Screen("conflict_resolver/{filePath}") {
        fun createRoute(filePath: String): String {
            val encoded = java.net.URLEncoder.encode(filePath, "UTF-8")
            return "conflict_resolver/$encoded"
        }
    }

    // NEW: Diff Viewer Route
    data object DiffViewer : Screen("diff_viewer/{filePath}") {
        fun createRoute(filePath: String): String {
            val encoded = java.net.URLEncoder.encode(filePath, "UTF-8")
            return "diff_viewer/$encoded"
        }
    }
}