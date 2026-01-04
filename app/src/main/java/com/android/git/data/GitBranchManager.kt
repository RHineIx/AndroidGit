package com.android.git.data

import com.android.git.model.BranchModel
import com.android.git.model.BranchType
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.Repository

class GitBranchManager(private val git: Git) {

    fun getRichBranches(): List<BranchModel> {
        val list = mutableListOf<BranchModel>()
        val currentBranch = git.repository.fullBranch
        val refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()

        refs.forEach { ref ->
            val name = ref.name
            val shortName = Repository.shortenRefName(name)
            val type = if (name.startsWith("refs/remotes/")) BranchType.REMOTE else BranchType.LOCAL
            val isCurrent = (name == currentBranch)
            list.add(BranchModel(shortName, name, type, isCurrent))
        }
        return list.sortedWith(compareBy({ !it.isCurrent }, { it.type }, { it.name }))
    }

    fun createBranch(branchName: String): String {
        git.branchCreate().setName(branchName).call()
        return "Created branch $branchName"
    }

    fun deleteBranch(branchName: String): String {
        val current = git.repository.branch
        if (current == branchName) throw Exception("Cannot delete active branch!")
        git.branchDelete().setBranchNames(branchName).setForce(true).call()
        return "Deleted branch $branchName"
    }

    fun checkoutBranch(branchName: String): String {
        val isLocal = git.branchList().call().any { it.name.endsWith(branchName) }
        
        if (isLocal) {
            git.checkout().setName(branchName).call()
        } else {
            val remoteRefs = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()
            val remoteRef = remoteRefs.find { it.name.endsWith("/$branchName") }
            
            if (remoteRef != null) {
                git.checkout()
                    .setCreateBranch(true)
                    .setName(branchName)
                    .setStartPoint("origin/$branchName")
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .call()
            } else {
                git.checkout().setCreateBranch(true).setName(branchName).call()
            }
        }
        return "Switched to $branchName"
    }

    fun renameBranch(newName: String): String {
        git.branchRename().setNewName(newName).call()
        return "Renamed to $newName"
    }

    fun mergeBranch(branchName: String): String {
        val repo = git.repository
        val ref = repo.findRef(branchName) ?: throw Exception("Branch not found")
        val result = git.merge().include(ref).call()
        return if (result.mergeStatus.isSuccessful) "Merged $branchName" else "Merge failed: ${result.mergeStatus}"
    }

    fun rebaseBranch(branchName: String): String {
        val status = git.status().call()
        if (status.hasUncommittedChanges()) throw Exception("Commit changes before rebasing.")
        
        val repo = git.repository
        val ref = repo.findRef(branchName) ?: throw Exception("Branch not found")
        // Fix: Pass ObjectId
        val result = git.rebase().setUpstream(ref.objectId).call()
        return if (result.status.isSuccessful) "Rebased onto $branchName" else "Rebase failed: ${result.status}"
    }
}