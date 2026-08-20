package com.android.git.data

import org.junit.Assert.assertEquals
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
}
