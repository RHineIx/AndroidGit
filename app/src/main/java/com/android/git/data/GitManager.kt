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
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A robust, thread-safe Git Manager.
 * Optimized for Concurrency: Separates Index-Write locks from Read-only operations.
 */
class GitManager(private val rootDir: File) : Closeable {

    private var git: Git? = null
    private var branchManager: GitBranchManager? = null
    private var stashManager: GitStashManager? = null
    
    // Mutex specifically for operations that modify the Working Tree or Index (Staging Area).
    // e.g., Checkout, Merge, Commit, Add, Pull, Status (updates index cache).
    private val writeMutex = Mutex()
    
    private val isClosed = AtomicBoolean(false)

    val authManager = GitAuthManager(rootDir.parentFile ?: rootDir)

    fun isGitRepo(): Boolean = File(rootDir, ".git").exists()

    // --- Initialization ---

    suspend fun initRepo(): String = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            runCatching {
                Git.init().setDirectory(rootDir).call()
                openRepoInternal()
                "Repository initialized successfully!"
            }.getOrElse { "Failed to init: ${it.message}" }
        }
    }

    suspend fun openRepo(): String = withContext(Dispatchers.IO) {
        // Opening usually doesn't need a write lock, but we want to ensure atomicity during setup
        writeMutex.withLock {
            runCatching {
                openRepoInternal()
                "Repository opened successfully!"
            }.getOrElse { "Failed to open: ${it.message}" }
        }
    }

    private fun openRepoInternal() {
        if (isClosed.get()) throw IllegalStateException("Manager is closed")
        
        if (isGitRepo()) {
            git?.close() // Close old instance if exists
            git = Git.open(rootDir)
            branchManager = GitBranchManager(git!!)
            stashManager = GitStashManager(git!!)
        } else {
            throw Exception("Not a Git repository at ${rootDir.absolutePath}")
        }
    }

    suspend fun configureUser(name: String, email: String) = withContext(Dispatchers.IO) {
        // Config writes are safe to do quickly
        writeMutex.withLock {
            ensureOpen()
            val config = git?.repository?.config
            if (name.isNotEmpty()) config?.setString("user", null, "name", name)
            if (email.isNotEmpty()) config?.setString("user", null, "email", email)
            config?.save()
        }
    }

    // --- Dashboard & Status (READ + WRITE MIXED) ---

    /**
     * Optimized:
     * - `status` needs Write Lock (updates index).
     * - `log` (unpushed count) is Read-only (safe).
     */
    suspend fun getDashboardStats(): DashboardState = withContext(Dispatchers.IO) {
        try {
            ensureOpen()
            val repo = git?.repository ?: return@withContext DashboardState.Error("Repo closed")
            
            // 1. Status (Requires Lock to be accurate and safe)
            val (changedCount, branchName) = writeMutex.withLock {
                val status = git?.status()?.call()
                val count = (status?.untracked?.size ?: 0) + 
                             (status?.modified?.size ?: 0) + 
                             (status?.added?.size ?: 0) + 
                             (status?.missing?.size ?: 0) + 
                             (status?.removed?.size ?: 0) +
                             (status?.conflicting?.size ?: 0)
                Pair(count, repo.branch ?: "Unknown")
            }

            // 2. Unpushed Commits (Read-Only, JGit allows concurrent object database access)
            var unpushedCount = 0
            if (hasRemoteInternal()) {
                runCatching {
                    val localHead = repo.resolve("HEAD")
                    val remoteBranch = "origin/$branchName"
                    val remoteHead = repo.resolve("refs/remotes/$remoteBranch")
                    
                    if (localHead != null) {
                        unpushedCount = if (remoteHead != null) {
                            // Only counting commits, no index modification -> No Mutex needed
                            git?.log()?.addRange(remoteHead, localHead)?.call()?.count() ?: 0
                        } else {
                            git?.log()?.call()?.count() ?: 0
                        }
                    }
                }
            }
            DashboardState.Success(branchName, changedCount, unpushedCount)
        } catch (e: Exception) {
            DashboardState.Error(e.message ?: "Unknown Error")
        }
    }

    suspend fun getChangedFiles(): List<GitFile> = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val list = mutableListOf<GitFile>()
            try {
                ensureOpen()
                val status = git?.status()?.call() ?: return@withLock emptyList()
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

    // --- Branch Operations (READ vs WRITE) ---

    // READ-ONLY: No Mutex needed (JGit reads packed-refs safely)
    suspend fun getRichBranches(): List<BranchModel> = withContext(Dispatchers.IO) {
        runSafeRead { branchManager?.getRichBranches() ?: emptyList() }
    }

    // WRITE: Requires Mutex
    suspend fun checkoutBranch(name: String): String = withContext(Dispatchers.IO) {
        writeMutex.withLock { runGitOperation { branchManager?.checkoutBranch(name) ?: "Error" } }
    }

    suspend fun createBranch(name: String): String = withContext(Dispatchers.IO) {
        writeMutex.withLock { runGitOperation { branchManager?.createBranch(name) ?: "Error" } }
    }

    suspend fun deleteBranch(name: String): String = withContext(Dispatchers.IO) {
        writeMutex.withLock { runGitOperation { branchManager?.deleteBranch(name) ?: "Error" } }
    }

    suspend fun mergeBranch(name: String): String = withContext(Dispatchers.IO) {
        writeMutex.withLock { runGitOperation { branchManager?.mergeBranch(name) ?: "Error" } }
    }

    suspend fun rebaseBranch(name: String): String = withContext(Dispatchers.IO) {
        writeMutex.withLock { runGitOperation { branchManager?.rebaseBranch(name) ?: "Error" } }
    }

    suspend fun renameBranch(name: String): String = withContext(Dispatchers.IO) {
        writeMutex.withLock { runGitOperation { branchManager?.renameBranch(name) ?: "Error" } }
    }

    // --- Stash (WRITE) ---

    suspend fun stashChanges(msg: String): String = withContext(Dispatchers.IO) {
        writeMutex.withLock { runGitOperation { stashManager?.stashChanges(msg) ?: "Error" } }
    }

    suspend fun applyStash(index: Int, drop: Boolean): String = withContext(Dispatchers.IO) {
        writeMutex.withLock { runGitOperation { stashManager?.applyStash(index, drop) ?: "Error" } }
    }

    suspend fun dropStash(index: Int): String = withContext(Dispatchers.IO) {
        writeMutex.withLock { runGitOperation { stashManager?.dropStash(index) ?: "Error" } }
    }

    // READ-ONLY
    suspend fun getStashList(): List<StashItem> = withContext(Dispatchers.IO) {
        runSafeRead { stashManager?.getStashList() ?: emptyList() }
    }

    // --- Commits & Log (READ vs WRITE) ---

    // READ-ONLY: Pure history reading
    suspend fun getCommits(): List<CommitItem> = withContext(Dispatchers.IO) {
        runSafeRead {
            val repo = git?.repository ?: return@runSafeRead emptyList()
            val logs = git?.log()?.call() ?: return@runSafeRead emptyList()

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
                    date = rev.authorIdent.`when`,
                    hash = hash.substring(0, 7),
                    isPushed = isPushed
                ))
            }
            commits
        }
    }

    // READ-ONLY
    suspend fun getLastCommitMessage(): String = withContext(Dispatchers.IO) {
        runSafeRead {
            git?.log()?.setMaxCount(1)?.call()?.firstOrNull()?.fullMessage ?: ""
        }
    }

    // WRITE: Modifies Index
    suspend fun addToStage(files: List<GitFile>) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            ensureOpen()
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
    }

    // WRITE: Creates Commit object and updates HEAD
    suspend fun commit(message: String, amend: Boolean = false): String = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            runGitOperation {
                git?.commit()?.setMessage(message)?.setAmend(amend)?.call()
                if (amend) "Commit amended!" else "Committed!"
            }
        }
    }

    // WRITE: Updates Index/WorkingDir
    suspend fun checkoutCommit(hash: String): String = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            runGitOperation {
                git?.checkout()?.setName(hash)?.call()
                "Checked out $hash"
            }
        }
    }

    suspend fun revertCommit(hash: String): String = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            runGitOperation {
                val objId = git?.repository?.resolve(hash) ?: throw Exception("Commit not found")
                git?.revert()?.include(objId)?.call()
                "Reverted commit"
            }
        }
    }

    suspend fun cherryPickCommit(hash: String): String = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            runGitOperation {
                val objId = git?.repository?.resolve(hash) ?: throw Exception("Commit not found")
                val res = git?.cherryPick()?.include(objId)?.call()
                if (res?.status == org.eclipse.jgit.api.CherryPickResult.CherryPickStatus.OK) "Cherry-picked!" else "Failed: ${res?.status}"
            }
        }
    }

    suspend fun resetToCommit(hash: String, hard: Boolean): String = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            runGitOperation {
                val objId = git?.repository?.resolve(hash) ?: throw Exception("Not found")
                val mode = if (hard) ResetCommand.ResetType.HARD else ResetCommand.ResetType.MIXED
                git?.reset()?.setMode(mode)?.setRef(objId.name)?.call()
                if (hard) "Reset HARD" else "Reset MIXED"
            }
        }
    }

    // --- Remote Operations ---

    suspend fun hasRemote(): Boolean = withContext(Dispatchers.IO) {
        hasRemoteInternal() // Fast config read, no lock needed usually
    }

    private fun hasRemoteInternal(): Boolean {
        ensureOpen()
        return !git?.repository?.config?.getString("remote", "origin", "url").isNullOrEmpty()
    }

    suspend fun getRemoteUrl(): String = withContext(Dispatchers.IO) {
        ensureOpen()
        git?.repository?.config?.getString("remote", "origin", "url") ?: ""
    }

    suspend fun addRemote(url: String): String = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            runGitOperation {
                val config = git?.repository?.config
                config?.setString("remote", "origin", "url", url)
                config?.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*")
                config?.save()
                "Remote linked!"
            }
        }
    }

    // Optimized: No Write Lock needed for simple Fetch
    suspend fun fetchAll(token: String): String = withContext(Dispatchers.IO) {
        runGitOperation {
            val cmd = git?.fetch()?.setCheckFetchedObjects(true)
            if (token.isNotEmpty()) cmd?.setCredentialsProvider(authManager.getCredentialsProvider(token))
            cmd?.call()
            "Fetched all"
        }
    }

    suspend fun push(token: String, force: Boolean = false): String = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            runGitOperation {
                val cmd = git?.push()?.setForce(force)
                if (token.isNotEmpty()) {
                    cmd?.setCredentialsProvider(authManager.getCredentialsProvider(token))
                }

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

    suspend fun pull(token: String): String = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            runGitOperation {
                val cmd = git?.pull()
                if (token.isNotEmpty()) cmd?.setCredentialsProvider(authManager.getCredentialsProvider(token))
                cmd?.call()
                "Pulled!"
            }
        }
    }

    // --- Conflict Resolution & Files ---

    suspend fun getConflictingFiles(): List<String> = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            try {
                ensureOpen()
                val status = git?.status()?.call()
                status?.conflicting?.toList() ?: emptyList()
            } catch (e: Exception) { emptyList() }
        }
    }
    
    suspend fun resolveUsingOurs(path: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
             runGitOperation {
                 git?.checkout()?.setStage(org.eclipse.jgit.api.CheckoutCommand.Stage.OURS)?.addPath(path)?.call()
                 git?.add()?.addFilepattern(path)?.call() 
                 "Resolved (Ours)"
             }
        }
    }

    suspend fun resolveUsingTheirs(path: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
             runGitOperation {
                 git?.checkout()?.setStage(org.eclipse.jgit.api.CheckoutCommand.Stage.THEIRS)?.addPath(path)?.call()
                 git?.add()?.addFilepattern(path)?.call() 
                 "Resolved (Theirs)"
             }
        }
    }
    
    suspend fun readFileContent(path: String): String = withContext(Dispatchers.IO) {
         try {
             val file = File(rootDir, path)
             if (!file.exists()) return@withContext "File not found."
             if (isBinaryFile(file)) {
                 return@withContext "Binary file detected (Image/PDF/Exec). \nCannot display content."
             }

             val maxLength = 50 * 1024 
             if (file.length() > maxLength) {
                 val content = file.reader().use { it.readText().take(maxLength) }
                 "$content\n\n... [File truncated because it is too large] ..."
             } else {
                 file.readText()
             }
         } catch (e: Exception) { 
             "Error reading file: ${e.message}" 
         }
    }

    // --- FIX: Correct Diff Logic ---
    suspend fun getFileDiff(path: String): String = withContext(Dispatchers.IO) {
        runSafeRead {
            val repo = git?.repository ?: return@runSafeRead "Error: Repo closed"
            val out = ByteArrayOutputStream()
            val df = DiffFormatter(out)
            df.setRepository(repo)
            df.setPathFilter(PathFilter.create(path))

            // 1. Check if Untracked or Added (Fix: using status.untracked.contains)
            val status = git?.status()?.addPath(path)?.call()
            if (status != null) {
                if (status.untracked.contains(path) || status.added.contains(path)) {
                    // Return whole file as added
                    return@runSafeRead readFileContent(path).lines().joinToString("\n") { "+$it" }
                }
            }

            // 2. Resolve HEAD tree
            val headId = repo.resolve("HEAD^{tree}")
            
            if (headId == null) {
                // No HEAD yet, everything is new
                 return@runSafeRead readFileContent(path).lines().joinToString("\n") { "+$it" }
            }

            // 3. Prepare Iterators (Fix: Use CanonicalTreeParser for HEAD)
            val reader = repo.newObjectReader()
            val oldTreeIter = CanonicalTreeParser()
            oldTreeIter.reset(reader, headId)
            
            // New: Working Tree
            val newTreeIter = FileTreeIterator(repo)

            // 4. Format Diff (CanonicalTreeParser vs FileTreeIterator is valid)
            df.format(oldTreeIter, newTreeIter)
            
            val diffText = out.toString("UTF-8")
            if (diffText.isBlank()) "No changes or binary file." else diffText
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

    private fun ensureOpen() {
        if (isClosed.get()) throw IllegalStateException("Manager closed")
        if (git == null) {
            openRepoInternal()
        }
    }

    private inline fun runGitOperation(block: () -> String): String {
        return try {
            ensureOpen()
            block()
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message ?: "Unknown"}"
        }
    }
    
    private inline fun <T> runSafeRead(block: () -> T): T {
        ensureOpen()
        return try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            git?.close()
            git = null
        }
    }

    companion object {
        suspend fun cloneRepo(url: String, parentDir: File, folderName: String, token: String): Pair<File?, String> = withContext(Dispatchers.IO) {
            val destDir = File(parentDir, folderName)
            if (destDir.exists() && destDir.listFiles()?.isNotEmpty() == true) {
                return@withContext Pair(null, "Error: Folder exists.")
            }
            try {
                val cmd = Git.cloneRepository().setURI(url).setDirectory(destDir)
                if (token.isNotEmpty()) {
                    cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
                }
                cmd.call().close() 
                Pair(destDir, "Cloned!")
            } catch (e: Exception) {
                if (destDir.exists()) destDir.deleteRecursively()
                Pair(null, "Clone failed: ${e.message}")
            }
        }
    }
}