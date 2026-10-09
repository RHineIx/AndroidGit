package com.android.git.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.R
import com.android.git.data.GitManager
import com.android.git.data.GitTemplates
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.SnackbarType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgnoreEditorScreen(
    gitManager: GitManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var statusType by remember { mutableStateOf(SnackbarType.INFO) }
    var showTemplateMenu by remember { mutableStateOf(false) }

    LaunchedEffect(gitManager) {
        try {
            content = gitManager.readGitIgnore()
        } catch (e: Exception) {
            statusMessage = e.message ?: "Unable to read .gitignore"
            statusType = SnackbarType.ERROR
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ignore_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showTemplateMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = stringResource(R.string.ignore_add_template))
                        }
                        
                        DropdownMenu(
                            expanded = showTemplateMenu,
                            onDismissRequest = { showTemplateMenu = false }
                        ) {
                            GitTemplates.templates.forEach { (name, templateContent) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        val prefix = if (content.isNotEmpty() && !content.endsWith("\n")) "\n\n" else ""
                                        content += "$prefix$templateContent\n"
                                        showTemplateMenu = false
                                        // "Added X template" logic handled manually as string resource format
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            if (isSaving) return@IconButton
                            scope.launch {
                                isSaving = true
                                val result = gitManager.saveGitIgnore(content)
                                statusMessage = result
                                statusType = if (result.trim().lowercase().startsWith("error")) SnackbarType.ERROR else SnackbarType.SUCCESS
                                isSaving = false
                            }
                        },
                        enabled = !isLoading && !isSaving
                    ) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.action_save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (statusMessage.isNotEmpty()) {
                AppSnackbar(
                    message = statusMessage,
                    type = statusType,
                    onDismiss = { statusMessage = "" }
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
        }
    }
}