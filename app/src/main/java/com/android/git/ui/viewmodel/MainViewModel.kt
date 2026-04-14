package com.android.git.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.android.git.R
import com.android.git.data.GitHubUpdateManager
import com.android.git.data.GitManager
import com.android.git.data.PreferencesManager
import com.android.git.data.ThemeMode
import com.android.git.model.BranchModel
import com.android.git.model.CommitItem
import com.android.git.model.DashboardState
import com.android.git.model.GitFile
import com.android.git.model.UpdateInfo
import com.android.git.ui.components.SnackbarType
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)

    var gitManager: GitManager? = null
        private set

    var currentRepoFile: File? by mutableStateOf(null)
        private set

    var dashboardState: DashboardState by mutableStateOf(DashboardState.NotInitialized)
        private set

    var branchList: List<BranchModel> by mutableStateOf(emptyList())
        private set

    var changedFiles: List<GitFile> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var isAIGenerating: Boolean by mutableStateOf(false)
        private set

    var statusMessage: String by mutableStateOf("")
        private set

    var statusType: SnackbarType by mutableStateOf(SnackbarType.INFO)
        private set

    var cloneProgress: Float by mutableStateOf(0f)
        private set
    var cloneTaskName: String by mutableStateOf("")
        private set
    var cloneTaskDetails: String by mutableStateOf("")
        private set

    var logList: List<CommitItem> by mutableStateOf(emptyList())
        private set

    var isLogLoading: Boolean by mutableStateOf(false)
        private set

    private var logCurrentOffset = 0
    private var logHasMore = true
    private val LOG_PAGE_SIZE = 50

    var updateInfo: UpdateInfo? by mutableStateOf(null)
        private set

    var showUpdateSheet: Boolean by mutableStateOf(false)
        private set

    var themeMode: ThemeMode by mutableStateOf(prefs.getThemeMode())
        private set

    init {
        savedStateHandle.get<String>("current_repo_path")?.let { path ->
            File(path).takeIf { it.exists() }?.let { openProject(it) }
        }
        checkForUpdates(isManual = false)
    }

    fun getToken(): String = prefs.getToken()
    
    fun saveToken(token: String) = prefs.saveToken(token)
    
    fun clearToken() = prefs.clearToken()
    
    fun getLastValidToken(): String = prefs.getLastValidToken()
    
    fun restoreLastToken(): String = prefs.restoreLastToken()

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.setThemeMode(mode)
    }

    fun verifyGeminiSettings(apiKey: String, modelName: String, prompt: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                if (apiKey.isBlank()) throw Exception("API Key cannot be empty.")
                
                prefs.setGeminiApiKey(apiKey)
                prefs.setGeminiModel(modelName)
                prefs.setGeminiPrompt(prompt)

                val model = GenerativeModel(
                    modelName = modelName, 
                    apiKey = apiKey,
                    generationConfig = generationConfig { temperature = 0.1f }
                )
                
                model.generateContent("Hello, respond with exactly 'OK'")
                showStatus("Gemini Configuration Saved and Verified!", SnackbarType.SUCCESS)
            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("MissingFieldException") == true) {
                    "Verification Failed: Invalid API Key, Unsupported Region, or Model Not Found."
                } else {
                    "Verification Failed: ${e.localizedMessage}"
                }
                showStatus(errorMsg, SnackbarType.ERROR)
            } finally {
                isLoading = false
            }
        }
    }
    
    fun generateAICommitMessage(selectedPaths: Set<String>, onSuccess: (String) -> Unit) {
        if (selectedPaths.isEmpty()) {
            showStatus("Please select files first to generate a commit message.", SnackbarType.WARNING)
            return
        }
        
        val apiKey = prefs.getGeminiApiKey()
        if (apiKey.isEmpty()) {
            showStatus("Gemini API Key is missing. Please set it in General Settings.", SnackbarType.ERROR)
            return
        }

        viewModelScope.launch {
            isAIGenerating = true
            try {
                val diff = gitManager?.getDiff(selectedPaths) ?: ""
                if (diff.isEmpty() || diff.startsWith("Error")) {
                    showStatus("Could not extract diff for AI processing.", SnackbarType.ERROR)
                    isAIGenerating = false
                    return@launch
                }

                val customPrompt = prefs.getGeminiPrompt()
                val basePrompt = if (customPrompt.isNotBlank()) customPrompt else 
                    "You are an expert developer. Generate a concise, standard Conventional Commit message based on the following git diff. Output ONLY the commit message (e.g., feat: ..., fix: ..., chore: ...) without any markdown formatting, explanations, or quotes."
                
                val finalPrompt = "$basePrompt\n\nGit Diff:\n$diff"

                val generativeModel = GenerativeModel(
                    modelName = prefs.getGeminiModel(),
                    apiKey = apiKey,
                    generationConfig = generationConfig {
                        temperature = 0.2f
                    }
                )

                val response = generativeModel.generateContent(finalPrompt)
                val generatedText = response.text?.trim() ?: ""
                val cleanText = generatedText.removePrefix("```").removeSuffix("```").trim()

                onSuccess(cleanText)
                showStatus("Commit message generated successfully!", SnackbarType.SUCCESS)
            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("MissingFieldException") == true) {
                    "AI Error: Invalid Key, Unsupported Region, or Model Not Found."
                } else {
                    "AI Error: ${e.localizedMessage}"
                }
                showStatus(errorMsg, SnackbarType.ERROR)
            } finally {
                isAIGenerating = false
            }
        }
    }

    fun checkForUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            val context = getApplication<Application>()

            if (isManual) {
                showStatus(context.getString(R.string.update_checking), SnackbarType.INFO)
            }

            val packageInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            } catch (e: Exception) { null }

            val currentVersion = packageInfo?.versionName ?: "1.0.0"

            val updateManager = GitHubUpdateManager(
                repoOwner = "RHineIx",
                repoName = "AndroidGit",
                currentVersionName = currentVersion
            )

            val result = updateManager.checkForUpdate()

            if (result != null) {
                updateInfo = result
                showUpdateSheet = true
                if (isManual) clearStatus()
            } else {
                if (isManual) {
                    showStatus(context.getString(R.string.update_latest), SnackbarType.SUCCESS)
                }
            }
        }
    }

    fun dismissUpdateSheet() {
        showUpdateSheet = false
    }

    fun openProject(file: File) {
        if (currentRepoFile?.absolutePath == file.absolutePath && gitManager != null) return
        closeProject()
        currentRepoFile = file
        savedStateHandle["current_repo_path"] = file.absolutePath
        gitManager = GitManager(file)
        prefs.addRecentProject(file.absolutePath)
        loadDashboard()
    }

    fun closeProject() {
        gitManager?.close()
        gitManager = null
        currentRepoFile = null
        savedStateHandle.remove<String>("current_repo_path")
        dashboardState = DashboardState.NotInitialized
        branchList = emptyList()
        changedFiles = emptyList()
        logList = emptyList()
        statusMessage = ""
    }

    fun loadDashboard() {
        val manager = gitManager ?: return
        dashboardState = DashboardState.Loading 
        viewModelScope.launch {
            if (manager.isGitRepo()) {
                manager.configureUser(prefs.getUserName(), prefs.getUserEmail())
                manager.openRepo()
                dashboardState = manager.getDashboardStats()
            } else {
                dashboardState = DashboardState.NotInitialized
            }
        }
    }

    fun initRepo() {
        val manager = gitManager ?: return
        viewModelScope.launch {
            isLoading = true
            showStatus(manager.initRepo(), SnackbarType.SUCCESS)
            loadDashboard()
            isLoading = false
        }
    }

    fun cloneRepository(url: String, folderName: String, token: String, onSuccess: (File) -> Unit) {
        if (isLoading) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            isLoading = true
            cloneProgress = 0f
            cloneTaskName = context.getString(R.string.clone_progress)
            cloneTaskDetails = ""
            
            showStatus(context.getString(R.string.clone_progress), SnackbarType.INFO)
            
            val result = GitManager.cloneRepo(url, Environment.getExternalStorageDirectory(), folderName, token) { task, progress, details ->
                cloneTaskName = task
                cloneProgress = progress
                cloneTaskDetails = details
            }
            
            isLoading = false
            if (result.first != null) {
                showStatus(context.getString(R.string.clone_success), SnackbarType.SUCCESS)
                onSuccess(result.first!!)
            } else {
                showStatus(result.second, SnackbarType.ERROR)
            }
        }
    }

    fun pullChanges() {
        val manager = gitManager ?: return
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (prefs.getToken().isEmpty()) {
                showStatus(context.getString(R.string.error_set_token), SnackbarType.ERROR)
                return@launch
            }
            isLoading = true
            val result = manager.pull(prefs.getToken())
            isLoading = false
            showStatus(result, if (result.contains("Error") || result.contains("Exception")) SnackbarType.ERROR else SnackbarType.SUCCESS)
            loadDashboard()
        }
    }

    fun pushChanges(force: Boolean = false) {
        val manager = gitManager ?: return
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (prefs.getToken().isEmpty()) {
                showStatus(context.getString(R.string.error_set_token), SnackbarType.ERROR)
                return@launch
            }
            isLoading = true
            val result = manager.push(prefs.getToken(), force)
            isLoading = false
            showStatus(result, if (result.contains("Error") || result.contains("Rejected")) SnackbarType.ERROR else SnackbarType.SUCCESS)
            loadDashboard()
        }
    }

    fun loadBranches() {
        val manager = gitManager ?: return
        viewModelScope.launch {
            isLoading = true
            branchList = manager.getRichBranches()
            isLoading = false
        }
    }

    fun checkoutBranch(name: String) {
        val manager = gitManager ?: return
        viewModelScope.launch {
            isLoading = true
            showStatus(manager.checkoutBranch(name), SnackbarType.SUCCESS)
            loadBranches()
            loadDashboard()
            isLoading = false
        }
    }

    fun createBranch(name: String) {
        val manager = gitManager ?: return
        viewModelScope.launch {
            isLoading = true
            val result = manager.createBranch(name)
            manager.checkoutBranch(name)
            showStatus(result, SnackbarType.SUCCESS)
            loadBranches()
            loadDashboard()
            isLoading = false
        }
    }

    fun deleteBranch(name: String) {
        val manager = gitManager ?: return
        viewModelScope.launch {
            isLoading = true
            val result = manager.deleteBranch(name)
            showStatus(result, if(result.contains("Error")) SnackbarType.ERROR else SnackbarType.SUCCESS)
            loadBranches()
            isLoading = false
        }
    }

    fun renameBranch(newName: String) {
        val manager = gitManager ?: return
        viewModelScope.launch {
            isLoading = true
            showStatus(manager.renameBranch(newName), SnackbarType.SUCCESS)
            loadBranches()
            loadDashboard()
            isLoading = false
        }
    }

    fun mergeBranch(name: String) {
        val manager = gitManager ?: return
        viewModelScope.launch {
            isLoading = true
            val result = manager.mergeBranch(name)
            showStatus(result, if(result.contains("failed")) SnackbarType.ERROR else SnackbarType.SUCCESS)
            loadBranches()
            loadDashboard()
            isLoading = false
        }
    }

    fun rebaseBranch(name: String) {
        val manager = gitManager ?: return
        viewModelScope.launch {
            isLoading = true
            val result = manager.rebaseBranch(name)
            showStatus(result, if(result.contains("failed")) SnackbarType.ERROR else SnackbarType.SUCCESS)
            loadBranches()
            loadDashboard()
            isLoading = false
        }
    }

    fun fetchAll() {
        val manager = gitManager ?: return
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (prefs.getToken().isNotEmpty()) {
                isLoading = true
                showStatus(manager.fetchAll(prefs.getToken()), SnackbarType.SUCCESS)
                loadBranches()
                isLoading = false
            } else {
                showStatus(context.getString(R.string.error_set_token), SnackbarType.ERROR)
            }
        }
    }

    fun loadChangedFiles() {
        val manager = gitManager ?: return
        viewModelScope.launch {
            isLoading = true
            changedFiles = manager.getChangedFiles()
            isLoading = false
        }
    }

    fun commitChanges(message: String, isAmend: Boolean, selectedPaths: Set<String>) {
        val manager = gitManager ?: return
        viewModelScope.launch {
            isLoading = true
            manager.addToStage(changedFiles.filter { selectedPaths.contains(it.path) })
            showStatus(manager.commit(message, isAmend), SnackbarType.SUCCESS)
            loadChangedFiles()
            loadDashboard()
            isLoading = false
        }
    }

    fun getLastCommitMessage(onResult: (String) -> Unit) {
        val manager = gitManager ?: return
        viewModelScope.launch { onResult(manager.getLastCommitMessage()) }
    }

    fun loadLogs(reset: Boolean = false) {
        val manager = gitManager ?: return
        if (isLogLoading) return

        if (reset) {
            logCurrentOffset = 0
            logHasMore = true
            logList = emptyList()
        }

        if (!logHasMore) return

        viewModelScope.launch {
            isLogLoading = true
            val newLogs = manager.getCommits(limit = LOG_PAGE_SIZE, offset = logCurrentOffset)
            if (newLogs.size < LOG_PAGE_SIZE) {
                logHasMore = false
            }
            logList = if (reset) newLogs else logList + newLogs
            logCurrentOffset += newLogs.size
            isLogLoading = false
        }
    }

    fun clearStatus() { statusMessage = "" }

    private fun showStatus(message: String, type: SnackbarType) {
        statusMessage = message
        statusType = type
    }

    override fun onCleared() {
        super.onCleared()
        gitManager?.close()
    }
}