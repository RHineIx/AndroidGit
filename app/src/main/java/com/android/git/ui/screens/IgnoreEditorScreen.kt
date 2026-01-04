package com.android.git.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.data.GitManager
import com.android.git.data.GitTemplates
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgnoreEditorScreen(
    repoFile: File,
    onBack: () -> Unit
) {
    val gitManager = remember { GitManager(repoFile) }
    val scope = rememberCoroutineScope()
    
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    
    // Template Menu State
    var showTemplateMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        onBack()
    }

    LaunchedEffect(repoFile) {
        content = gitManager.readGitIgnore()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(".gitignore") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 1. Templates Button
                    Box {
                        IconButton(onClick = { showTemplateMenu = true }) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = "Add Template")
                        }
                        
                        DropdownMenu(
                            expanded = showTemplateMenu,
                            onDismissRequest = { showTemplateMenu = false }
                        ) {
                            // Generate menu items dynamically from GitTemplates
                            GitTemplates.templates.forEach { (name, templateContent) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        // Append template to existing content
                                        val prefix = if (content.isNotEmpty() && !content.endsWith("\n")) "\n\n" else ""
                                        content += "$prefix$templateContent\n"
                                        showTemplateMenu = false
                                        statusMessage = "Added $name template"
                                    }
                                )
                            }
                        }
                    }

                    // 2. Save Button
                    IconButton(onClick = {
                        scope.launch {
                            statusMessage = "Saving..."
                            statusMessage = gitManager.saveGitIgnore(content)
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (statusMessage.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = statusMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                LaunchedEffect(statusMessage) {
                    // Auto-hide message after delay unless it's "Saving..."
                    if (statusMessage != "Saving...") {
                        kotlinx.coroutines.delay(2000)
                        statusMessage = ""
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator()
            }
        } else {
            // Text Editor Area
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
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