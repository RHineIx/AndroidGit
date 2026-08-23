package com.android.git.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitBranchManagerTest {
    @Test
    fun `remote branch keeps all slash-separated path segments`() {
        assertEquals(
            "feature/shared-shopping-list",
            GitBranchManager.logicalBranchName("refs/remotes/origin/feature/shared-shopping-list")
        )
    }

    @Test
    fun `local branch keeps slash-separated path segments`() {
        assertEquals(
            "feature/shared-shopping-list",
            GitBranchManager.logicalBranchName("refs/heads/feature/shared-shopping-list")
        )
    }

    @Test
    fun `remote tracking name removes only remote name`() {
        assertEquals(
            "bugfix/api/v2/login",
            GitBranchManager.logicalBranchName("refs/remotes/upstream/bugfix/api/v2/login")
        )
    }

    @Test
    fun `valid branch names are trimmed and preserved`() {
        assertEquals("feature/login", GitBranchManager.validateBranchName("  feature/login  "))
        assertEquals("release/2026.08", GitBranchManager.validateBranchName("release/2026.08"))
    }

    @Test
    fun `invalid branch names are rejected before invoking JGit`() {
        val invalidNames = listOf(
            "",
            "   ",
            "HEAD",
            "feature..login",
            "feature/@{broken}",
            "feature~login",
            "feature:login",
            "/feature",
            "feature/",
            "feature.lock"
        )

        invalidNames.forEach { name ->
            val rejected = runCatching { GitBranchManager.validateBranchName(name) }.isFailure
            assertTrue("Expected '$name' to be rejected", rejected)
        }
    }
}
