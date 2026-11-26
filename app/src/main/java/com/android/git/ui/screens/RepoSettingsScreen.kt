package com.android.git.ui.screens

import com.android.git.ui.components.OplusScaffold

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.data.GitManager
import com.android.git.data.PreferencesManager
import com.android.git.ui.components.AuthenticationSettings
import com.android.git.ui.components.rememberAuthenticationForm
import com.android.git.utils.isGitFailureMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoSettingsScreen(
    gitManager: GitManager?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val authForm = rememberAuthenticationForm(prefs)

    OplusScaffold(
        title = stringResource(R.string.repo_settings_title),
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            AuthenticationSettings(
                state = authForm,
                beforeSave = { draft ->
                    val result = gitManager?.configureUser(draft.userName, draft.userEmail)
                    check(result == null || !isGitFailureMessage(result))
                },
                onSaved = onBack
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}