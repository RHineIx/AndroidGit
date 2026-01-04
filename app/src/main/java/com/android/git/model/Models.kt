package com.android.git.model

import java.util.Date

// File Status Enum
enum class ChangeType {
    ADDED, MODIFIED, DELETED, UNTRACKED, MISSING
}

// Represents a single file change
data class GitFile(
    val path: String,
    val type: ChangeType
)

// Represents a commit in history
data class CommitItem(
    val message: String,
    val author: String,
    val date: Date,
    val hash: String
)

// Represents the Dashboard UI State
sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(
        val branch: String, 
        val changes: Int, 
        val unpushedCount: Int
    ) : DashboardState()
    data class Error(val message: String) : DashboardState()
    object NotInitialized : DashboardState()
}