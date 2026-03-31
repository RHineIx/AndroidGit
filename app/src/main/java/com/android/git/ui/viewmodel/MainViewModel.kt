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
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)

    var gitManager: GitManager? = null
        private set

    // UI State Properties
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

    var statusMessage: String by mutableStateOf("")
        private set

    var statusType: SnackbarType by mutableStateOf(SnackbarType.INFO)
        private set

    // Log & Pagination State
    var logList: List<CommitItem> by mutableStateOf(emptyList())
        private set

    var isLogLoading: Boolean by mutableStateOf(false)
        private set

    private var logCurrentOffset = 0
    private var logHasMore = true
    private val LOG_PAGE_SIZE = 50

    // Update System State
    var updateInfo: UpdateInfo? by mutableStateOf(null)
        private set

    var showUpdateSheet: Boolean by mutableStateOf(false)
        private set

    // Theme Management State
    var themeMode: ThemeMode by mutableStateOf(prefs.getThemeMode())
        private set

    init {
        // Restore session
        savedStateHandle.get<String>("current_repo_path")?.let { path ->
            File(path).takeIf { it.exists() }?.let { openProject(it) }
        }

        // Auto check for updates on startup
        checkForUpdates(isManual = false)
    }

    // --- Theme Management ---

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.setThemeMode(mode)
    }

    // --- Update Logic ---

    fun checkForUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            val context = getApplication<Application>()

            if (isManual) {
                showStatus(context.getString(R.string.update_checking), SnackbarType.INFO)
            }

            // Get current version
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

    // --- Core Git Logic ---

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
            showStatus(context.getString(R.string.clone_progress), SnackbarType.INFO)
            val result = GitManager.cloneRepo(url, Environment.getExternalStorageDirectory(), folderName, token)
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