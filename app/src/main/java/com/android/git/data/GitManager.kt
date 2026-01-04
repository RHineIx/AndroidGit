package com.android.git.data

import com.android.git.model.BranchModel
import com.android.git.model.BranchType
import com.android.git.model.ChangeType
import com.android.git.model.CommitItem
import com.android.git.model.DashboardState
import com.android.git.model.GitFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

class GitManager(private val rootDir: File) {

    private var git: Git? = null

    fun isGitRepo(): Boolean = File(rootDir, ".git").exists()

    // --- Init & Open ---
    suspend fun initRepo(): String = withContext(Dispatchers.IO) {
        runGitOperation {
            Git.init().setDirectory(rootDir).call()
            git = Git.open(rootDir)
            "Repository initialized successfully!"
        }
    }

    suspend fun openRepo(): String = withContext(Dispatchers.IO) {
        runGitOperation {
            if (isGitRepo()) {
                git = Git.open(rootDir)
                "Repository opened successfully!"
            } else {
                throw Exception("Not a Git repository.")
            }
        }
    }

    fun configureUser(name: String, email: String) {
        if (git == null && isGitRepo()) git = Git.open(rootDir)
        val config = git?.repository?.config
        if (name.isNotEmpty()) config?.setString("user", null, "name", name)
        if (email.isNotEmpty()) config?.setString("user", null, "email", email)
        config?.save()
    }

    // --- Dashboard ---
    suspend fun getDashboardStats(): DashboardState = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            val repo = git?.repository
            val branch = repo?.branch ?: "Unknown"
            val status = git?.status()?.call()
            val changedCount = (status?.untracked?.size ?: 0) + (status?.modified?.size ?: 0) + (status?.added?.size ?: 0) + (status?.missing?.size ?: 0) + (status?.removed?.size ?: 0)
            
            var unpushedCount = 0
            if (hasRemote()) {
                try {
                    val localHead = repo?.resolve("HEAD")
                    val remoteHead = repo?.resolve("refs/remotes/origin/$branch")
                    if (localHead != null) {
                         if (remoteHead != null) {
                            unpushedCount = git?.log()?.addRange(remoteHead, localHead)?.call()?.count() ?: 0
                         } else {
                            unpushedCount = git?.log()?.call()?.count() ?: 0
                         }
                    }
                } catch (_: Exception) { }
            }
            DashboardState.Success(branch, changedCount, unpushedCount)
        } catch (e: Exception) {
            DashboardState.Error(e.message ?: "Unknown Error")
        }
    }

    suspend fun getChangedFiles(): List<GitFile> = withContext(Dispatchers.IO) {
        val list = mutableListOf<GitFile>()
        try {
            if (git == null) openRepo()
            val status = git?.status()?.call() ?: return@withContext emptyList()
            status.added.forEach { list.add(GitFile(it, ChangeType.ADDED)) }
            status.modified.forEach { list.add(GitFile(it, ChangeType.MODIFIED)) }
            status.changed.forEach { list.add(GitFile(it, ChangeType.MODIFIED)) }
            status.untracked.forEach { list.add(GitFile(it, ChangeType.UNTRACKED)) }
            status.missing.forEach { list.add(GitFile(it, ChangeType.DELETED)) }
            status.removed.forEach { list.add(GitFile(it, ChangeType.DELETED)) }
            list.sortedBy { it.path }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addToStage(files: List<GitFile>) = withContext(Dispatchers.IO) {
        if (git == null) openRepo()
        val addCommand = git?.add()
        val rmCommand = git?.rm()
        var hasAdds = false; var hasRms = false
        files.forEach { file ->
            if (file.type == ChangeType.DELETED || file.type == ChangeType.MISSING) {
                rmCommand?.addFilepattern(file.path); hasRms = true
            } else {
                addCommand?.addFilepattern(file.path); hasAdds = true
            }
        }
        if (hasAdds) addCommand?.call()
        if (hasRms) rmCommand?.call()
    }

    suspend fun commit(message: String, amend: Boolean = false): String = withContext(Dispatchers.IO) {
        runGitOperation {
            git?.commit()?.setMessage(message)?.setAmend(amend)?.call()
            if (amend) "Commit amended!" else "Committed!"
        }
    }

    suspend fun getCommits(): List<CommitItem> = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            val logs = git?.log()?.call()
            val commits = mutableListOf<CommitItem>()
            logs?.forEach { rev ->
                commits.add(CommitItem(rev.fullMessage.trim(), rev.authorIdent.name ?: "Unknown", rev.authorIdent.`when`, rev.name.substring(0, 7)))
            }
            commits
        } catch (e: Exception) { emptyList() }
    }
    
    suspend fun getLastCommitMessage(): String = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            git?.log()?.setMaxCount(1)?.call()?.firstOrNull()?.fullMessage ?: ""
        } catch (e: Exception) { "" }
    }

    // --- Reset & Checkout ---
    suspend fun resetToCommit(hash: String, hard: Boolean): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val repo = git?.repository
            val objId = repo?.resolve(hash) ?: throw Exception("Commit not found")
            val mode = if (hard) ResetCommand.ResetType.HARD else ResetCommand.ResetType.MIXED
            git?.reset()?.setMode(mode)?.setRef(objId.name)?.call()
            if (hard) "Reset HARD to $hash" else "Reset MIXED to $hash"
        }
    }

    suspend fun checkoutCommit(hash: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            git?.checkout()?.setName(hash)?.call()
            "Checked out $hash"
        }
    }

    // --- Advanced Branching Operations ---

    suspend fun getRichBranches(): List<BranchModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<BranchModel>()
        try {
            if (git == null) openRepo()
            val currentBranch = git?.repository?.fullBranch
            val refs = git?.branchList()?.setListMode(ListBranchCommand.ListMode.ALL)?.call()
            
            refs?.forEach { ref ->
                val name = ref.name
                val shortName = Repository.shortenRefName(name)
                val type = if (name.startsWith("refs/remotes/")) BranchType.REMOTE else BranchType.LOCAL
                val isCurrent = (name == currentBranch)
                list.add(BranchModel(shortName, name, type, isCurrent))
            }
            list.sortedWith(compareBy({ !it.isCurrent }, { it.type }, { it.name }))
        } catch (e: Exception) { emptyList() }
    }

    suspend fun deleteBranch(branchName: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val current = git?.repository?.branch
            if (current == branchName) throw Exception("Cannot delete active branch!")
            git?.branchDelete()?.setBranchNames(branchName)?.setForce(true)?.call()
            "Deleted branch $branchName"
        }
    }

    suspend fun checkoutBranch(branchName: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            if (git?.repository?.resolve("HEAD") == null) throw Exception("Repo empty. Commit first.")
            
            val isLocal = git?.branchList()?.call()?.any { it.name.endsWith(branchName) } == true
            if (isLocal) {
                git?.checkout()?.setName(branchName)?.call()
            } else {
                val remoteRefs = git?.branchList()?.setListMode(ListBranchCommand.ListMode.REMOTE)?.call()
                val remoteRef = remoteRefs?.find { it.name.endsWith("/$branchName") }
                if (remoteRef != null) {
                    git?.checkout()?.setCreateBranch(true)?.setName(branchName)
                        ?.setStartPoint("origin/$branchName")?.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)?.call()
                } else {
                    git?.checkout()?.setCreateBranch(true)?.setName(branchName)?.call()
                }
            }
            "Switched to $branchName"
        }
    }
    
    suspend fun createBranch(branchName: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            if (git?.repository?.resolve("HEAD") == null) throw Exception("Repo empty. Commit first.")
            git?.branchCreate()?.setName(branchName)?.call()
            "Created branch $branchName"
        }
    }

    // NEW: Merge
    suspend fun mergeBranch(branchName: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val repo = git?.repository
            val ref = repo?.findRef(branchName) ?: throw Exception("Branch not found")
            val result = git?.merge()?.include(ref)?.call()
            if (result?.mergeStatus?.isSuccessful == true) "Merged $branchName successfully"
            else "Merge failed: ${result?.mergeStatus}"
        }
    }

    // NEW: Rebase
    suspend fun rebaseBranch(branchName: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            // Usually requires clean working tree
            val status = git?.status()?.call()
            if (status?.hasUncommittedChanges() == true) throw Exception("Please commit changes before rebasing.")
            
            val repo = git?.repository
            val ref = repo?.findRef(branchName) ?: throw Exception("Branch not found")
            
            // FIX: Pass ref.objectId instead of ref
            val result = git?.rebase()?.setUpstream(ref.objectId)?.call()
            
            if (result?.status?.isSuccessful == true) "Rebased onto $branchName"
            else "Rebase failed: ${result?.status}"
        }
    }

    // NEW: Rename
    suspend fun renameBranch(newName: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            git?.branchRename()?.setNewName(newName)?.call()
            "Renamed to $newName"
        }
    }

    // NEW: Fetch All (Update Remotes)
    suspend fun fetchAll(token: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val creds = UsernamePasswordCredentialsProvider(token, "")
            git?.fetch()?.setCredentialsProvider(creds)?.setCheckFetchedObjects(true)?.call()
            "Fetched all remote updates"
        }
    }

    // NEW: Revert a specific commit (Create a new commit that undoes changes)
    suspend fun revertCommit(hash: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val repo = git?.repository
            val objId = repo?.resolve(hash) ?: throw Exception("Commit not found")
            val result = git?.revert()?.include(objId)?.call()
            if (result != null) "Revert successful! New commit created." else "Revert failed."
        }
    }

    // NEW: Cherry-Pick a specific commit (Apply changes from another branch/commit to current)
    suspend fun cherryPickCommit(hash: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val repo = git?.repository
            val objId = repo?.resolve(hash) ?: throw Exception("Commit not found")
            val result = git?.cherryPick()?.include(objId)?.call()
            
            if (result?.status == org.eclipse.jgit.api.CherryPickResult.CherryPickStatus.OK) {
                "Cherry-pick successful!"
            } else {
                "Cherry-pick failed: ${result?.status}"
            }
        }
    }
    
    // --- Remote & Sync ---
    suspend fun hasRemote(): Boolean = withContext(Dispatchers.IO) {
        if (git == null) openRepo()
        !git?.repository?.config?.getString("remote", "origin", "url").isNullOrEmpty()
    }

    suspend fun addRemote(url: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val config = git?.repository?.config
            config?.setString("remote", "origin", "url", url)
            config?.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*")
            config?.save()
            "Remote linked!"
        }
    }

    suspend fun push(token: String, force: Boolean = false): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val creds = UsernamePasswordCredentialsProvider(token, "")
            git?.push()?.setCredentialsProvider(creds)?.setForce(force)?.call()
            if (force) "Force Pushed!" else "Pushed successfully!"
        }
    }

    suspend fun pull(token: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val creds = UsernamePasswordCredentialsProvider(token, "")
            git?.pull()?.setCredentialsProvider(creds)?.call()
            "Pulled successfully!"
        }
    }

    suspend fun linkAndRepair(token: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val creds = UsernamePasswordCredentialsProvider(token, "")
            git?.fetch()?.setCredentialsProvider(creds)?.call()
            
            val remotes = git?.branchList()?.setListMode(ListBranchCommand.ListMode.REMOTE)?.call()
            val mainBranch = remotes?.find { it.name.contains("main") } ?: remotes?.find { it.name.contains("master") } ?: throw Exception("No remote main/master branch.")
            val name = mainBranch.name.substringAfterLast("/")
            
            git?.branchCreate()?.setName(name)?.setStartPoint(mainBranch.name)?.setForce(true)?.call()
            git?.checkout()?.setName(name)?.call()
            git?.reset()?.setMode(ResetCommand.ResetType.MIXED)?.setRef(mainBranch.name)?.call()
            "Repaired & Linked to $name"
        }
    }

    // --- .gitignore ---
    suspend fun readGitIgnore(): String = withContext(Dispatchers.IO) {
        val f = File(rootDir, ".gitignore")
        if (f.exists()) f.readText() else ""
    }

    suspend fun saveGitIgnore(content: String): String = withContext(Dispatchers.IO) {
        File(rootDir, ".gitignore").writeText(content)
        "Saved .gitignore"
    }

    // --- Helpers ---
    suspend fun getBranches(): List<String> = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            val refs = git?.branchList()?.setListMode(ListBranchCommand.ListMode.ALL)?.call()
            refs?.map { it.name.substringAfter("refs/heads/").substringAfter("refs/remotes/origin/") }?.distinct()?.sorted() ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getUnpushedCommits(): List<CommitItem> = withContext(Dispatchers.IO) {
        emptyList() 
    }

    private inline fun runGitOperation(block: () -> String): String {
        return try {
            if (git == null) {
                if (isGitRepo()) git = Git.open(rootDir)
                else return "Error: Not a Git repository"
            }
            block()
        } catch (e: Exception) { e.message ?: "Unknown Error" }
    }

    companion object {
        suspend fun cloneRepo(url: String, parentDir: File, folderName: String, token: String): Pair<File?, String> = withContext(Dispatchers.IO) {
            val destDir = File(parentDir, folderName)
            if (destDir.exists() && destDir.listFiles()?.isNotEmpty() == true) return@withContext Pair(null, "Error: Folder exists & not empty.")
            try {
                val cmd = Git.cloneRepository().setURI(url).setDirectory(destDir)
                if (token.isNotEmpty()) cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
                cmd.call()
                Pair(destDir, "Cloned successfully!")
            } catch (e: Exception) {
                if (destDir.exists()) destDir.deleteRecursively()
                Pair(null, "Clone failed: ${e.message}")
            }
        }
    }
}