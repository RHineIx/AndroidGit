package com.android.git.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("git_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "github_token"
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String {
        return prefs.getString(KEY_TOKEN, "") ?: ""
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }
}