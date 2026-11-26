package com.android.git.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.data.GitManager
import com.android.git.data.PreferencesManager
import com.android.git.ui.components.AuthenticationSettings
import com.android.git.ui.components.OplusScaffold
import com.android.git.ui.components.rememberAuthenticationForm
import com.android.git.utils.isGitFailureMessage

@Composable
fun RepoSettingsScreen(gitManager: GitManager?, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val authForm = rememberAuthenticationForm(prefs)
    OplusScaffold(title = stringResource(R.string.repo_settings_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            AuthenticationSettings(
                state = authForm,
                beforeSave = { draft ->
                    val result = gitManager?.configureUser(draft.userName, draft.userEmail)
                    check(result == null || !isGitFailureMessage(result))
                },
                onSaved = onBack
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
