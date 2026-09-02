package com.android.git.data

import com.android.git.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

sealed class UpdateResult {
    data class Available(val updateInfo: UpdateInfo) : UpdateResult()
    object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

class GitHubUpdateManager(
    private val repoOwner: String,
    private val repoName: String,
    private val currentVersionName: String
) {

    companion object {
        private const val TIMEOUT = 5000 // 5 seconds
    }

    suspend fun checkForUpdate(): UpdateResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT
            connection.readTimeout = TIMEOUT
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "AndroidGit-App")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val scanner = Scanner(connection.inputStream)
                scanner.useDelimiter("\\A")
                val responseBody = if (scanner.hasNext()) scanner.next() else ""
                scanner.close()

                val info = parseResponse(responseBody)
                if (info != null) {
                    return@withContext UpdateResult.Available(info)
                } else {
                    return@withContext UpdateResult.UpToDate
                }
            } else {
                return@withContext UpdateResult.Error("HTTP Error: $responseCode")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext UpdateResult.Error(e.message ?: "Network connection failed")
        } finally {
            connection?.disconnect()
        }
    }

    @Throws(Exception::class)
    private fun parseResponse(jsonString: String): UpdateInfo? {
        val json = JSONObject(jsonString)

        // Extract Version Tag (e.g., "v4.9.1-stable") -> "4.9.1-stable"
        val tagName = json.optString("tag_name", "").removePrefix("v")

        // Compare Versions
        if (!isNewerVersion(currentVersionName, tagName)) {
            return null // Explicitly means it is up-to-date
        }

        val body = json.optString("body", "New update available.")

        // Extract Download URL for APK
        var downloadUrl = json.optString("html_url")
        val assets = json.optJSONArray("assets")
        if (assets != null && assets.length() > 0) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val browserDownloadUrl = asset.optString("browser_download_url")
                if (browserDownloadUrl.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = browserDownloadUrl
                    break
                }
            }
        }

        return UpdateInfo(
            versionName = tagName,
            versionCode = 0, // GitHub doesn't provide versionCode usually
            releaseNotes = body,
            downloadUrl = downloadUrl,
            isMandatory = false
        )
    }

    /**
     * Compare versions robustly. 
     * Ignores suffixes like "-stable". E.g., "4.9.1" > "4.9.0"
     */
    private fun isNewerVersion(localVersion: String, remoteVersion: String): Boolean {
        // Clean strings: "4.9.1-stable" -> "4.9.1"
        val localClean = localVersion.substringBefore("-")
        val remoteClean = remoteVersion.substringBefore("-")

        val localParts = localClean.split(".").map { it.toIntOrNull() ?: 0 }
        val remoteParts = remoteClean.split(".").map { it.toIntOrNull() ?: 0 }

        val length = maxOf(localParts.size, remoteParts.size)

        for (i in 0 until length) {
            val localPart = localParts.getOrElse(i) { 0 }
            val remotePart = remoteParts.getOrElse(i) { 0 }

            if (remotePart > localPart) return true
            if (remotePart < localPart) return false
        }

        return false
    }
}