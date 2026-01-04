package com.android.git.data

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
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

class GitManager(private val rootDir: File) {

    private var git: Git? = null

    // Helper to check if repo exists
    fun isGitRepo(): Boolean {
        return File(rootDir, ".git").exists()
    }

    // 1. Init
    suspend fun initRepo(): String = withContext(Dispatchers.IO) {
        return@withContext try {
            Git.init().setDirectory(rootDir).call()
            git = Git.open(rootDir)
            "Repository initialized successfully!"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    // 2. Open
    suspend fun openRepo(): String = withContext(Dispatchers.IO) {
        return@withContext try {
            if (isGitRepo()) {
                git = Git.open(rootDir)
                "Repository opened successfully!"
            } else {
                "Not a Git repository."
            }
        } catch (e: Exception) {
            "Error opening repo: ${e.message}"
        }
    }

    // 3. Dashboard Stats
    suspend fun getDashboardStats(): DashboardState = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            
            val repo = git?.repository
            val branch = repo?.branch ?: "Unknown"
            
            // Status (Local changes)
            val status = git?.status()?.call()
            val changedCount = (status?.untracked?.size ?: 0) + 
                             (status?.modified?.size ?: 0) + 
                             (status?.added?.size ?: 0) +
                             (status?.missing?.size ?: 0) +
                             (status?.removed?.size ?: 0)
            
            // Unpushed Commits Count
            var unpushedCount = 0
            if (hasRemote()) {
                try {
                    val localHead = repo?.resolve("HEAD")
                    val remoteHead = repo?.resolve("refs/remotes/origin/$branch")
                    
                    if (localHead != null && remoteHead != null) {
                        val unpushed = git?.log()?.addRange(remoteHead, localHead)?.call()
                        unpushedCount = unpushed?.count() ?: 0
                    } else if (localHead != null && remoteHead == null) {
                        val all = git?.log()?.call()
                        unpushedCount = all?.count() ?: 0
                    }
                } catch (e: Exception) {
                    // Ignore errors
                }
            }

            DashboardState.Success(branch, changedCount, unpushedCount)
        } catch (e: Exception) {
            DashboardState.Error(e.message ?: "Unknown Error")
        }
    }

    // 4. Get Changed Files
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
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 5. Stage Files
    suspend fun addToStage(files: List<GitFile>) = withContext(Dispatchers.IO) {
        if (git == null) openRepo()
        
        val addCommand = git?.add()
        val rmCommand = git?.rm()
        var hasAdds = false
        var hasRms = false

        files.forEach { file ->
            if (file.type == ChangeType.DELETED || file.type == ChangeType.MISSING) {
                rmCommand?.addFilepattern(file.path)
                hasRms = true
            } else {
                addCommand?.addFilepattern(file.path)
                hasAdds = true
            }
        }

        if (hasAdds) addCommand?.call()
        if (hasRms) rmCommand?.call()
    }

    // 6. Commit
    suspend fun commit(message: String, amend: Boolean = false): String = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            git?.commit()
                ?.setMessage(message)
                ?.setAmend(amend)
                ?.call()
            
            if (amend) "Commit amended successfully!" else "Commit successful!"
        } catch (e: Exception) {
            "Commit failed: ${e.message}"
        }
    }

    // 7. Get History
    suspend fun getCommits(): List<CommitItem> = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            val logs = git?.log()?.call()
            val commits = mutableListOf<CommitItem>()
            
            logs?.forEach { rev ->
                commits.add(
                    CommitItem(
                        message = rev.fullMessage.trim(),
                        author = rev.authorIdent.name,
                        date = rev.authorIdent.`when`,
                        hash = rev.name.substring(0, 7)
                    )
                )
            }
            commits
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Get Last Commit Message
    suspend fun getLastCommitMessage(): String = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            val lastCommit = git?.log()?.setMaxCount(1)?.call()?.firstOrNull()
            lastCommit?.fullMessage ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // Reset To Specific Commit
    suspend fun resetToCommit(hash: String, hard: Boolean): String = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            val repo = git?.repository
            val objId = repo?.resolve(hash) ?: return@withContext "Commit not found."

            val mode = if (hard) ResetCommand.ResetType.HARD else ResetCommand.ResetType.MIXED
            git?.reset()
                ?.setMode(mode)
                ?.setRef(objId.name)
                ?.call()

            if (hard) "Hard reset to $hash (Changes discarded)" else "Reset to $hash (Changes kept)"
        } catch (e: Exception) {
            "Reset failed: ${e.message}"
        }
    }
    
    // Checkout Specific Commit
    suspend fun checkoutCommit(hash: String): String = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            git?.checkout()?.setName(hash)?.call()
            "Checked out commit $hash"
        } catch (e: Exception) {
            "Checkout failed: ${e.message}"
        }
    }

    // 8. Get Unpushed
    suspend fun getUnpushedCommits(): List<CommitItem> = withContext(Dispatchers.IO) {
        val commits = mutableListOf<CommitItem>()
        try {
            if (git == null) openRepo()
            val branch = git?.repository?.branch
            val localHead = git?.repository?.resolve("HEAD")
            val remoteHead = git?.repository?.resolve("refs/remotes/origin/$branch")

            if (localHead != null) {
                val command = git?.log()
                if (remoteHead != null) {
                    command?.addRange(remoteHead, localHead)
                } else {
                    command?.add(localHead)
                }
                val logs = command?.call()
                logs?.forEach { rev ->
                    commits.add(
                        CommitItem(
                            message = rev.fullMessage.trim(),
                            author = rev.authorIdent.name,
                            date = rev.authorIdent.`when`,
                            hash = rev.name.substring(0, 7)
                        )
                    )
                }
            }
            commits
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- .gitignore Management ---

    suspend fun readGitIgnore(): String = withContext(Dispatchers.IO) {
        val file = File(rootDir, ".gitignore")
        if (file.exists()) file.readText() else ""
    }

    suspend fun saveGitIgnore(content: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(rootDir, ".gitignore")
            file.writeText(content)
            "Saved .gitignore"
        } catch (e: Exception) {
            "Error saving .gitignore: ${e.message}"
        }
    }

    // --- Remote & Sync ---

    suspend fun hasRemote(): Boolean = withContext(Dispatchers.IO) {
        if (git == null) openRepo()
        val config = git?.repository?.config
        val remoteUrl = config?.getString("remote", "origin", "url")
        !remoteUrl.isNullOrEmpty()
    }

    suspend fun addRemote(url: String): String = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            val config = git?.repository?.config
            config?.setString("remote", "origin", "url", url)
            config?.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*")
            config?.save()
            "Remote added successfully!"
        } catch (e: Exception) {
            "Error adding remote: ${e.message}"
        }
    }

    suspend fun push(token: String, force: Boolean = false): String = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            val credentials = UsernamePasswordCredentialsProvider(token, "")
            
            git?.push()
                ?.setCredentialsProvider(credentials)
                ?.setForce(force)
                ?.call()
                
            if (force) "Force Push successful!" else "Push successful!"
        } catch (e: Exception) {
            "Push failed: ${e.message}"
        }
    }

    suspend fun pull(token: String): String = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            val credentials = UsernamePasswordCredentialsProvider(token, "")
            git?.pull()?.setCredentialsProvider(credentials)?.call()
            "Pull successful!"
        } catch (e: Exception) {
            "Pull failed: ${e.message}"
        }
    }

    suspend fun linkAndRepair(token: String): String = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            val credentials = UsernamePasswordCredentialsProvider(token, "")
            git?.fetch()?.setCredentialsProvider(credentials)?.call()

            val remoteBranches = git?.branchList()?.setListMode(ListBranchCommand.ListMode.REMOTE)?.call()
            val targetBranch = remoteBranches?.find { it.name.contains("main") } 
                           ?: remoteBranches?.find { it.name.contains("master") }
                           ?: return@withContext "Could not find remote 'main' or 'master' branch."

            val branchName = targetBranch.name.substringAfterLast("/")

            git?.branchCreate()
                ?.setName(branchName)
                ?.setStartPoint(targetBranch.name)
                ?.setForce(true)
                ?.call()

            git?.checkout()?.setName(branchName)?.call()

            git?.reset()
                ?.setMode(ResetCommand.ResetType.MIXED)
                ?.setRef(targetBranch.name)
                ?.call()

            "Linked successfully! You are now on '$branchName'."
        } catch (e: Exception) {
            "Link failed: ${e.message}"
        }
    }

    // --- Branching ---

    suspend fun getBranches(): List<String> = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()
            val refs = git?.branchList()?.setListMode(ListBranchCommand.ListMode.ALL)?.call()
            refs?.map { ref -> 
                ref.name.substringAfter("refs/heads/").substringAfter("refs/remotes/origin/") 
            }?.distinct()?.sorted() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun checkoutBranch(branchName: String): String = withContext(Dispatchers.IO) {
        try {
            if (git == null) openRepo()

            val localRefs = git?.branchList()?.call()
            val isLocal = localRefs?.any { it.name == "refs/heads/$branchName" } == true

            if (isLocal) {
                git?.checkout()?.setName(branchName)?.call()
            } else {
                val remoteRefs = git?.branchList()?.setListMode(ListBranchCommand.ListMode.REMOTE)?.call()
                val remoteRef = remoteRefs?.find { it.name.endsWith("/$branchName") }

                if (remoteRef != null) {
                    git?.checkout()
                        ?.setCreateBranch(true)
                        ?.setName(branchName)
                        ?.setStartPoint("origin/$branchName")
                        ?.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        ?.call()
                } else {
                    git?.checkout()
                        ?.setCreateBranch(true)
                        ?.setName(branchName)
                        ?.call()
                }
            }
            "Switched to $branchName"
        } catch (e: Exception) {
            "Checkout failed: ${e.message}"
        }
    }
    
    suspend fun createBranch(branchName: String): String = withContext(Dispatchers.IO) {
         try {
            if (git == null) openRepo()
            git?.branchCreate()?.setName(branchName)?.call()
            "Created branch $branchName"
        } catch (e: Exception) {
            "Create branch failed: ${e.message}"
        }
    }

    companion object {
        suspend fun cloneRepo(url: String, parentDir: File, folderName: String, token: String): Pair<File?, String> = withContext(Dispatchers.IO) {
            val destDir = File(parentDir, folderName)
            if (destDir.exists() && destDir.listFiles()?.isNotEmpty() == true) {
                return@withContext Pair(null, "Error: Folder '$folderName' already exists and is not empty.")
            }
            
            try {
                val command = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(destDir)
                
                if (token.isNotEmpty()) {
                    command.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
                }
                
                command.call()
                Pair(destDir, "Clone successful!")
            } catch (e: Exception) {
                if (destDir.exists()) destDir.deleteRecursively()
                Pair(null, "Clone failed: ${e.message}")
            }
        }
    }
}