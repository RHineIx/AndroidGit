package com.android.git.data

import java.io.File
import org.apache.sshd.common.util.io.PathUtils
import org.eclipse.jgit.transport.SshSessionFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitAuthManagerTest {
    @Test
    fun `generated preferred key is in OpenSSH and GitHub public key formats`() {
        withTemporaryDirectory { directory ->
            val generated = GitAuthManager(directory).generateKeyPair(
                passphrase = "",
                email = "androidgit-test",
                preferredAlgorithm = SshKeyAlgorithm.ED25519
            )

            assertTrue(generated.privateKey.startsWith("-----BEGIN OPENSSH PRIVATE KEY-----"))
            assertTrue(generated.privateKey.contains("-----END OPENSSH PRIVATE KEY-----"))
            assertTrue(generated.publicKey.startsWith("ssh-ed25519 "))
            assertTrue(generated.publicKey.endsWith(" androidgit-test"))
            assertEquals("Ed25519", generated.algorithm)
        }
    }

    @Test
    fun `rsa fallback generates a GitHub-compatible OpenSSH key`() {
        withTemporaryDirectory { directory ->
            val generated = GitAuthManager(directory).generateKeyPair(
                passphrase = "test-passphrase",
                email = "androidgit-rsa-test",
                preferredAlgorithm = SshKeyAlgorithm.RSA_4096
            )

            assertTrue(generated.privateKey.startsWith("-----BEGIN OPENSSH PRIVATE KEY-----"))
            assertTrue(generated.privateKey.contains("-----END OPENSSH PRIVATE KEY-----"))
            assertTrue(generated.publicKey.startsWith("ssh-rsa "))
            assertTrue(generated.publicKey.endsWith(" androidgit-rsa-test"))
            assertEquals("RSA-4096", generated.algorithm)
        }
    }

    @Test
    fun `configure ssh sets an app private user home`() {
        withTemporaryDirectory { directory ->
            val manager = GitAuthManager(directory)
            val generated = manager.generateKeyPair(preferredAlgorithm = SshKeyAlgorithm.RSA_4096)

            manager.configureSsh(
                GitAuthConfig(
                    mode = GitAuthMode.SSH,
                    privateKey = generated.privateKey
                )
            )

            assertEquals(
                File(directory, ".androidgit-home").toPath(),
                PathUtils.getUserHomeFolder()
            )
            val knownHosts = File(directory, ".androidgit-ssh/known_hosts").readText()
            assertTrue(knownHosts.contains("github.com ssh-ed25519"))
            assertTrue(knownHosts.contains("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQCj7ndNxQowgcQnjshcLrqPEiiphnt"))
            manager.closeActiveSshFactory()
        }
    }

    @Test
    fun `same ssh configuration reuses an active session factory`() {
        withTemporaryDirectory { directory ->
            val manager = GitAuthManager(directory)
            val generated = manager.generateKeyPair(preferredAlgorithm = SshKeyAlgorithm.ED25519)
            val config = GitAuthConfig(
                mode = GitAuthMode.SSH,
                privateKey = generated.privateKey
            )

            val first = manager.configureSsh(config)
            val second = manager.configureSsh(config)

            assertTrue(first === second)
            assertTrue(SshSessionFactory.getInstance() === first)
            manager.closeActiveSshFactory()
            assertTrue(SshSessionFactory.getInstance() !== first)
        }
    }

    @Test
    fun `sshd KeyUtils is available to the JVM test runtime`() {
        val keyUtils = Class.forName("org.apache.sshd.common.config.keys.KeyUtils")
        assertNotNull(keyUtils)
    }

    private fun withTemporaryDirectory(block: (File) -> Unit) {
        val directory = File(System.getProperty("java.io.tmpdir"), "androidgit-key-test-${System.nanoTime()}")
            .apply { mkdirs() }
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
