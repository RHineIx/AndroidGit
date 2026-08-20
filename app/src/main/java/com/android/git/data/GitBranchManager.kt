package com.android.git.data

import com.android.git.model.BranchModel
import com.android.git.model.BranchType
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.Ref

class GitBranchManager(private val git: Git) {

    fun getRichBranches(): List<BranchModel> {
        val repository = git.repository
        val currentBranch = repository.branch
        val refs = git.branchList()
            .setListMode(ListBranchCommand.ListMode.ALL)
            .call()

        return refs.map { ref ->
            val type = if (ref.name.startsWith(REMOTE_PREFIX)) {
                BranchType.REMOTE
            } else {
                BranchType.LOCAL
            }
            val name = displayName(ref)
            BranchModel(
                name = name,
                fullPath = ref.name,
                type = type,
                isCurrent = type == BranchType.LOCAL && name == currentBranch
            )
        }.distinctBy { "${it.type}:$it.fullPath" }
            .sortedWith(compareBy({ !it.isCurrent }, { it.type }, { it.name }))
    }

    fun createBranch(branchName: String): String {
        val name = branchName.trim()
        require(name.isNotEmpty()) { "Branch name cannot be empty." }
        git.branchCreate().setName(name).call()
        return "Created branch $name"
    }

    fun deleteBranch(branchName: String): String {
        val name = localBranchName(branchName)
        val current = git.repository.branch
        if (current == name) throw Exception("Cannot delete active branch!")
        git.branchDelete().setBranchNames(name).setForce(true).call()
        return "Deleted branch $name"
    }

    fun checkoutBranch(branchRefOrName: String): String {
        val requested = branchRefOrName.trim()
        require(requested.isNotEmpty()) { "Branch name cannot be empty." }

        val repository = git.repository
        val localName = localBranchName(requested)
        val localRef = repository.findRef("$LOCAL_PREFIX$localName")
        if (localRef != null) {
            git.checkout().setName(localName).call()
            return "Switched to $localName"
        }

        val remoteRef = resolveRemoteRef(requested)
        if (remoteRef != null) {
            // Keep the complete logical branch name. Do not use substringAfterLast("/").
            val remoteBranchName = displayName(remoteRef)
            git.checkout()
                .setCreateBranch(true)
                .setName(remoteBranchName)
                .setStartPoint(remoteRef.name)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .call()
            return "Switched to $remoteBranchName"
        }

        git.checkout().setCreateBranch(true).setName(localName).call()
        return "Switched to $localName"
    }

    fun renameBranch(newName: String): String {
        val name = newName.trim()
        require(name.isNotEmpty()) { "Branch name cannot be empty." }
        git.branchRename().setNewName(name).call()
        return "Renamed to $name"
    }

    fun mergeBranch(branchName: String): String {
        val ref = resolveBranchRef(branchName) ?: throw Exception("Branch not found")
        val result = git.merge().include(ref).call()
        return if (result.mergeStatus.isSuccessful) {
            "Merged ${displayName(ref)}"
        } else {
            "Merge failed: ${result.mergeStatus}"
        }
    }

    fun rebaseBranch(branchName: String): String {
        val status = git.status().call()
        if (status.hasUncommittedChanges()) throw Exception("Commit changes before rebasing.")

        val ref = resolveBranchRef(branchName) ?: throw Exception("Branch not found")
        val result = git.rebase().setUpstream(ref.objectId).call()
        return if (result.status.isSuccessful) {
            "Rebased onto ${displayName(ref)}"
        } else {
            "Rebase failed: ${result.status}"
        }
    }

    private fun resolveBranchRef(branchRefOrName: String): Ref? {
        val requested = branchRefOrName.trim()
        val repository = git.repository
        return repository.findRef(requested)
            ?: repository.findRef("$LOCAL_PREFIX${localBranchName(requested)}")
            ?: resolveRemoteRef(requested)
    }

    private fun resolveRemoteRef(branchRefOrName: String): Ref? {
        val requested = branchRefOrName.trim()
        val remoteRefs = git.branchList()
            .setListMode(ListBranchCommand.ListMode.REMOTE)
            .call()

        return remoteRefs.firstOrNull { ref ->
            val remoteQualifiedName = ref.name.removePrefix(REMOTE_PREFIX)
            ref.name == requested ||
                remoteQualifiedName == requested ||
                displayName(ref) == requested ||
                displayName(ref) == localBranchName(requested)
        }
    }

    private fun localBranchName(branchRefOrName: String): String {
        return when {
            branchRefOrName.startsWith(LOCAL_PREFIX) -> branchRefOrName.removePrefix(LOCAL_PREFIX)
            branchRefOrName.startsWith(REMOTE_PREFIX) -> {
                branchRefOrName.removePrefix(REMOTE_PREFIX).substringAfter('/', "")
            }
            else -> branchRefOrName
        }
    }

    private fun displayName(ref: Ref): String = logicalBranchName(ref.name)

    companion object {
        private const val LOCAL_PREFIX = "refs/heads/"
        private const val REMOTE_PREFIX = "refs/remotes/"

        internal fun logicalBranchName(refName: String): String {
            return when {
                refName.startsWith(LOCAL_PREFIX) -> refName.removePrefix(LOCAL_PREFIX)
                refName.startsWith(REMOTE_PREFIX) -> {
                    // Remove only the remote name; retain every slash in the branch name.
                    refName.removePrefix(REMOTE_PREFIX).substringAfter('/', "")
                }
                else -> refName
            }
        }
    }
}
