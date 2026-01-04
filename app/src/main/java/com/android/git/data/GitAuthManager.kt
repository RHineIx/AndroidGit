package com.android.git.data

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

class GitAuthManager(private val contextDir: File) {

    // Simple Helper for HTTPS Token Auth
    fun getCredentialsProvider(token: String): UsernamePasswordCredentialsProvider {
        // Username can be empty for GitHub tokens usually, or use "token"
        return UsernamePasswordCredentialsProvider(token, "")
    }
}