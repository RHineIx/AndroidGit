package com.android.git.data

import com.android.git.utils.isGitFailureMessage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitOperationResultTest {
    @Test
    fun `recognizes error and failed results`() {
        assertTrue(isGitFailureMessage("Error: repository is closed"))
        assertTrue(isGitFailureMessage("Failed to apply stash: conflict"))
    }

    @Test
    fun `recognizes rejected and conflict results`() {
        assertTrue(isGitFailureMessage("Rejected: Non-fast-forward"))
        assertTrue(isGitFailureMessage("Merge conflict detected"))
    }

    @Test
    fun `does not classify normal success as failure`() {
        assertFalse(isGitFailureMessage("Committed!"))
        assertFalse(isGitFailureMessage("Fetched all"))
        assertFalse(isGitFailureMessage("Stash applied"))
    }
}
