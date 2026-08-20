package com.android.git.data

import java.net.URI

object GitRemoteUrl {
    fun isValid(raw: String): Boolean = parse(raw) != null

    fun isSsh(raw: String): Boolean {
        val value = raw.trim()
        return value.startsWith("git@") || value.startsWith("ssh://")
    }

    fun toSshUrl(raw: String): String? {
        val value = raw.trim().removeSuffix("/")
        if (value.startsWith("git@")) return value

        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (!uri.host.equals("github.com", ignoreCase = true)) return null
        val path = uri.path?.trim('/')?.takeIf { it.isNotBlank() } ?: return null
        return "git@github.com:$path"
    }

    fun parse(raw: String): ParsedGitUrl? {
        val value = raw.trim().removeSuffix("/")
        if (value.isBlank()) return null

        if (value.startsWith("git@")) {
            val separator = value.indexOf(':')
            if (separator <= 4 || separator == value.lastIndex) return null
            val host = value.substring(4, separator)
            val path = value.substring(separator + 1).trim('/')
            return ParsedGitUrl(GitRemoteScheme.SSH, host, path)
        }

        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val scheme = when (uri.scheme?.lowercase()) {
            "ssh" -> GitRemoteScheme.SSH
            "https", "http", "git" -> GitRemoteScheme.HTTP
            else -> return null
        }
        val host = uri.host?.takeIf { it.isNotBlank() } ?: return null
        val path = uri.path?.trim('/')?.takeIf { it.isNotBlank() } ?: return null
        return ParsedGitUrl(scheme, host, path)
    }
}

enum class GitRemoteScheme {
    SSH,
    HTTP
}

data class ParsedGitUrl(
    val scheme: GitRemoteScheme,
    val host: String,
    val path: String
)
