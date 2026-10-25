package com.android.git.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.git.R
import com.android.git.data.GitManager
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.components.Button
import com.android.git.ui.components.OplusScaffold
import com.android.git.ui.components.SnackbarType
import com.android.git.utils.isGitFailureMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictResolverScreen(
    gitManager: GitManager,
    filePath: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var fileContent by remember { mutableStateOf("") } // Init empty, load in LaunchedEffect
    var isResolving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var statusType by remember { mutableStateOf(SnackbarType.INFO) }

    fun isFailure(message: String): Boolean = isGitFailureMessage(message)

    LaunchedEffect(filePath) {
        fileContent = gitManager.readFileContent(filePath)
    }

    OplusScaffold(
        title = stringResource(R.string.conflict_resolver_title),
        onBack = onBack,
        subtitle = filePath,
        bottomBar = {
            Column(Modifier
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (isResolving) return@Button
                            isResolving = true
                            scope.launch {
                                val result = gitManager.resolveUsingOurs(filePath)
                                isResolving = false
                                if (isFailure(result)) {
                                    statusMessage = result
                                    statusType = SnackbarType.ERROR
                                } else {
                                    onBack()
                                }
                            }
                        },
                        enabled = !isResolving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(R.string.conflict_btn_ours))
                    }

                    Button(
                        onClick = {
                            if (isResolving) return@Button
                            isResolving = true
                            scope.launch {
                                val result = gitManager.resolveUsingTheirs(filePath)
                                isResolving = false
                                if (isFailure(result)) {
                                    statusMessage = result
                                    statusType = SnackbarType.ERROR
                                } else {
                                    onBack()
                                }
                            }
                        },
                        enabled = !isResolving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.conflict_btn_theirs))
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

            // Info Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    stringResource(R.string.conflict_instruction),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // File Content Preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (fileContent.isEmpty()) {
                    Text(stringResource(R.string.action_loading), fontSize = 12.sp)
                } else {
                    Text(
                        text = fileContent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        if (statusMessage.isNotEmpty()) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp)) {
                AppSnackbar(message = statusMessage, type = statusType) { statusMessage = "" }
            }
        }
    }
}
}
