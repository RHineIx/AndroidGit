package com.android.git.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.data.GitManager
import com.android.git.data.PreferencesManager
import com.android.git.ui.components.AuthenticationSettings
import com.android.git.ui.components.GitIcons
import com.android.git.ui.components.rememberAuthenticationForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoSettingsScreen(gitManager: GitManager?, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val authForm = rememberAuthenticationForm(prefs)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.repo_settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(GitIcons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            AuthenticationSettings(
                state = authForm,
                beforeSave = { draft -> gitManager?.configureUser(draft.userName, draft.userEmail) },
                onSaved = onBack
            )
        }
    }
}
