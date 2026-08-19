package com.android.git.data

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class GitAuthManagerTest {
    @Test
    fun `generated key is in OpenSSH and GitHub public key formats`() {
        val directory = File(System.getProperty("java.io.tmpdir"), "androidgit-key-test-${System.nanoTime()}")
            .apply { mkdirs() }

        try {
            val generated = GitAuthManager(directory).generateKeyPair(
                passphrase = "",
                email = "androidgit-test"
            )

            assertTrue(generated.privateKey.startsWith("-----BEGIN OPENSSH PRIVATE KEY-----"))
            assertTrue(generated.privateKey.contains("-----END OPENSSH PRIVATE KEY-----"))
            assertTrue(generated.publicKey.startsWith("ssh-ed25519 "))
            assertTrue(generated.publicKey.endsWith(" androidgit-test"))
        } finally {
            directory.deleteRecursively()
        }
    }
}
