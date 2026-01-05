package com.android.git.model

import java.util.Date

enum class ChangeType {
    ADDED, MODIFIED, DELETED, UNTRACKED, MISSING
}

data class GitFile(
    val path: String,
    val type: ChangeType
)

data class CommitItem(
    val message: String,
    val author: String,
    val date: Date,
    val hash: String,
    val isPushed: Boolean
)

data class StashItem(
    val index: Int,
    val message: String,
    val hash: String
)

sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(
        val branch: String, 
        val changes: Int, 
        val unpushedCount: Int,
        val conflictCount: Int // New Field
    ) : DashboardState()
    data class Error(val message: String) : DashboardState()
    object NotInitialized : DashboardState()
}

enum class BranchType {
    LOCAL, REMOTE
}

data class BranchModel(
    val name: String,
    val fullPath: String,
    val type: BranchType,
    val isCurrent: Boolean
)