package com.android.git.utils

/**
 * Shared interpretation of the human-readable results returned by GitManager.
 * GitManager keeps its public API string-based for the existing UI, so all
 * screens must use the same failure rules rather than assuming success.
 */
fun isGitFailureMessage(message: String): Boolean {
    val normalized = message.trim().lowercase()
    return normalized.startsWith("error") ||
        normalized.startsWith("failed") ||
        normalized.contains("rejected") ||
        normalized.contains("conflict") ||
        normalized.contains("exception")
}
