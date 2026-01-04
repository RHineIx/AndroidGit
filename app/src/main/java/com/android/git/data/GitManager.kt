package com.android.git.data

import com.android.git.model.BranchModel
import com.android.git.model.ChangeType
import com.android.git.model.CommitItem
import com.android.git.model.DashboardState
import com.android.git.model.GitFile
import com.android.git.model.StashItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

class GitManager(private val rootDir: File) {

    private var git: Git? = null
    
    private var branchManager: GitBranchManager? = null
    private var stashManager: GitStashManager? = null
    
    val authManager = GitAuthManager(rootDir.parentFile ?: rootDir) 

    fun isGitRepo(): Boolean = File(rootDir, ".git").exists()

    // --- Core Operations ---

    suspend fun initRepo(): String = withContext(Dispatchers.IO) {
        runGitOperation {
            Git.init().setDirectory(rootDir).call()
            openRepo()
            "Repository initialized successfully!"
        }
    }

    suspend fun openRepo(): String = withContext(Dispatchers.IO) {
        runGitOperation {
            if (isGitRepo()) {
                git = Git.open(rootDir)
                branchManager = GitBranchManager(git!!)
                stashManager = GitStashManager(git!!)
                "Repository opened successfully!"
            } else {
                throw Exception("Not a Git repository.")
            }
        }
    }

    fun configureUser(name: String, email: String) {
        if (git == null && isGitRepo()) runBlockingOpen()
        val config = git?.repository?.config
        if (name.isNotEmpty()) config?.setString("user", null, "name", name)
        if (email.isNotEmpty()) config?.setString("user", null, "email", email)
        config?.save()
    }
    
    private fun runBlockingOpen() {
         try { git = Git.open(rootDir) } catch(_: Exception) {}
    }

    // --- Delegated Branch Operations ---
    suspend fun getRichBranches(): List<BranchModel> = withContext(Dispatchers.IO) {
        ensureOpen()
        branchManager?.getRichBranches() ?: emptyList()
    }

    suspend fun checkoutBranch(name: String): String = withContext(Dispatchers.IO) {
        runGitOperation { branchManager?.checkoutBranch(name) ?: "Error" }
    }

    suspend fun createBranch(name: String): String = withContext(Dispatchers.IO) {
        runGitOperation { branchManager?.createBranch(name) ?: "Error" }
    }

    suspend fun deleteBranch(name: String): String = withContext(Dispatchers.IO) {
        runGitOperation { branchManager?.deleteBranch(name) ?: "Error" }
    }
    
    suspend fun mergeBranch(name: String): String = withContext(Dispatchers.IO) {
        runGitOperation { branchManager?.mergeBranch(name) ?: "Error" }
    }
    
    suspend fun rebaseBranch(name: String): String = withContext(Dispatchers.IO) {
        runGitOperation { branchManager?.rebaseBranch(name) ?: "Error" }
    }
    
    suspend fun renameBranch(name: String): String = withContext(Dispatchers.IO) {
        runGitOperation { branchManager?.renameBranch(name) ?: "Error" }
    }
    
    // --- Delegated Stash Operations ---
    suspend fun stashChanges(msg: String): String = withContext(Dispatchers.IO) {
        runGitOperation { stashManager?.stashChanges(msg) ?: "Error" }
    }
    
    suspend fun applyStash(index: Int, drop: Boolean): String = withContext(Dispatchers.IO) {
        runGitOperation { stashManager?.applyStash(index, drop) ?: "Error" }
    }

    suspend fun dropStash(index: Int): String = withContext(Dispatchers.IO) {
        runGitOperation { stashManager?.dropStash(index) ?: "Error" }
    }

    suspend fun getStashList(): List<StashItem> = withContext(Dispatchers.IO) {
        ensureOpen()
        stashManager?.getStashList() ?: emptyList()
    }
    
    // --- Dashboard & Status ---

    suspend fun getDashboardStats(): DashboardState = withContext(Dispatchers.IO) {
        try {
            ensureOpen()
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
                         unpushedCount = if (remoteHead != null) {
                            git?.log()?.addRange(remoteHead, localHead)?.call()?.count() ?: 0
                         } else {
                            git?.log()?.call()?.count() ?: 0
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
            ensureOpen()
            val status = git?.status()?.call() ?: return@withContext emptyList()
            status.added.forEach { list.add(GitFile(it, ChangeType.ADDED)) }
            status.modified.forEach { list.add(GitFile(it, ChangeType.MODIFIED)) }
            status.changed.forEach { list.add(GitFile(it, ChangeType.MODIFIED)) }
            status.untracked.forEach { list.add(GitFile(it, ChangeType.UNTRACKED)) }
            status.missing.forEach { list.add(GitFile(it, ChangeType.DELETED)) }
            status.removed.forEach { list.add(GitFile(it, ChangeType.DELETED)) }
            status.conflicting.forEach { list.add(GitFile(it, ChangeType.MODIFIED)) }
            
            list.sortedBy { it.path }
        } catch (e: Exception) { emptyList() }
    }
    
    suspend fun getLastCommitMessage(): String = withContext(Dispatchers.IO) {
        try {
            ensureOpen()
            git?.log()?.setMaxCount(1)?.call()?.firstOrNull()?.fullMessage ?: ""
        } catch (e: Exception) { "" }
    }

    suspend fun readGitIgnore(): String = withContext(Dispatchers.IO) {
        val f = File(rootDir, ".gitignore")
        if (f.exists()) f.readText() else ""
    }

    suspend fun saveGitIgnore(content: String): String = withContext(Dispatchers.IO) {
        try {
            File(rootDir, ".gitignore").writeText(content)
            "Saved .gitignore"
        } catch (e: Exception) {
            "Error saving .gitignore: ${e.message}"
        }
    }

    // --- Commit & Remote ---

    suspend fun addToStage(files: List<GitFile>) = withContext(Dispatchers.IO) {
        ensureOpen()
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
    
    suspend fun revertCommit(hash: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val objId = git?.repository?.resolve(hash) ?: throw Exception("Commit not found")
            git?.revert()?.include(objId)?.call()
            "Reverted commit"
        }
    }

    suspend fun cherryPickCommit(hash: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val objId = git?.repository?.resolve(hash) ?: throw Exception("Commit not found")
            val res = git?.cherryPick()?.include(objId)?.call()
            if (res?.status == org.eclipse.jgit.api.CherryPickResult.CherryPickStatus.OK) "Cherry-picked!" else "Failed: ${res?.status}"
        }
    }
    
    // UPDATED: Now checks for push status
    suspend fun getCommits(): List<CommitItem> = withContext(Dispatchers.IO) {
        try {
            ensureOpen()
            val repo = git?.repository
            val logs = git?.log()?.call() ?: return@withContext emptyList()
            
            // Logic to determine pushed status
            val branch = repo?.branch
            val remoteRef = if (branch != null) repo.resolve("refs/remotes/origin/$branch") else null
            var isSynced = false
            
            val commits = mutableListOf<CommitItem>()
            
            logs.forEach { rev ->
                val hash = rev.name
                
                // Once we hit the remote hash, everything from here downwards (older) is pushed
                if (!isSynced && remoteRef != null && (rev.id == remoteRef)) {
                    isSynced = true
                }
                
                // If remoteRef is null (no remote branch), all are unpushed (isPushed = false)
                // If we found sync point, it's pushed. If not yet found, it's unpushed.
                val isPushed = isSynced
                
                commits.add(CommitItem(
                    message = rev.fullMessage.trim(),
                    author = rev.authorIdent.name ?: "Unknown",
                    date = rev.authorIdent.`when`,
                    hash = hash.substring(0, 7),
                    isPushed = isPushed
                ))
            }
            commits
        } catch (e: Exception) { emptyList() }
    }
    
    suspend fun resetToCommit(hash: String, hard: Boolean): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val objId = git?.repository?.resolve(hash) ?: throw Exception("Not found")
            val mode = if (hard) ResetCommand.ResetType.HARD else ResetCommand.ResetType.MIXED
            git?.reset()?.setMode(mode)?.setRef(objId.name)?.call()
            if (hard) "Reset HARD" else "Reset MIXED"
        }
    }
    
    suspend fun checkoutCommit(hash: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            git?.checkout()?.setName(hash)?.call()
            "Checked out $hash"
        }
    }

    // --- Remote (HTTPS Only) ---

    suspend fun hasRemote(): Boolean = withContext(Dispatchers.IO) {
        ensureOpen()
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

    suspend fun fetchAll(token: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val cmd = git?.fetch()?.setCheckFetchedObjects(true)
            if (token.isNotEmpty()) cmd?.setCredentialsProvider(authManager.getCredentialsProvider(token))
            cmd?.call()
            "Fetched all"
        }
    }

    suspend fun push(token: String, force: Boolean = false): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val cmd = git?.push()?.setForce(force)
            if (token.isNotEmpty()) cmd?.setCredentialsProvider(authManager.getCredentialsProvider(token))
            cmd?.call()
            if (force) "Force Pushed!" else "Pushed!"
        }
    }

    suspend fun pull(token: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val cmd = git?.pull()
            if (token.isNotEmpty()) cmd?.setCredentialsProvider(authManager.getCredentialsProvider(token))
            cmd?.call()
            "Pulled!"
        }
    }

    suspend fun linkAndRepair(token: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val cmd = git?.fetch()
            if (token.isNotEmpty()) cmd?.setCredentialsProvider(authManager.getCredentialsProvider(token))
            cmd?.call()
            
            val remotes = git?.branchList()?.setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE)?.call()
            val mainBranch = remotes?.find { it.name.contains("main") } ?: remotes?.find { it.name.contains("master") } ?: throw Exception("No remote main found")
            val name = mainBranch.name.substringAfterLast("/")
            
            git?.branchCreate()?.setName(name)?.setStartPoint(mainBranch.name)?.setForce(true)?.call()
            git?.checkout()?.setName(name)?.call()
            git?.reset()?.setMode(ResetCommand.ResetType.MIXED)?.setRef(mainBranch.name)?.call()
            "Repaired & Linked to $name"
        }
    }
    
    private fun ensureOpen() {
        if (git == null) {
            if (isGitRepo()) {
                 git = Git.open(rootDir)
                 branchManager = GitBranchManager(git!!)
                 stashManager = GitStashManager(git!!)
            }
        }
    }

    private inline fun runGitOperation(block: () -> String): String {
        return try {
            ensureOpen()
            if (git == null) return "Error: Not a Git repo"
            block()
        } catch (e: Exception) { e.message ?: "Unknown Error" }
    }
    
    suspend fun getBranches(): List<String> = withContext(Dispatchers.IO) {
        ensureOpen()
        branchManager?.getRichBranches()?.map { it.name } ?: emptyList()
    }
    
    companion object {
        suspend fun cloneRepo(url: String, parentDir: File, folderName: String, token: String): Pair<File?, String> = withContext(Dispatchers.IO) {
            val destDir = File(parentDir, folderName)
            if (destDir.exists() && destDir.listFiles()?.isNotEmpty() == true) return@withContext Pair(null, "Error: Folder exists.")
            try {
                val cmd = Git.cloneRepository().setURI(url).setDirectory(destDir)
                if (token.isNotEmpty()) cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
                cmd.call()
                Pair(destDir, "Cloned!")
            } catch (e: Exception) {
                if (destDir.exists()) destDir.deleteRecursively()
                Pair(null, "Clone failed: ${e.message}")
            }
        }
    }
}