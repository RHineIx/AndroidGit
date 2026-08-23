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
import com.android.git.data.GitAuthConfig
import com.android.git.data.GitAuthMode
import com.android.git.data.GitHubUpdateManager
import com.android.git.data.GitHubWorkflowApiManager
import com.android.git.data.GitHubWorkflowException
import com.android.git.data.GitRemoteUrl
import com.android.git.data.GitManager
import com.android.git.data.PreferencesManager
import com.android.git.data.ThemeMode
import com.android.git.model.BranchModel
import com.android.git.model.CommitItem
import com.android.git.model.DashboardState
import com.android.git.model.GitFile
import com.android.git.model.GitHubRepositoryRef
import com.android.git.model.GitHubWorkflow
import com.android.git.model.GitHubWorkflowInput
import com.android.git.model.GitHubWorkflowRun
import com.android.git.model.UpdateInfo
import com.android.git.ui.components.SnackbarType
import com.android.git.utils.isGitFailureMessage
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

    var githubWorkflows: List<GitHubWorkflow> by mutableStateOf(emptyList())
        private set

    var githubWorkflowRuns: List<GitHubWorkflowRun> by mutableStateOf(emptyList())
        private set

    var githubWorkflowInputs: List<GitHubWorkflowInput> by mutableStateOf(emptyList())
        private set

    var githubWorkflowRepository: GitHubRepositoryRef? by mutableStateOf(null)
        private set

    var selectedWorkflow: GitHubWorkflow? by mutableStateOf(null)
        private set

    var isWorkflowsLoading: Boolean by mutableStateOf(false)
        private set

    var isWorkflowInputLoading: Boolean by mutableStateOf(false)
        private set

    var isWorkflowRunning: Boolean by mutableStateOf(false)
        private set

    var workflowErrorMessage: String by mutableStateOf("")
        private set

    var logList: List<CommitItem> by mutableStateOf(emptyList())
        private set

    var isLogLoading: Boolean by mutableStateOf(false)
        private set

    var logErrorMessage: String by mutableStateOf("")
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

    fun getAuthConfig(): GitAuthConfig {
        return when (prefs.getAuthMode()) {
            GitAuthMode.SSH -> GitAuthConfig(
                mode = GitAuthMode.SSH,
                privateKey = prefs.getSshPrivateKey(),
                passphrase = prefs.getSshPassphrase()
            )
            GitAuthMode.HTTPS -> GitAuthConfig(
                mode = GitAuthMode.HTTPS,
                token = prefs.getToken()
            )
        }
    }

    fun saveAuthMode(mode: GitAuthMode) = prefs.setAuthMode(mode)

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
                } else "Verification Failed: ${e.localizedMessage}"
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
                    return@launch
                }
                val customPrompt = prefs.getGeminiPrompt()
                val basePrompt = if (customPrompt.isNotBlank()) customPrompt else
                    "You are an expert developer. Generate a Conventional Commit message based on the following git diff.\n" +
                        "Format requirements:\n" +
                        "1. A concise subject line (e.g., feat: ..., fix: ...).\n" +
                        "2. A blank line.\n" +
                        "3. A concise bulleted list summarizing ALL notable changes.\n" +
                        "Keep the bullet points strictly short and to the point.\n" +
                        "Output ONLY the commit message without any markdown formatting like ``` ."
                val generativeModel = GenerativeModel(
                    modelName = prefs.getGeminiModel(),
                    apiKey = apiKey,
                    generationConfig = generationConfig {
                        temperature = 0.3f
                        maxOutputTokens = 2048
                    }
                )
                val response = generativeModel.generateContent("$basePrompt\n\nGit Diff:\n$diff")
                val cleanText = (response.text?.trim() ?: "").removePrefix("```").removeSuffix("```").trim()
                onSuccess(cleanText)
                showStatus("Commit message generated successfully!", SnackbarType.SUCCESS)
            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("MissingFieldException") == true) {
                    "AI Error: Invalid Key, Unsupported Region, or Model Not Found."
                } else "AI Error: ${e.localizedMessage}"
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
        gitManager = GitManager(file, getApplication<Application>().filesDir)
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
        logErrorMessage = ""
        statusMessage = ""
    }

    fun loadDashboard() {
        val manager = gitManager ?: return
        dashboardState = DashboardState.Loading 
        viewModelScope.launch {
            try {
                if (manager.isGitRepo()) {
                    manager.configureUser(prefs.getUserName(), prefs.getUserEmail())
                    manager.openRepo()
                    dashboardState = manager.getDashboardStats()
                } else {
                    dashboardState = DashboardState.NotInitialized
                }
            } catch (e: Exception) {
                dashboardState = DashboardState.Error(e.message ?: "Unable to load repository")
            }
        }
    }

    fun initRepo() {
        val manager = gitManager ?: return
        viewModelScope.launch {
            isLoading = true
            val result = manager.initRepo()
            showStatus(result, resultSnackbarType(result))
            loadDashboard()
            isLoading = false
        }
    }

    fun cloneRepository(url: String, folderName: String, auth: GitAuthConfig, onSuccess: (File) -> Unit) {
        if (isLoading) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            isLoading = true
            cloneProgress = 0f
            cloneTaskName = context.getString(R.string.clone_progress)
            cloneTaskDetails = ""
            
            showStatus(context.getString(R.string.clone_progress), SnackbarType.INFO)
            
            val result = GitManager.cloneRepo(
                url = url,
                parentDir = Environment.getExternalStorageDirectory(),
                folderName = folderName,
                auth = auth,
                secureStorageDir = context.filesDir
            ) { task, progress, details ->
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
            val auth = getAuthConfig()
            if (auth.mode == GitAuthMode.HTTPS && auth.token.isEmpty()) {
                showStatus(context.getString(R.string.error_set_token), SnackbarType.ERROR)
                return@launch
            }
            if (auth.mode == GitAuthMode.SSH && auth.privateKey.isEmpty()) {
                showStatus(context.getString(R.string.error_set_ssh_key), SnackbarType.ERROR)
                return@launch
            }
            isLoading = true
            try {
                val result = manager.pull(auth)
                showStatus(result, resultSnackbarType(result))
                refreshBranchData(manager)
            } catch (e: Exception) {
                showStatus(errorMessage(e, "Pull failed"), SnackbarType.ERROR)
            } finally {
                isLoading = false
            }
        }
    }

    fun pushChanges(force: Boolean = false) {
        val manager = gitManager ?: return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val auth = getAuthConfig()
            if (auth.mode == GitAuthMode.HTTPS && auth.token.isEmpty()) {
                showStatus(context.getString(R.string.error_set_token), SnackbarType.ERROR)
                return@launch
            }
            if (auth.mode == GitAuthMode.SSH && auth.privateKey.isEmpty()) {
                showStatus(context.getString(R.string.error_set_ssh_key), SnackbarType.ERROR)
                return@launch
            }
            isLoading = true
            val result = manager.push(auth, force)
            isLoading = false
            showStatus(result, resultSnackbarType(result))
            loadDashboard()
        }
    }

    fun loadBranches() {
        val manager = gitManager ?: return
        if (isLoading) return
        viewModelScope.launch {
            isLoading = true
            try {
                branchList = manager.getRichBranches()
            } catch (e: Exception) {
                branchList = emptyList()
                showStatus(errorMessage(e, "Unable to load branches"), SnackbarType.ERROR)
            } finally {
                isLoading = false
            }
        }
    }

    fun checkoutBranch(name: String) = runBranchOperation { manager ->
        manager.checkoutBranch(name)
    }

    fun createBranch(name: String) = runBranchOperation { manager ->
        val created = manager.createBranch(name)
        if (isFailure(created)) {
            created
        } else {
            val checkout = manager.checkoutBranch(name)
            if (isFailure(checkout)) {
                "Error: $created, but checkout failed: $checkout"
            } else {
                "Created and switched to ${name.trim()}"
            }
        }
    }

    fun deleteBranch(name: String) = runBranchOperation { manager ->
        manager.deleteBranch(name)
    }

    fun forceDeleteBranch(name: String) = runBranchOperation { manager ->
        manager.forceDeleteBranch(name)
    }

    fun renameBranch(newName: String) = runBranchOperation { manager ->
        manager.renameBranch(newName)
    }

    fun mergeBranch(name: String) = runBranchOperation { manager ->
        manager.mergeBranch(name)
    }

    fun rebaseBranch(name: String) = runBranchOperation { manager ->
        manager.rebaseBranch(name)
    }

    private fun runBranchOperation(operation: suspend (GitManager) -> String) {
        val manager = gitManager ?: return
        if (isLoading) return
        viewModelScope.launch {
            isLoading = true
            try {
                val result = operation(manager)
                showStatus(result, resultSnackbarType(result))
                // Keep the list and dashboard in sync before controls become active again.
                branchList = manager.getRichBranches()
                if (manager.isGitRepo()) dashboardState = manager.getDashboardStats()
            } catch (e: Exception) {
                showStatus(errorMessage(e, "Branch operation failed"), SnackbarType.ERROR)
                runCatching { branchList = manager.getRichBranches() }
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun refreshBranchData(manager: GitManager) {
        branchList = manager.getRichBranches()
        if (manager.isGitRepo()) {
            dashboardState = manager.getDashboardStats()
        }
    }

    private fun errorMessage(error: Throwable, fallback: String): String {
        val message = generateSequence(error) { it.cause }
            .mapNotNull { it.message?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .joinToString(" -> ")
        return "Error: ${if (message.isBlank()) fallback else message}"
    }

    fun fetchAll() {
        val manager = gitManager ?: return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val auth = getAuthConfig()
            val configured = when (auth.mode) {
                GitAuthMode.HTTPS -> auth.token.isNotEmpty()
                GitAuthMode.SSH -> auth.privateKey.isNotEmpty()
            }
            if (configured) {
                isLoading = true
                try {
                    val result = manager.fetchAll(auth)
                    showStatus(result, resultSnackbarType(result))
                    // Do not call loadBranches() here: it intentionally ignores
                    // requests while isLoading is true. Refresh synchronously so
                    // pruned refs disappear from Branch Manager immediately.
                    refreshBranchData(manager)
                } catch (e: Exception) {
                    showStatus(errorMessage(e, "Fetch failed"), SnackbarType.ERROR)
                } finally {
                    isLoading = false
                }
            } else {
                showStatus(
                    if (auth.mode == GitAuthMode.SSH) context.getString(R.string.error_set_ssh_key)
                    else context.getString(R.string.error_set_token),
                    SnackbarType.ERROR
                )
            }
        }
    }

    fun loadChangedFiles() {
        val manager = gitManager ?: return
        viewModelScope.launch {
            isLoading = true
            try {
                changedFiles = manager.getChangedFiles()
            } catch (e: Exception) {
                changedFiles = emptyList()
                showStatus(e.message ?: "Unable to load changes", SnackbarType.ERROR)
            } finally {
                isLoading = false
            }
        }
    }

    fun commitChanges(
        message: String,
        isAmend: Boolean,
        selectedPaths: Set<String>,
        onSuccess: () -> Unit = {}
    ) {
        val manager = gitManager ?: return
        viewModelScope.launch {
            isLoading = true
            val stageResult = manager.addToStage(changedFiles.filter { selectedPaths.contains(it.path) })
            if (isFailure(stageResult)) {
                showStatus(stageResult, SnackbarType.ERROR)
                isLoading = false
                return@launch
            }
            val result = manager.commit(message, isAmend)
            showStatus(result, resultSnackbarType(result))
            if (!isFailure(result)) onSuccess()
            changedFiles = manager.getChangedFiles()
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
            logErrorMessage = ""
        }

        if (!logHasMore) return

        viewModelScope.launch {
            isLogLoading = true
            try {
                val newLogs = manager.getCommits(limit = LOG_PAGE_SIZE, offset = logCurrentOffset)
                if (newLogs.size < LOG_PAGE_SIZE) {
                    logHasMore = false
                }
                logList = if (reset) newLogs else logList + newLogs
                logCurrentOffset += newLogs.size
                logErrorMessage = ""
            } catch (e: Exception) {
                logErrorMessage = e.message ?: "Unable to load commit history"
            } finally {
                isLogLoading = false
            }
        }
    }

    fun clearStatus() { statusMessage = "" }

    fun loadWorkflowsForCurrentRepository() {
        val manager = gitManager ?: return
        val token = prefs.getToken()
        if (token.isBlank()) {
            workflowErrorMessage = getApplication<Application>().getString(R.string.workflow_no_token)
            githubWorkflows = emptyList()
            githubWorkflowRuns = emptyList()
            return
        }
        viewModelScope.launch {
            isWorkflowsLoading = true
            workflowErrorMessage = ""
            try {
                val remote = manager.getRemoteUrl()
                val parsed = GitRemoteUrl.parse(remote)
                val path = parsed?.path?.trim('/')?.removeSuffix(".git")?.split('/') ?: emptyList()
                if (parsed == null || !parsed.host.equals("github.com", ignoreCase = true) || path.size < 2) {
                    throw GitHubWorkflowException(getApplication<Application>().getString(R.string.workflow_github_remote_required))
                }
                val ref = manager.getCurrentBranch().ifBlank { "main" }
                val repository = GitHubRepositoryRef(path[0], path[1], ref)
                val api = GitHubWorkflowApiManager(token)
                githubWorkflowRepository = repository
                githubWorkflows = api.listWorkflows(repository)
                githubWorkflowRuns = api.recentRuns(repository).take(5)
            } catch (error: Exception) {
                workflowErrorMessage = error.message ?: getApplication<Application>().getString(R.string.workflow_load_failed)
                githubWorkflows = emptyList()
                githubWorkflowRuns = emptyList()
            } finally {
                isWorkflowsLoading = false
            }
        }
    }

    fun prepareWorkflowRun(workflow: GitHubWorkflow) {
        val repository = githubWorkflowRepository ?: return
        val token = prefs.getToken()
        if (token.isBlank()) {
            showStatus(getApplication<Application>().getString(R.string.workflow_no_token), SnackbarType.ERROR)
            return
        }
        selectedWorkflow = workflow
        githubWorkflowInputs = emptyList()
        isWorkflowInputLoading = true
        viewModelScope.launch {
            try {
                githubWorkflowInputs = GitHubWorkflowApiManager(token).readWorkflowInputs(repository, workflow)
            } catch (error: Exception) {
                workflowErrorMessage = error.message ?: getApplication<Application>().getString(R.string.workflow_inputs_load_failed)
            } finally {
                isWorkflowInputLoading = false
            }
        }
    }

    fun closeWorkflowRunDialog() {
        selectedWorkflow = null
        githubWorkflowInputs = emptyList()
        isWorkflowInputLoading = false
    }

    fun runWorkflow(ref: String, inputs: Map<String, String>) {
        val workflow = selectedWorkflow ?: return
        val repository = githubWorkflowRepository ?: return
        val token = prefs.getToken()
        if (token.isBlank()) {
            showStatus(getApplication<Application>().getString(R.string.workflow_no_token), SnackbarType.ERROR)
            return
        }
        viewModelScope.launch {
            isWorkflowRunning = true
            try {
                GitHubWorkflowApiManager(token).dispatchWorkflow(repository, workflow, ref, inputs)
                closeWorkflowRunDialog()
                showStatus(getApplication<Application>().getString(R.string.workflow_started), SnackbarType.SUCCESS)
                githubWorkflowRuns = GitHubWorkflowApiManager(token).recentRuns(repository).take(5)
            } catch (error: Exception) {
                showStatus(error.message ?: getApplication<Application>().getString(R.string.workflow_run_failed), SnackbarType.ERROR)
            } finally {
                isWorkflowRunning = false
            }
        }
    }

    private fun isFailure(message: String): Boolean = isGitFailureMessage(message)

    private fun resultSnackbarType(message: String): SnackbarType =
        if (isFailure(message)) SnackbarType.ERROR else SnackbarType.SUCCESS

    private fun showStatus(message: String, type: SnackbarType) {
        statusMessage = message
        statusType = type
    }

    override fun onCleared() {
        super.onCleared()
        gitManager?.close()
    }
}