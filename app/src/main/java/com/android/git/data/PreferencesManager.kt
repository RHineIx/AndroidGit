package com.android.git.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class PreferencesManager(context: Context) {
    
    // Use EncryptedSharedPreferences to securely store sensitive data (Tokens & API Keys)
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "git_secure_prefs", // Switched namespace to ensure clean secure state
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_TOKEN = "github_token"
        private const val KEY_LAST_VALID_TOKEN = "last_valid_github_token"
        private const val KEY_AUTH_MODE = "git_auth_mode"
        private const val KEY_SSH_PRIVATE_KEY = "ssh_private_key"
        private const val KEY_SSH_PUBLIC_KEY = "ssh_public_key"
        private const val KEY_SSH_PASSPHRASE = "ssh_passphrase"
        private const val KEY_USERNAME = "git_username"
        private const val KEY_EMAIL = "git_email"
        private const val KEY_THEME_DARK = "app_theme_dark"

        private const val KEY_AUTO_OPEN = "auto_open_last_project"
        private const val KEY_LAST_PROJECT_PATH = "last_project_path"
        private const val KEY_RECENT_PROJECTS = "recent_projects_list"

        private const val KEY_THEME_MODE = "theme_mode"

        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_GEMINI_MODEL = "gemini_model"
        private const val KEY_GEMINI_PROMPT = "gemini_prompt"
    }

    fun saveToken(token: String) {
        val editor = prefs.edit()
        editor.putString(KEY_TOKEN, token)
        if (token.isNotEmpty()) {
            editor.putString(KEY_LAST_VALID_TOKEN, token)
        }
        editor.apply()
    }

    fun getToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""

    fun clearToken() = prefs.edit().remove(KEY_TOKEN).apply()

    fun getLastValidToken(): String = prefs.getString(KEY_LAST_VALID_TOKEN, "") ?: ""

    fun restoreLastToken(): String {
        val lastToken = getLastValidToken()
        if (lastToken.isNotEmpty()) {
            saveToken(lastToken)
        }
        return lastToken
    }

    fun getAuthMode(): GitAuthMode {
        return runCatching { GitAuthMode.valueOf(prefs.getString(KEY_AUTH_MODE, GitAuthMode.HTTPS.name) ?: GitAuthMode.HTTPS.name) }
            .getOrDefault(GitAuthMode.HTTPS)
    }

    fun setAuthMode(mode: GitAuthMode) {
        prefs.edit().putString(KEY_AUTH_MODE, mode.name).apply()
    }

    fun saveSshKey(privateKey: String, publicKey: String, passphrase: String) {
        prefs.edit()
            .putString(KEY_SSH_PRIVATE_KEY, privateKey)
            .putString(KEY_SSH_PUBLIC_KEY, publicKey)
            .putString(KEY_SSH_PASSPHRASE, passphrase)
            .apply()
    }

    fun getSshPrivateKey(): String = prefs.getString(KEY_SSH_PRIVATE_KEY, "") ?: ""
    fun getSshPublicKey(): String = prefs.getString(KEY_SSH_PUBLIC_KEY, "") ?: ""
    fun getSshPassphrase(): String = prefs.getString(KEY_SSH_PASSPHRASE, "") ?: ""

    fun clearSshKey() {
        prefs.edit()
            .remove(KEY_SSH_PRIVATE_KEY)
            .remove(KEY_SSH_PUBLIC_KEY)
            .remove(KEY_SSH_PASSPHRASE)
            .putString(KEY_AUTH_MODE, GitAuthMode.HTTPS.name)
            .apply()
    }

    fun saveGitIdentity(name: String, email: String) {
        prefs.edit()
            .putString(KEY_USERNAME, name)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun setUserName(name: String) = prefs.edit().putString(KEY_USERNAME, name).apply()
    fun setUserEmail(email: String) = prefs.edit().putString(KEY_EMAIL, email).apply()

    fun getUserName(): String = prefs.getString(KEY_USERNAME, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""

    fun isAutoOpenEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_OPEN, false)
    fun setAutoOpenEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_OPEN, enabled).apply()

    fun getLastProjectPath(): String? = prefs.getString(KEY_LAST_PROJECT_PATH, null)

    fun setLastProjectPath(path: String) = prefs.edit().putString(KEY_LAST_PROJECT_PATH, path).apply()

    fun addRecentProject(path: String) {
        prefs.edit().putString(KEY_LAST_PROJECT_PATH, path).apply()

        val currentListString = prefs.getString(KEY_RECENT_PROJECTS, "") ?: ""
        val currentList = if (currentListString.isEmpty()) mutableListOf() else currentListString.split("|").toMutableList()

        currentList.remove(path)
        currentList.add(0, path)
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
        if (getLastProjectPath() == path) {
            prefs.edit().remove(KEY_LAST_PROJECT_PATH).apply()
        }
    }

    fun getThemeMode(): ThemeMode {
        val modeOrdinal = prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)
        return ThemeMode.entries.getOrElse(modeOrdinal) { ThemeMode.SYSTEM }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
    }

    fun getGeminiApiKey(): String = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    fun setGeminiApiKey(key: String) = prefs.edit().putString(KEY_GEMINI_API_KEY, key).apply()

    fun getGeminiModel(): String = prefs.getString(KEY_GEMINI_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash"
    fun setGeminiModel(model: String) = prefs.edit().putString(KEY_GEMINI_MODEL, model).apply()

    fun getGeminiPrompt(): String = prefs.getString(KEY_GEMINI_PROMPT, "") ?: ""
    fun setGeminiPrompt(prompt: String) = prefs.edit().putString(KEY_GEMINI_PROMPT, prompt).apply()

}