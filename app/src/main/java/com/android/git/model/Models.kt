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
        val unpushedCount: Int
    ) : DashboardState()
    data class Error(val message: String) : DashboardState()
    object NotInitialized : DashboardState()
}

enum class BranchType {
    LOCAL, REMOTE
}

data class BranchModel(
    /** Human-friendly branch name without the refs/heads or refs/remotes prefix. */
    val name: String,
    /** Canonical ref used for Git operations. Never use [name] as an operation identifier. */
    val fullPath: String,
    val type: BranchType,
    val isCurrent: Boolean,
    /** Remote name for remote refs, or null for local refs. */
    val remoteName: String? = null,
    /** Configured upstream display name for a local branch. */
    val trackingName: String? = null,
    /** Number of commits ahead of upstream, when an upstream is available. */
    val aheadCount: Int = 0,
    /** Number of commits behind upstream, when an upstream is available. */
    val behindCount: Int = 0,
    /** True when the configured upstream no longer exists locally. */
    val isUpstreamGone: Boolean = false
)

// [New] Model for Update System
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val releaseNotes: String,
    val downloadUrl: String,
    val isMandatory: Boolean = false
)