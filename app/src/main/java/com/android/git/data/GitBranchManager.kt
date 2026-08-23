package com.android.git.data

import com.android.git.model.BranchModel
import com.android.git.model.BranchType
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevWalk

/**
 * Owns all branch-specific Git operations.
 *
 * The UI must pass a full ref whenever it has one. A display name is accepted
 * for backwards compatibility, but ambiguous remote names are rejected rather
 * than silently choosing the first match.
 */
class GitBranchManager(private val git: Git) {

    fun getRichBranches(): List<BranchModel> {
        val repository = git.repository
        val currentRef = repository.fullBranch?.takeIf { it.startsWith(LOCAL_PREFIX) }
        val refs = git.branchList()
            .setListMode(ListBranchCommand.ListMode.ALL)
            .call()

        return refs.filterNot { it.isSymbolic }.map { ref ->
            if (ref.name.startsWith(REMOTE_PREFIX)) {
                BranchModel(
                    name = logicalBranchName(ref.name),
                    fullPath = ref.name,
                    type = BranchType.REMOTE,
                    isCurrent = false,
                    remoteName = remoteName(ref.name)
                )
            } else {
                val localName = logicalBranchName(ref.name)
                val tracking = runCatching { trackingInfo(localName) }.getOrDefault(TrackingInfo())
                BranchModel(
                    name = localName,
                    fullPath = ref.name,
                    type = BranchType.LOCAL,
                    isCurrent = ref.name == currentRef,
                    trackingName = tracking.displayName,
                    aheadCount = tracking.ahead,
                    behindCount = tracking.behind,
                    isUpstreamGone = tracking.isGone
                )
            }
        }
            .distinctBy { it.fullPath }
            .sortedWith(
                compareBy<BranchModel>({ !it.isCurrent }, { it.type }, { it.name }, { it.remoteName.orEmpty() })
            )
    }

    fun createBranch(branchName: String): String {
        val name = validateBranchName(branchName)
        git.branchCreate().setName(name).call()
        return "Created branch $name"
    }

    fun deleteBranch(branchRefOrName: String): String {
        val name = requireLocalBranchName(branchRefOrName)
        val currentRef = git.repository.fullBranch
        if (currentRef == "$LOCAL_PREFIX$name") {
            throw IllegalStateException("Cannot delete the active branch.")
        }
        if (git.repository.findRef("$LOCAL_PREFIX$name") == null) {
            throw NoSuchElementException("Local branch not found: $name")
        }

        // Never force-delete by default. Git will protect an unmerged branch.
        git.branchDelete()
            .setBranchNames(name)
            .setForce(false)
            .call()
        return "Deleted branch $name"
    }

    fun forceDeleteBranch(branchRefOrName: String): String {
        val name = requireLocalBranchName(branchRefOrName)
        if (git.repository.fullBranch == "$LOCAL_PREFIX$name") {
            throw IllegalStateException("Cannot delete the active branch.")
        }
        if (git.repository.findRef("$LOCAL_PREFIX$name") == null) {
            throw NoSuchElementException("Local branch not found: $name")
        }

        git.branchDelete()
            .setBranchNames(name)
            .setForce(true)
            .call()
        return "Force-deleted branch $name"
    }

    fun checkoutBranch(branchRefOrName: String): String {
        val requested = branchRefOrName.trim()
        require(requested.isNotEmpty()) { "Branch name cannot be empty." }

        val repository = git.repository
        val localRef = resolveLocalRef(requested)
        if (localRef != null) {
            val localName = logicalBranchName(localRef.name)
            if (repository.fullBranch == localRef.name) return "Already on $localName"
            git.checkout().setName(localName).call()
            return "Switched to $localName"
        }

        val remoteRef = resolveRemoteRef(requested)
            ?: throw NoSuchElementException("Branch not found: $requested")
        val remoteBranchName = logicalBranchName(remoteRef.name)
        val existingLocal = repository.findRef("$LOCAL_PREFIX$remoteBranchName")
        if (existingLocal != null) {
            git.checkout().setName(remoteBranchName).call()
            return "Switched to $remoteBranchName"
        }

        git.checkout()
            .setCreateBranch(true)
            .setName(remoteBranchName)
            .setStartPoint(remoteRef.name)
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            .call()
        return "Switched to $remoteBranchName"
    }

    fun renameBranch(newName: String): String {
        val name = validateBranchName(newName)
        val currentRef = git.repository.fullBranch
        if (currentRef == null || !currentRef.startsWith(LOCAL_PREFIX)) {
            throw IllegalStateException("Cannot rename a branch while HEAD is detached.")
        }
        if (logicalBranchName(currentRef) == name) {
            return "Branch is already named $name"
        }
        git.branchRename().setNewName(name).call()
        return "Renamed to $name"
    }

    fun mergeBranch(branchRefOrName: String): String {
        ensureCleanWorktree("merge")
        val ref = resolveBranchRef(branchRefOrName)
            ?: throw NoSuchElementException("Branch not found: ${branchRefOrName.trim()}")
        ensureNotCurrent(ref, "merge")

        val result = git.merge().include(ref).call()
        return if (result.mergeStatus.isSuccessful) {
            "Merged ${logicalBranchName(ref.name)} (${result.mergeStatus})"
        } else {
            "Merge failed: ${result.mergeStatus}"
        }
    }

    fun rebaseBranch(branchRefOrName: String): String {
        ensureCleanWorktree("rebase")
        val ref = resolveBranchRef(branchRefOrName)
            ?: throw NoSuchElementException("Branch not found: ${branchRefOrName.trim()}")
        ensureNotCurrent(ref, "rebase")

        val result = git.rebase().setUpstream(ref.objectId).call()
        return if (result.status.isSuccessful) {
            "Rebased onto ${logicalBranchName(ref.name)} (${result.status})"
        } else {
            "Rebase failed: ${result.status}"
        }
    }

    private fun resolveBranchRef(branchRefOrName: String): Ref? {
        val requested = branchRefOrName.trim()
        if (requested.isEmpty()) return null

        val repository = git.repository
        return repository.findRef(requested)?.takeIf { isBranchRef(it.name) }
            ?: resolveLocalRef(requested)
            ?: resolveRemoteRef(requested)
    }

    private fun resolveLocalRef(branchRefOrName: String): Ref? {
        val requested = branchRefOrName.trim()
        val localName = when {
            requested.startsWith(LOCAL_PREFIX) -> requested.removePrefix(LOCAL_PREFIX)
            requested.startsWith(REMOTE_PREFIX) -> return null
            else -> requested
        }
        if (localName.isEmpty()) return null
        return git.repository.findRef("$LOCAL_PREFIX$localName")
    }

    private fun resolveRemoteRef(branchRefOrName: String): Ref? {
        val requested = branchRefOrName.trim()
        if (requested.isEmpty()) return null

        val remoteRefs = git.branchList()
            .setListMode(ListBranchCommand.ListMode.REMOTE)
            .call()
        val exact = remoteRefs.firstOrNull { it.name == requested }
        if (exact != null) return exact

        val candidates = remoteRefs.filter { ref ->
            val qualifiedName = ref.name.removePrefix(REMOTE_PREFIX)
            qualifiedName == requested || logicalBranchName(ref.name) == requested
        }
        if (candidates.size > 1) {
            val choices = candidates.joinToString(", ") { it.name.removePrefix(REMOTE_PREFIX) }
            throw IllegalArgumentException("Ambiguous remote branch '$requested'. Choose one of: $choices")
        }
        return candidates.singleOrNull()
    }

    private fun requireLocalBranchName(branchRefOrName: String): String {
        val requested = branchRefOrName.trim()
        require(requested.isNotEmpty()) { "Branch name cannot be empty." }
        if (requested.startsWith(REMOTE_PREFIX)) {
            throw IllegalArgumentException("Remote branches cannot be deleted locally.")
        }
        val name = requested.removePrefix(LOCAL_PREFIX)
        validateBranchName(name)
        return name
    }

    private fun ensureCleanWorktree(operation: String) {
        if (git.status().call().hasUncommittedChanges()) {
            throw IllegalStateException("Commit or stash your changes before $operation.")
        }
    }

    private fun ensureNotCurrent(ref: Ref, operation: String) {
        val current = git.repository.fullBranch
        if (ref.name == current || ref.objectId == current?.let { git.repository.resolve(it) }) {
            throw IllegalArgumentException("Cannot $operation the current branch onto itself.")
        }
    }

    private data class TrackingInfo(
        val displayName: String? = null,
        val ahead: Int = 0,
        val behind: Int = 0,
        val isGone: Boolean = false
    )

    private fun trackingInfo(localName: String): TrackingInfo {
        val config = git.repository.config
        val remote = config.getString("branch", localName, "remote") ?: return TrackingInfo()
        val merge = config.getString("branch", localName, "merge") ?: return TrackingInfo()
        val mergeName = merge.removePrefix(LOCAL_PREFIX)
        val trackingRef = if (remote == ".") {
            merge
        } else {
            "$REMOTE_PREFIX$remote/$mergeName"
        }
        val upstream = git.repository.findRef(trackingRef)
            ?: return TrackingInfo(
                displayName = if (remote == ".") mergeName else "$remote/$mergeName",
                isGone = true
            )
        val local = git.repository.findRef("$LOCAL_PREFIX$localName") ?: return TrackingInfo()
        val ahead = countReachable(local.objectId, upstream.objectId)
        val behind = countReachable(upstream.objectId, local.objectId)
        return TrackingInfo(
            displayName = if (remote == ".") mergeName else "$remote/$mergeName",
            ahead = ahead,
            behind = behind
        )
    }

    private fun countReachable(start: ObjectId, excluded: ObjectId): Int {
        RevWalk(git.repository).use { walk ->
            walk.markStart(walk.parseCommit(start))
            walk.markUninteresting(walk.parseCommit(excluded))
            var count = 0
            while (walk.next() != null) count++
            return count
        }
    }

    private fun isBranchRef(refName: String): Boolean =
        refName.startsWith(LOCAL_PREFIX) || refName.startsWith(REMOTE_PREFIX)

    companion object {
        private const val LOCAL_PREFIX = "refs/heads/"
        private const val REMOTE_PREFIX = "refs/remotes/"

        internal fun validateBranchName(branchName: String): String {
            val name = branchName.trim()
            require(name.isNotEmpty()) { "Branch name cannot be empty." }
            require(name != "HEAD") { "HEAD is reserved and cannot be used as a branch name." }
            require(!name.startsWith("-") && !name.startsWith("/") && !name.endsWith("/")) {
                "Branch name cannot start with '-' or contain a leading/trailing '/'."
            }
            require(!name.endsWith(".") && !name.contains("..")) {
                "Branch name cannot end with '.' or contain '..'."
            }
            require(name.split('/').none { it.endsWith(".lock", ignoreCase = true) }) {
                "Branch name components cannot end with '.lock'."
            }
            require(!name.contains("@{")) { "Branch name cannot contain '@{'." }
            require(name.none { it.isISOControl() || it == '~' || it == '^' || it == ':' || it == '?' || it == '*' || it == '[' || it == '\\' }) {
                "Branch name contains characters that Git does not allow."
            }
            return name
        }

        internal fun logicalBranchName(refName: String): String {
            return when {
                refName.startsWith(LOCAL_PREFIX) -> refName.removePrefix(LOCAL_PREFIX)
                refName.startsWith(REMOTE_PREFIX) -> {
                    refName.removePrefix(REMOTE_PREFIX).substringAfter('/', "")
                }
                else -> refName
            }
        }

        private fun remoteName(refName: String): String? {
            if (!refName.startsWith(REMOTE_PREFIX)) return null
            return refName.removePrefix(REMOTE_PREFIX).substringBefore('/').ifBlank { null }
        }
    }
}
