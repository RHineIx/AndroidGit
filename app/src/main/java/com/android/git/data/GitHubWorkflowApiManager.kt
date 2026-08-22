package com.android.git.data

import android.util.Base64
import com.android.git.model.GitHubRepositoryRef
import com.android.git.model.GitHubWorkflow
import com.android.git.model.GitHubWorkflowInput
import com.android.git.model.GitHubWorkflowRun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GitHubWorkflowApiManager(private val token: String) {
    companion object {
        private const val API_ROOT = "https://api.github.com"
        private const val TIMEOUT_MS = 15_000
    }

    suspend fun listWorkflows(repository: GitHubRepositoryRef): List<GitHubWorkflow> = withContext(Dispatchers.IO) {
        val json = requestJson("/repos/${repository.owner}/${repository.repository}/actions/workflows?per_page=100")
        val workflows = json.optJSONArray("workflows") ?: JSONArray()
        buildList {
            for (index in 0 until workflows.length()) {
                val item = workflows.optJSONObject(index) ?: continue
                add(
                    GitHubWorkflow(
                        id = item.optLong("id"),
                        name = item.optString("name", item.optString("path", "Workflow")),
                        path = item.optString("path"),
                        state = item.optString("state", "unknown"),
                        htmlUrl = item.optString("html_url")
                    )
                )
            }
        }
    }

    suspend fun recentRuns(repository: GitHubRepositoryRef): List<GitHubWorkflowRun> = withContext(Dispatchers.IO) {
        val json = requestJson("/repos/${repository.owner}/${repository.repository}/actions/runs?per_page=5")
        val runs = json.optJSONArray("workflow_runs") ?: JSONArray()
        buildList {
            for (index in 0 until runs.length()) {
                val item = runs.optJSONObject(index) ?: continue
                add(item.toWorkflowRun())
            }
        }
    }

    suspend fun readWorkflowInputs(repository: GitHubRepositoryRef, workflow: GitHubWorkflow): List<GitHubWorkflowInput> = withContext(Dispatchers.IO) {
        val contentJson = requestJson(
            "/repos/${repository.owner}/${repository.repository}/contents/${encodePath(workflow.path)}?ref=${encode(repository.ref)}"
        )
        val encoded = contentJson.optString("content")
        if (encoded.isBlank()) return@withContext emptyList()
        val yaml = String(Base64.decode(encoded.replace("\\s".toRegex(), ""), Base64.DEFAULT), StandardCharsets.UTF_8)
        parseWorkflowDispatchInputs(yaml)
    }

    suspend fun dispatchWorkflow(
        repository: GitHubRepositoryRef,
        workflow: GitHubWorkflow,
        ref: String,
        inputs: Map<String, String>
    ) = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("ref", ref.ifBlank { repository.ref.ifBlank { "main" } })
            put("inputs", JSONObject(inputs))
        }
        request(
            method = "POST",
            path = "/repos/${repository.owner}/${repository.repository}/actions/workflows/${workflow.id}/dispatches",
            body = payload.toString()
        )
    }

    private fun requestJson(path: String): JSONObject {
        return JSONObject(request("GET", path, null).body)
    }

    private fun request(method: String, path: String, body: String?): HttpResponse {
        val connection = (URL(API_ROOT + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("User-Agent", "AndroidGit-App")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(responseBody).optString("message") }.getOrNull().orEmpty()
                throw GitHubWorkflowException("GitHub API $status${if (message.isNotBlank()) ": $message" else ""}")
            }
            HttpResponse(status, responseBody)
        } catch (error: GitHubWorkflowException) {
            throw error
        } catch (error: IOException) {
            throw GitHubWorkflowException(error.message ?: "Network request failed", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseWorkflowDispatchInputs(yaml: String): List<GitHubWorkflowInput> {
        val lines = yaml.lines()
        var dispatchIndent = -1
        var inputsIndent = -1
        var inDispatch = false
        var inInputs = false
        var currentName: String? = null
        var currentIndent = -1
        var description = ""
        var type = "string"
        var required = false
        var defaultValue = ""
        var options = mutableListOf<String>()
        var readingOptionsList = false
        val result = mutableListOf<GitHubWorkflowInput>()

        fun flush() {
            val name = currentName ?: return
            result += GitHubWorkflowInput(name, description, type, required, defaultValue, options.toList())
            currentName = null
            description = ""
            type = "string"
            required = false
            defaultValue = ""
            options = mutableListOf()
            readingOptionsList = false
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#")) continue
            val indent = line.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)

            if (!inDispatch && trimmed == "workflow_dispatch:") {
                inDispatch = true
                dispatchIndent = indent
                continue
            }
            if (inDispatch && indent <= dispatchIndent && trimmed != "workflow_dispatch:") break
            if (inDispatch && !inInputs && trimmed == "inputs:") {
                inInputs = true
                inputsIndent = indent
                continue
            }
            if (!inInputs) continue
            if (indent <= inputsIndent && trimmed != "inputs:") break

            if (indent == inputsIndent + 2 && trimmed.endsWith(":")) {
                flush()
                currentName = trimmed.removeSuffix(":").trim()
                currentIndent = indent
                continue
            }
            if (currentName == null || indent <= currentIndent) continue

            if (trimmed.startsWith("- ") && readingOptionsList) {
                options += cleanScalar(trimmed.removePrefix("- ").trim())
                continue
            }

            val separator = trimmed.indexOf(':')
            if (separator <= 0) continue
            val key = trimmed.substring(0, separator).trim()
            val value = trimmed.substring(separator + 1).trim()
            when (key) {
                "description" -> description = cleanScalar(value)
                "type" -> type = cleanScalar(value).ifBlank { "string" }
                "required" -> required = cleanScalar(value).equals("true", ignoreCase = true)
                "default" -> defaultValue = cleanScalar(value)
                "options" -> {
                    options = parseInlineOptions(value).toMutableList()
                    readingOptionsList = value.isBlank()
                }
                else -> readingOptionsList = false
            }
        }
        flush()
        return result
    }

    private fun parseInlineOptions(value: String): List<String> {
        if (!value.startsWith("[") || !value.endsWith("]")) return emptyList()
        return value.removePrefix("[").removeSuffix("]").split(',').map { cleanScalar(it.trim()) }.filter { it.isNotBlank() }
    }

    private fun cleanScalar(value: String): String {
        return value.substringBefore(" #").trim().removeSurrounding("\"").removeSurrounding("'")
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun encodePath(value: String): String = value.split('/').joinToString("/") { encode(it) }

    private fun JSONObject.toWorkflowRun(): GitHubWorkflowRun = GitHubWorkflowRun(
        id = optLong("id"),
        name = optString("name", "Workflow run"),
        status = optString("status", "unknown"),
        conclusion = optString("conclusion", ""),
        branch = optString("head_branch", ""),
        runNumber = optInt("run_number"),
        createdAt = optString("created_at", ""),
        htmlUrl = optString("html_url", "")
    )
}

data class HttpResponse(val status: Int, val body: String)

class GitHubWorkflowException(message: String, cause: Throwable? = null) : Exception(message, cause)
