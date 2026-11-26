package com.android.git.data

import java.net.URI

internal enum class CloneAuthChoice { SAVED, NONE, CUSTOM }

internal enum class CloneAuthError {
    INVALID_URL, SSH_REQUIRED, TOKEN_REQUIRED, KEY_REQUIRED, HTTPS_REQUIRED, SAVED_GITHUB_ONLY
}

internal class CloneAuthResult(
    val config: GitAuthConfig? = null,
    val error: CloneAuthError? = null
)

/** Resolve credentials from the actual transport without writing preferences. */
internal object CloneAuthPolicy {
    fun resolve(
        url: String,
        choice: CloneAuthChoice,
        savedHttps: GitAuthConfig,
        savedSsh: GitAuthConfig,
        custom: GitAuthConfig
    ): CloneAuthResult {
        val remote = GitRemoteUrl.parse(url) ?: return CloneAuthResult(error = CloneAuthError.INVALID_URL)
        val ssh = remote.scheme == GitRemoteScheme.SSH
        if (choice == CloneAuthChoice.NONE) {
            return if (ssh) CloneAuthResult(error = CloneAuthError.SSH_REQUIRED)
            else CloneAuthResult(config = GitAuthConfig())
        }
        if (choice == CloneAuthChoice.SAVED && !remote.host.equals("github.com", ignoreCase = true)) {
            return CloneAuthResult(error = CloneAuthError.SAVED_GITHUB_ONLY)
        }
        if (ssh) {
            val config = if (choice == CloneAuthChoice.SAVED) savedSsh else custom
            return if (config.privateKey.isBlank()) CloneAuthResult(error = CloneAuthError.KEY_REQUIRED)
            else CloneAuthResult(config = GitAuthConfig(GitAuthMode.SSH, privateKey = config.privateKey, passphrase = config.passphrase))
        }
        // Never send authentication tokens over HTTP or the anonymous git protocol.
        if (!runCatching { URI(url.trim()).scheme.equals("https", ignoreCase = true) }.getOrDefault(false)) {
            return CloneAuthResult(error = CloneAuthError.HTTPS_REQUIRED)
        }
        val config = if (choice == CloneAuthChoice.SAVED) savedHttps else custom
        return if (config.token.isBlank()) CloneAuthResult(error = CloneAuthError.TOKEN_REQUIRED)
        else CloneAuthResult(config = GitAuthConfig(token = config.token.trim()))
    }
}
