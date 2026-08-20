package com.android.git.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitRemoteUrlTest {
    @Test
    fun `accepts github scp style ssh url`() {
        val parsed = GitRemoteUrl.parse("git@github.com:RHineIx/AndroidGit.git")
        assertEquals(GitRemoteScheme.SSH, parsed?.scheme)
        assertEquals("RHineIx/AndroidGit.git", parsed?.path)
        assertTrue(GitRemoteUrl.isSsh("git@github.com:RHineIx/AndroidGit.git"))
    }

    @Test
    fun `converts github https url to ssh`() {
        assertEquals(
            "git@github.com:RHineIx/AndroidGit.git",
            GitRemoteUrl.toSshUrl("https://github.com/RHineIx/AndroidGit.git")
        )
    }

    @Test
    fun `accepts ssh scheme`() {
        assertTrue(GitRemoteUrl.isValid("ssh://git@github.com/RHineIx/AndroidGit.git"))
        assertTrue(GitRemoteUrl.isSsh("ssh://git@github.com/RHineIx/AndroidGit.git"))
    }

    @Test
    fun `rejects malformed remote`() {
        assertFalse(GitRemoteUrl.isValid("not-a-git-url"))
        assertEquals(null, GitRemoteUrl.toSshUrl("https://example.com/repository.git"))
    }
}
