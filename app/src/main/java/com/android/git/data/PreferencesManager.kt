package com.android.git.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("git_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "github_token"
        private const val KEY_USERNAME = "git_username"
        private const val KEY_EMAIL = "git_email"
        private const val KEY_THEME_DARK = "app_theme_dark"
        
        // New Keys
        private const val KEY_AUTO_OPEN = "auto_open_last_project"
        private const val KEY_LAST_PROJECT_PATH = "last_project_path"
        private const val KEY_RECENT_PROJECTS = "recent_projects_list" // Stored as path|path|path
    }

    // --- Git Config ---
    fun saveToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()
    fun getToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""
    fun clearToken() = prefs.edit().remove(KEY_TOKEN).apply()

    fun saveGitIdentity(name: String, email: String) {
        prefs.edit()
            .putString(KEY_USERNAME, name)
            .putString(KEY_EMAIL, email)
            .apply()
    }
    
    fun getUserName(): String = prefs.getString(KEY_USERNAME, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""

    // --- App Preferences ---
    
    fun isAutoOpenEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_OPEN, false)
    fun setAutoOpenEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_OPEN, enabled).apply()

    fun getLastProjectPath(): String? = prefs.getString(KEY_LAST_PROJECT_PATH, null)
    
    // Logic to add a project to recent list (and set as last opened)
    fun addRecentProject(path: String) {
        // 1. Set as last opened
        prefs.edit().putString(KEY_LAST_PROJECT_PATH, path).apply()

        // 2. Add to recent list (Max 5, Unique)
        val currentListString = prefs.getString(KEY_RECENT_PROJECTS, "") ?: ""
        val currentList = if (currentListString.isEmpty()) mutableListOf() else currentListString.split("|").toMutableList()

        // Remove if exists to move it to top
        currentList.remove(path)
        // Add to top
        currentList.add(0, path)
        // Keep only top 5
        if (currentList.size > 5) {
            currentList.removeAt(currentList.size - 1)
        }

        val newListString = currentList.joinToString("|")
        prefs.edit().putString(KEY_RECENT_PROJECTS, newListString).apply()
    }

    fun getRecentProjects(): List<String> {
        val str = prefs.getString(KEY_RECENT_PROJECTS, "") ?: ""
        return if (str.isEmpty()) emptyList() else str.split("|")
    }

    fun removeRecentProject(path: String) {
        val currentListString = prefs.getString(KEY_RECENT_PROJECTS, "") ?: ""
        if (currentListString.isNotEmpty()) {
            val list = currentListString.split("|").toMutableList()
            list.remove(path)
            prefs.edit().putString(KEY_RECENT_PROJECTS, list.joinToString("|")).apply()
        }
        // If it was the last project, clear it
        if (getLastProjectPath() == path) {
            prefs.edit().remove(KEY_LAST_PROJECT_PATH).apply()
        }
    }
}