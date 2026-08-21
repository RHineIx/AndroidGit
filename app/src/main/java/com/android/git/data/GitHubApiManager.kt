package com.android.git.data

import com.android.git.model.GitHubAccount
import com.android.git.model.GitHubAccountSnapshot
import com.android.git.model.GitHubRepository
import com.android.git.model.GitHubWorkflow
import com.android.git.model.GitHubWorkflowRun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class GitHubApiException(
    val statusCode: Int,
    message: String
) : IOException(message)

class GitHubApiManager {
    companion object {
        private const val API_ROOT = "https://api.github.com"
        private const val API_VERSION = "2026-03-10"
        private const val TIMEOUT_MS = 15_000
        private const val MAX_REPOSITORY_PAGES = 20
    }

    suspend fun getAccountSnapshot(token: String): GitHubAccountSnapshot = withContext(Dispatchers.IO) {
        requireToken(token)
        val accountJson = requestJson("/user", token)
        val repositories = buildList {
            for (page in 1..MAX_REPOSITORY_PAGES) {
                val pageItems = requestJsonArray("/user/repos?per_page=100&page=$page&sort=updated", token)
                    .toRepositories()
                addAll(pageItems)
                if (pageItems.size < 100) break
            }
        }
        GitHubAccountSnapshot(accountJson.toAccount(), repositories)
    }

    suspend fun listWorkflows(owner: String, repository: String, token: String): List<GitHubWorkflow> =
        withContext(Dispatchers.IO) {
            requireToken(token)
            requestJson("/repos/${owner.pathSegment()}/${repository.pathSegment()}/actions/workflows?per_page=100", token)
                .optJSONArray("workflows")
                .toWorkflows()
        }

    suspend fun listWorkflowRuns(
        owner: String,
        repository: String,
        token: String,
        workflowId: Long? = null
    ): List<GitHubWorkflowRun> = withContext(Dispatchers.IO) {
        requireToken(token)
        val suffix = workflowId?.let { "/workflows/$it" } ?: ""
        requestJson("/repos/${owner.pathSegment()}/${repository.pathSegment()}/actions${suffix}/runs?per_page=30", token)
            .optJSONArray("workflow_runs")
            .toWorkflowRuns()
    }

    suspend fun dispatchWorkflow(
        owner: String,
        repository: String,
        workflowId: Long,
        ref: String,
        token: String,
        inputs: Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        requireToken(token)
        val body = JSONObject()
            .put("ref", ref)
            .put("inputs", JSONObject(inputs))
        request(
            path = "/repos/${owner.pathSegment()}/${repository.pathSegment()}/actions/workflows/$workflowId/dispatches",
            method = "POST",
            token = token,
            body = body.toString()
        )
    }

    private fun requestJson(path: String, token: String): JSONObject =
        JSONObject(request(path, "GET", token))

    private fun requestJsonArray(path: String, token: String): JSONArray =
        JSONArray(request(path, "GET", token))

    private fun request(path: String, method: String, token: String, body: String? = null): String {
        val connection = (URL(API_ROOT + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doInput = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", API_VERSION)
            setRequestProperty("User-Agent", "AndroidGit-App")
            setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }

        return try {
            if (body != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw GitHubApiException(status, errorMessage(status, response))
            }
            response
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(status: Int, response: String): String {
        val apiMessage = runCatching { JSONObject(response).optString("message") }.getOrNull()
        return when (status) {
            HttpURLConnection.HTTP_UNAUTHORIZED -> "GitHub token is invalid or expired."
            HttpURLConnection.HTTP_FORBIDDEN -> "GitHub denied this request. Check token permissions or rate limits."
            HttpURLConnection.HTTP_NOT_FOUND -> "GitHub resource was not found or is not accessible by this token."
            else -> apiMessage?.takeIf { it.isNotBlank() } ?: "GitHub request failed (HTTP $status)."
        }
    }

    private fun requireToken(token: String) {
        require(token.isNotBlank()) { "A GitHub token is required." }
    }
}

private fun JSONObject.toAccount() = GitHubAccount(
    login = optString("login"),
    name = optString("name").ifBlank { optString("login") },
    avatarUrl = optString("avatar_url"),
    htmlUrl = optString("html_url"),
    bio = optString("bio"),
    publicRepos = optInt("public_repos"),
    privateRepos = optInt("total_private_repos"),
    followers = optInt("followers"),
    following = optInt("following")
)

private fun JSONArray?.toRepositories(): List<GitHubRepository> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val json = optJSONObject(index) ?: continue
            val owner = json.optJSONObject("owner")
            add(
                GitHubRepository(
                    id = json.optLong("id"),
                    name = json.optString("name"),
                    fullName = json.optString("full_name"),
                    ownerLogin = owner?.optString("login").orEmpty(),
                    description = json.optString("description"),
                    isPrivate = json.optBoolean("private"),
                    htmlUrl = json.optString("html_url"),
                    cloneUrl = json.optString("clone_url"),
                    sshUrl = json.optString("ssh_url"),
                    defaultBranch = json.optString("default_branch").ifBlank { "main" },
                    language = json.optString("language"),
                    stars = json.optInt("stargazers_count"),
                    forks = json.optInt("forks_count"),
                    updatedAt = json.optString("updated_at")
                )
            )
        }
    }
}

private fun JSONArray?.toWorkflows(): List<GitHubWorkflow> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val json = optJSONObject(index) ?: continue
            add(
                GitHubWorkflow(
                    id = json.optLong("id"),
                    name = json.optString("name"),
                    path = json.optString("path"),
                    state = json.optString("state"),
                    htmlUrl = json.optString("html_url")
                )
            )
        }
    }
}

private fun JSONArray?.toWorkflowRuns(): List<GitHubWorkflowRun> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val json = optJSONObject(index) ?: continue
            add(
                GitHubWorkflowRun(
                    id = json.optLong("id"),
                    name = json.optString("name"),
                    runNumber = json.optInt("run_number"),
                    status = json.optString("status"),
                    conclusion = json.optString("conclusion"),
                    branch = json.optString("head_branch"),
                    event = json.optString("event"),
                    htmlUrl = json.optString("html_url"),
                    createdAt = json.optString("created_at")
                )
            )
        }
    }
}

private fun String.pathSegment(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name())
