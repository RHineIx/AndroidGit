package com.android.git.data

import org.apache.sshd.common.config.keys.PublicKeyEntry
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyEncryptionContext
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.transport.sshd.KeyPasswordProvider
import org.eclipse.jgit.transport.sshd.SshdSessionFactory
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security

/** Authentication modes supported by GitHub and other Git remotes. */
enum class GitAuthMode {
    HTTPS,
    SSH
}

data class GitAuthConfig(
    val mode: GitAuthMode = GitAuthMode.HTTPS,
    val token: String = "",
    val privateKey: String = "",
    val passphrase: String = ""
)

data class GeneratedSshKey(
    val privateKey: String,
    val publicKey: String,
    val algorithm: String
)

class GitAuthManager(private val contextDir: File) {
    private var activeSshFactory: SshdSessionFactory? = null
    private var activeKeyFile: File? = null

    fun getCredentialsProvider(token: String): UsernamePasswordCredentialsProvider {
        require(token.isNotBlank()) { "A GitHub token is required for HTTPS authentication." }
        // GitHub accepts the token as the password; the username is ignored but must be non-empty.
        return UsernamePasswordCredentialsProvider("x-access-token", token)
    }

    @Synchronized
    fun configureSsh(config: GitAuthConfig): SshdSessionFactory {
        require(config.mode == GitAuthMode.SSH) { "SSH configuration requires SSH authentication mode." }
        require(config.privateKey.isNotBlank()) { "An SSH private key is required." }

        closeActiveSshFactory()

        val sshDirectory = File(contextDir, ".androidgit-ssh").apply { mkdirs() }
        val keyFile = File(sshDirectory, "id_androidgit")
        keyFile.writeText(config.privateKey.trimEnd() + "\n")
        keyFile.setReadable(false, false)
        keyFile.setReadable(true, true)
        keyFile.setWritable(false, false)
        keyFile.setWritable(true, true)

        activeKeyFile = keyFile

        val knownHostsFile = File(sshDirectory, "known_hosts")
        if (!knownHostsFile.exists()) {
            knownHostsFile.writeText(GITHUB_KNOWN_HOSTS)
            knownHostsFile.setReadable(false, false)
            knownHostsFile.setReadable(true, true)
            knownHostsFile.setWritable(false, false)
            knownHostsFile.setWritable(true, true)
        }

        val factory = SshdSessionFactoryBuilder()
            .setHomeDirectory(contextDir)
            .setSshDirectory(sshDirectory)
            .setDefaultIdentities { listOf(keyFile.toPath()) }
            .setDefaultKnownHostsFiles { listOf(knownHostsFile.toPath()) }
            .setKeyPasswordProvider {
                object : KeyPasswordProvider {
                    override fun getPassphrase(uri: URIish, attempt: Int): CharArray? {
                        return config.passphrase.takeIf { it.isNotEmpty() }?.toCharArray()
                    }

                    override fun setAttempts(maxNumberOfAttempts: Int) = Unit

                    override fun keyLoaded(uri: URIish, attempt: Int, error: Exception?): Boolean {
                        return error == null
                    }
                }
            }
            .build(null)

        SshSessionFactory.setInstance(factory)
        activeSshFactory = factory
        return factory
    }

    @Synchronized
    fun closeActiveSshFactory() {
        activeSshFactory?.close()
        if (SshSessionFactory.getInstance() === activeSshFactory) {
            SshSessionFactory.setInstance(null)
        }
        activeSshFactory = null
        activeKeyFile?.delete()
        activeKeyFile = null
    }

    fun generateKeyPair(passphrase: String = "", email: String = ""): GeneratedSshKey {
        ensureBouncyCastle()
        val keyPair = generateEd25519KeyPair()
        val privateKey = ByteArrayOutputStream().use { output ->
            val encryption = if (passphrase.isBlank()) {
                null
            } else {
                OpenSSHKeyEncryptionContext().apply {
                    setPassword(passphrase)
                    setKdfRounds(16)
                }
            }
            OpenSSHKeyPairResourceWriter.INSTANCE.writePrivateKey(
                keyPair,
                email.ifBlank { "androidgit" },
                encryption,
                output
            )
            output.toString(Charsets.UTF_8.name())
        }
        val publicKey = PublicKeyEntry.toString(keyPair.public) +
            " " + email.ifBlank { "androidgit" }
        return GeneratedSshKey(privateKey, publicKey, "Ed25519")
    }

    private fun generateEd25519KeyPair(): KeyPair {
        return try {
            KeyPairGenerator.getInstance("Ed25519", "BC").generateKeyPair()
        } catch (error: GeneralSecurityException) {
            throw IllegalStateException("Ed25519 is not available on this device", error)
        }
    }

    private fun ensureBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }


    companion object {
        // GitHub's documented host keys. Unknown or changed keys are rejected by JGit.
        private val GITHUB_KNOWN_HOSTS = """
            github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl
            github.com ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBEmKSENjQEezOmxkZMy7opKgwFB9nkt5YRrYMjNuG5N87uRgg6CLrbo5wAdT/y6v0mKV0U2w0WZ2YB/++Tpockg=
            github.com ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQCj7ndNxQowgcQnjshcLrqPEiiphnt+VTTvDP6mHBL9j1aNUkY4Ue1gvwnGLVlOhGeYrnZaMgRK6+PKCUXaDbC7qtbW8gIkhL7aGCsOr/C56SJMy/BCZfxd1nWzAOxSDPgVsmerOBYfNqltV9/hWCqBywINIR+5dIg6JTJ72pcEpEjcYgM4GC7uEnztnZyaVWQ7B381AK4Qdrwt51ZqExKbQpTUNn+EjqoTwvqNj4kqx5QUCI0ThS/YkOxJCXmPUWZbhjpCg56i+2aB6CmK2JGhn57K5mj0MNdBXA4/WnwH6XoPWJzK5Nyu2zB3nAZp+5S5hpQs+p1vN1/wsjk=
        """.trimIndent()
    }
}
