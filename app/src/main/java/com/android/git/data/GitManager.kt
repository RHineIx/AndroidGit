package com.android.git.data

import com.android.git.model.BranchModel
import com.android.git.model.ChangeType
import com.android.git.model.CommitItem
import com.android.git.model.DashboardState
import com.android.git.model.GitFile
import com.android.git.model.StashItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class GitManager(
    private val rootDir: File,
    secureStorageDir: File? = null
) : Closeable {

    private var git: Git? = null
    private var branchManager: GitBranchManager? = null
    private var stashManager: GitStashManager? = null

    // Segregated Mutexes to prevent read/write bottlenecks
    private val writeMutex = Mutex()
    private val lifecycleMutex = Mutex()
    private val isClosed = AtomicBoolean(false)

    val authManager = GitAuthManager(secureStorageDir ?: rootDir.parentFile ?: rootDir)

    fun isGitRepo(): Boolean = File(rootDir, ".git").exists()

    suspend fun initRepo(): String = withContext(Dispatchers.IO) {
        lifecycleMutex.withLock {
            runCatching {
                Git.init().setDirectory(rootDir).call()
                openRepoInternal()
                "Repository initialized successfully!"
            }.getOrElse { "Failed to init: ${it.message}" }
        }
    }

    suspend fun openRepo(): String = withContext(Dispatchers.IO) {
        lifecycleMutex.withLock {
            runCatching {
                openRepoInternal()
                "Repository opened successfully!"
            }.getOrElse { "Failed to open: ${it.message}" }
        }
    }

    private fun openRepoInternal() {
        if (isClosed.get()) throw IllegalStateException("Manager is closed")
        
        if (isGitRepo()) {
            git?.close()
            git = Git.open(rootDir)
            branchManager = GitBranchManager(git!!)
            stashManager = GitStashManager(git!!)
        } else {
            throw Exception("Not a Git repository at ${rootDir.absolutePath}")
        }
    }

    suspend fun configureUser(name: String, email: String) = withContext(Dispatchers.IO) {
        runGitOperation {
            val config = git?.repository?.config
            if (name.isNotEmpty()) config?.setString("user", null, "name", name)
            if (email.isNotEmpty()) config?.setString("user", null, "email", email)
            config?.save()
            "User config saved"
        }
    }

    suspend fun getDashboardStats(): DashboardState = withContext(Dispatchers.IO) {
        try {
            runSafeRead {
                val repo = git?.repository ?: return@runSafeRead DashboardState.Error("Repo closed")
                
                val status = git?.status()?.call()
                val changedCount = (status?.untracked?.size ?: 0) + 
                                   (status?.modified?.size ?: 0) + 
                                   (status?.added?.size ?: 0) + 
                                   (status?.missing?.size ?: 0) + 
                                   (status?.removed?.size ?: 0) +
                                   (status?.conflicting?.size ?: 0)
                
                val branchName = repo.branch ?: "Unknown"

                var unpushedCount = 0
                val remoteUrl = repo.config.getString("remote", "origin", "url")
                if (!remoteUrl.isNullOrEmpty()) {
                    runCatching {
                        val localHead = repo.resolve("HEAD")
                        val remoteBranch = "origin/$branchName"
                        val remoteHead = repo.resolve("refs/remotes/$remoteBranch")
                        
                        if (localHead != null) {
                            unpushedCount = if (remoteHead != null) {
                                git?.log()?.addRange(remoteHead, localHead)?.call()?.count() ?: 0
                            } else {
                                git?.log()?.call()?.count() ?: 0
                            }
                        }
                    }
                }
                DashboardState.Success(branchName, changedCount, unpushedCount)
            }
        } catch (e: Exception) {
            DashboardState.Error(e.message ?: "Unknown Error")
        }
    }

    suspend fun getChangedFiles(): List<GitFile> = withContext(Dispatchers.IO) {
        runSafeRead {
            val list = mutableListOf<GitFile>()
            try {
                val status = git?.status()?.call() ?: return@runSafeRead emptyList()
                status.added.forEach { list.add(GitFile(it, ChangeType.ADDED)) }
                status.modified.forEach { list.add(GitFile(it, ChangeType.MODIFIED)) }
                status.changed.forEach { list.add(GitFile(it, ChangeType.MODIFIED)) }
                status.untracked.forEach { list.add(GitFile(it, ChangeType.UNTRACKED)) }
                status.missing.forEach { list.add(GitFile(it, ChangeType.MISSING)) }
                status.removed.forEach { list.add(GitFile(it, ChangeType.DELETED)) }
                status.conflicting.forEach { list.add(GitFile(it, ChangeType.MODIFIED)) }
                list.sortedBy { it.path }
            } catch (e: Exception) { emptyList() }
        }
    }

    suspend fun getDiff(selectedPaths: Set<String>): String = withContext(Dispatchers.IO) {
        runSafeRead {
            val repo = git?.repository ?: return@runSafeRead ""
            val out = ByteArrayOutputStream()
            val df = DiffFormatter(out)
            df.setRepository(repo)
            
            try {
                val headId = repo.resolve("HEAD^{tree}")
                val headTree = if (headId != null) {
                    CanonicalTreeParser(null, repo.newObjectReader(), headId)
                } else {
                    EmptyTreeIterator()
                }
                
                val workTree = FileTreeIterator(repo)
                val diffEntries = df.scan(headTree, workTree)
                
                val maxDiffSize = 100 * 1024 // Increased limit to 100KB, but strictly bounded in memory
                
                for (entry in diffEntries) {
                    if (selectedPaths.contains(entry.newPath) || selectedPaths.contains(entry.oldPath)) {
                        df.format(entry)
                        if (out.size() > maxDiffSize) {
                            break // Memory optimization: Halt appending if diff becomes uncontrollably large
                        }
                    }
                }
                
                val fullDiff = out.toString("UTF-8")
                // Truncate safely before sending to AI to prevent token limits
                if (fullDiff.length > 6000) {
                    fullDiff.take(6000) + "\n\n... [Diff truncated to save tokens]"
                } else {
                    fullDiff
                }
            } catch (e: Exception) {
                "Error extracting diff: ${e.message}"
            } finally {
                df.close()
            }
        }
    }

    suspend fun getRichBranches(): List<BranchModel> = withContext(Dispatchers.IO) {
        runSafeRead { branchManager?.getRichBranches() ?: emptyList() }
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
        runSafeRead { stashManager?.getStashList() ?: emptyList() }
    }

    suspend fun getCommits(limit: Int = 100, offset: Int = 0): List<CommitItem> = withContext(Dispatchers.IO) {
        runSafeRead {
            val repo = git?.repository ?: return@runSafeRead emptyList()

            val logs = try {
                val cmd = git?.log() ?: return@runSafeRead emptyList()
                if (limit > 0) cmd.setMaxCount(limit)
                if (offset > 0) cmd.setSkip(offset)
                cmd.call()
            } catch (e: NoHeadException) {
                return@runSafeRead emptyList()
            }

            val branch = repo.branch
            val remoteRef = if (branch != null) repo.resolve("refs/remotes/origin/$branch") else null
            var isSynced = false

            val commits = mutableListOf<CommitItem>()

            logs.forEach { rev ->
                val hash = rev.name
                if (!isSynced && remoteRef != null && (rev.id == remoteRef)) {
                    isSynced = true
                }
                val isPushed = isSynced

               commits.add(CommitItem(
                   message = rev.fullMessage.trim(),
                   author = rev.authorIdent.name ?: "Unknown",
                   date = java.util.Date.from(rev.authorIdent.whenAsInstant),
                   hash = hash.substring(0, 7),
                   isPushed = isPushed
               ))
            }
            commits
        }
    }

    suspend fun getLastCommitMessage(): String = withContext(Dispatchers.IO) {
        runSafeRead {
             try {
                 git?.log()?.setMaxCount(1)?.call()?.firstOrNull()?.fullMessage ?: ""
             } catch (e: NoHeadException) { "" }
        }
    }

    suspend fun addToStage(files: List<GitFile>) = withContext(Dispatchers.IO) {
        runGitOperation {
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
            "Stage Updated"
        }
    }

    suspend fun commit(message: String, amend: Boolean = false): String = withContext(Dispatchers.IO) {
        runGitOperation {
            git?.commit()?.setMessage(message)?.setAmend(amend)?.call()
            if (amend) "Commit amended!" else "Committed!"
        }
    }

    suspend fun checkoutCommit(hash: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            git?.checkout()?.setName(hash)?.call()
            "Checked out $hash"
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

    suspend fun resetToCommit(hash: String, hard: Boolean): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val objId = git?.repository?.resolve(hash) ?: throw Exception("Not found")
            val mode = if (hard) ResetCommand.ResetType.HARD else ResetCommand.ResetType.MIXED
            git?.reset()?.setMode(mode)?.setRef(objId.name)?.call()
            if (hard) "Reset HARD" else "Reset MIXED"
        }
    }

    suspend fun hasRemote(): Boolean = withContext(Dispatchers.IO) {
        runSafeRead {
             !git?.repository?.config?.getString("remote", "origin", "url").isNullOrEmpty()
        }
    }

    suspend fun getRemoteUrl(): String = withContext(Dispatchers.IO) {
        runSafeRead { git?.repository?.config?.getString("remote", "origin", "url") ?: "" }
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

    suspend fun fetchAll(auth: GitAuthConfig): String = withContext(Dispatchers.IO) {
        runGitOperation {
            withAuth(auth) {
                val cmd = git?.fetch()?.setCheckFetchedObjects(true)
                applyCredentials(cmd, auth)
                cmd?.call()
                "Fetched all"
            }
        }
    }

    suspend fun push(auth: GitAuthConfig, force: Boolean = false): String = withContext(Dispatchers.IO) {
        runGitOperation {
            withAuth(auth) {
                val cmd = git?.push()?.setForce(force)
                applyCredentials(cmd, auth)

                val pushResults = cmd?.call()
                var resultMessage = ""

                pushResults?.forEach { result ->
                    result.remoteUpdates.forEach { update ->
                        when (update.status) {
                            RemoteRefUpdate.Status.OK -> resultMessage += "Success: ${update.srcRef} -> ${update.remoteName}\n"
                            RemoteRefUpdate.Status.UP_TO_DATE -> resultMessage += "Up to date.\n"
                            RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD -> resultMessage += "Rejected: Non-fast-forward (Pull first!)\n"
                            RemoteRefUpdate.Status.REJECTED_NODELETE -> resultMessage += "Rejected: No Delete.\n"
                            RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED -> resultMessage += "Rejected: Remote Changed.\n"
                            RemoteRefUpdate.Status.REJECTED_OTHER_REASON -> resultMessage += "Rejected: ${update.message}\n"
                            else -> resultMessage += "Status: ${update.status}\n"
                        }
                    }
                }
                if (resultMessage.isEmpty()) "Push executed (No updates)." else resultMessage.trim()
            }
        }
    }

    suspend fun pull(auth: GitAuthConfig): String = withContext(Dispatchers.IO) {
        runGitOperation {
            withAuth(auth) {
                val cmd = git?.pull()
                applyCredentials(cmd, auth)
                cmd?.call()
                "Pulled!"
            }
        }
    }

    suspend fun getConflictingFiles(): List<String> = withContext(Dispatchers.IO) {
        runSafeRead {
            val status = git?.status()?.call()
            status?.conflicting?.toList() ?: emptyList()
        }
    }
    
    suspend fun resolveUsingOurs(path: String) = withContext(Dispatchers.IO) {
        runGitOperation {
             git?.checkout()?.setStage(org.eclipse.jgit.api.CheckoutCommand.Stage.OURS)?.addPath(path)?.call()
             git?.add()?.addFilepattern(path)?.call() 
             "Resolved (Ours)"
         }
    }

    suspend fun resolveUsingTheirs(path: String) = withContext(Dispatchers.IO) {
        runGitOperation {
             git?.checkout()?.setStage(org.eclipse.jgit.api.CheckoutCommand.Stage.THEIRS)?.addPath(path)?.call()
             git?.add()?.addFilepattern(path)?.call() 
             "Resolved (Theirs)"
         }
    }
    
    suspend fun readFileContent(path: String): String = withContext(Dispatchers.IO) {
         try {
             val file = File(rootDir, path)
             if (!file.exists()) return@withContext "File not found."
             if (isBinaryFile(file)) {
                 return@withContext "Binary file detected (Image/PDF/Exec). \nCannot display content."
             }

             val maxLength = 50 * 1024 // 50KB limit to prevent Memory exhaustion
             // Memory optimization: using BufferedReader to read chunk by chunk instead of file.readText()
             file.bufferedReader().use { reader ->
                 val buffer = CharArray(maxLength)
                 val charsRead = reader.read(buffer, 0, maxLength)
                 
                 if (charsRead == -1) return@withContext ""
                 
                 val content = String(buffer, 0, charsRead)
                 // Check if there is still more content to read
                 if (reader.ready() || file.length() > maxLength) {
                     "$content\n\n... [File truncated because it is too large] ..."
                 } else {
                     content
                 }
             }
         } catch (e: Exception) { 
             "Error reading file: ${e.message}" 
         }
    }

    private fun isBinaryFile(file: File): Boolean {
        val name = file.name.lowercase()
        // Fast extension check
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
            name.endsWith(".gif") || name.endsWith(".pdf") || name.endsWith(".zip") ||
            name.endsWith(".apk") || name.endsWith(".jar") || name.endsWith(".webp") ||
            name.endsWith(".exe") || name.endsWith(".so")) {
            return true
        }

        // Fallback to content check
        return try {
            file.inputStream().use { ins ->
                val buffer = ByteArray(500)
                val read = ins.read(buffer)
                for (i in 0 until read) {
                    if (buffer[i].toInt() == 0) return true
                }
                false
            }
        } catch (e: Exception) { false }
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

    private suspend fun ensureOpen() {
        if (isClosed.get()) throw IllegalStateException("Manager is closed")
        if (git == null) {
            // Double-checked locking to prevent multiple initializations
            lifecycleMutex.withLock {
                if (git == null && !isClosed.get()) {
                    openRepoInternal()
                }
            }
        }
    }

    private suspend inline fun runGitOperation(crossinline block: suspend () -> String): String {
        return try {
            ensureOpen()
            // Write operations are exclusively locked
            writeMutex.withLock {
                block()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message ?: "Unknown"}"
        }
    }
    
    private fun <T> withAuth(auth: GitAuthConfig, block: () -> T): T {
        if (auth.mode == GitAuthMode.SSH) {
            authManager.configureSsh(auth)
        }
        return try {
            block()
        } finally {
            if (auth.mode == GitAuthMode.SSH) {
                authManager.closeActiveSshFactory()
            }
        }
    }

    private fun applyCredentials(command: Any?, auth: GitAuthConfig) {
        if (auth.mode != GitAuthMode.HTTPS || auth.token.isBlank()) return
        when (command) {
            is org.eclipse.jgit.api.FetchCommand -> command.setCredentialsProvider(authManager.getCredentialsProvider(auth.token))
            is org.eclipse.jgit.api.PushCommand -> command.setCredentialsProvider(authManager.getCredentialsProvider(auth.token))
            is org.eclipse.jgit.api.PullCommand -> command.setCredentialsProvider(authManager.getCredentialsProvider(auth.token))
            is org.eclipse.jgit.api.CloneCommand -> command.setCredentialsProvider(authManager.getCredentialsProvider(auth.token))
        }
    }

    private suspend inline fun <T> runSafeRead(crossinline block: suspend () -> T): T {
        ensureOpen()
        // Read operations can happen concurrently, bypassing the writeMutex
        try {
            return block()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                authManager.closeActiveSshFactory()
                git?.close()
                git = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        suspend fun cloneRepo(
            url: String,
            parentDir: File,
            folderName: String,
            auth: GitAuthConfig,
            secureStorageDir: File,
            onProgress: (String, Float, String) -> Unit
        ): Pair<File?, String> = withContext(Dispatchers.IO) {
            val authManager = GitAuthManager(secureStorageDir)
            val destDir = File(parentDir, folderName)
            if (destDir.exists() && destDir.listFiles()?.isNotEmpty() == true) {
                return@withContext Pair(null, "Error: Folder exists.")
            }

            val monitor = object : ProgressMonitor {
                private var totalTasks = 0
                private var completedTasks = 0
                private var currentTask = ""

                override fun start(totalTasks: Int) {}
                override fun beginTask(title: String?, totalWork: Int) {
                    currentTask = title ?: ""
                    totalTasks = totalWork
                    completedTasks = 0
                    onProgress(currentTask, 0f, "")
                }
                override fun update(completed: Int) {
                    completedTasks += completed
                    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
                    val details = if (totalTasks > 0) "$completedTasks / $totalTasks" else "$completedTasks"
                    onProgress(currentTask, progress, details)
                }
                override fun endTask() {
                    onProgress(currentTask, 1f, "")
                }
                override fun isCancelled(): Boolean = false
                
                // Added to satisfy JGit 7+ interface requirements
                override fun showDuration(enabled: Boolean) {}
            }

            try {
                if (auth.mode == GitAuthMode.SSH) {
                    authManager.configureSsh(auth)
                }

                val cmd = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(destDir)
                    .setProgressMonitor(monitor)

                if (auth.mode == GitAuthMode.HTTPS && auth.token.isNotBlank()) {
                    cmd.setCredentialsProvider(authManager.getCredentialsProvider(auth.token))
                }
                cmd.call().close()
                Pair(destDir, "Cloned!")
            } catch (e: Exception) {
                if (destDir.exists()) destDir.deleteRecursively()
                Pair(null, "Clone failed: ${e.message}")
            } finally {
                if (auth.mode == GitAuthMode.SSH) {
                    authManager.closeActiveSshFactory()
                }
            }
        }
    }
}