package com.android.git.data

import com.android.git.model.StashItem
import org.eclipse.jgit.api.Git

class GitStashManager(private val git: Git) {

    fun stashChanges(message: String): String {
        val cmd = git.stashCreate()
        if (message.isNotEmpty()) cmd.setWorkingDirectoryMessage(message)
        val commit = cmd.call()
        return if (commit != null) "Stashed: ${commit.shortMessage}" else "No changes to stash"
    }

    fun applyStash(index: Int, drop: Boolean): String {
        try {
            git.stashApply().setStashRef("stash@{$index}").call()
            if (drop) {
                val dropResult = dropStash(index)
                return if (dropResult.startsWith("Failed", ignoreCase = true) || dropResult.startsWith("Error", ignoreCase = true)) {
                    "Error: Stash applied, but drop failed: $dropResult"
                } else {
                    "Stash applied and dropped"
                }
            }
            return "Stash applied"
        } catch (e: Exception) {
            return "Failed to apply stash: ${e.message}"
        }
    }
    
    fun dropStash(index: Int): String {
        try {
            git.stashDrop().setStashRef(index).call()
            return "Stash dropped"
        } catch (e: Exception) {
            return "Failed to drop stash: ${e.message}"
        }
    }
    
    fun getStashList(): List<StashItem> {
        val stashes = git.stashList().call()
        return stashes.mapIndexed { index, rev ->
            StashItem(
                index = index,
                message = rev.shortMessage,
                hash = rev.name.take(7)
            )
        }
    }
}