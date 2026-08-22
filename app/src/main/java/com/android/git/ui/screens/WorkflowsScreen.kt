package com.android.git.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.model.GitHubWorkflowInput
import com.android.git.model.GitHubWorkflowRun
import com.android.git.ui.components.AppSnackbar
import com.android.git.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var showRunDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadWorkflowsForCurrentRepository()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.workflow_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadWorkflowsForCurrentRepository() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (viewModel.isWorkflowsLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    viewModel.githubWorkflowRepository?.let { repository ->
                        Text(
                            text = "${repository.owner}/${repository.repository}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.workflow_current_ref, repository.ref),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (viewModel.workflowErrorMessage.isNotBlank()) {
                        Text(
                            text = viewModel.workflowErrorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                if (viewModel.githubWorkflows.isEmpty() && !viewModel.isWorkflowsLoading) {
                    item {
                        Text(stringResource(R.string.workflow_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                items(viewModel.githubWorkflows, key = { it.id }) { workflow ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(workflow.name, fontWeight = FontWeight.Bold)
                                Text(workflow.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(workflow.state, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Button(
                                onClick = {
                                    viewModel.prepareWorkflowRun(workflow)
                                    showRunDialog = true
                                },
                                enabled = !viewModel.isWorkflowsLoading && workflow.state.equals("active", ignoreCase = true),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(Modifier.padding(horizontal = 2.dp))
                                Text(stringResource(R.string.workflow_configure_run))
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.workflow_runs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                if (viewModel.githubWorkflowRuns.isEmpty()) {
                    item { Text(stringResource(R.string.workflow_no_runs), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(viewModel.githubWorkflowRuns, key = { "run-${it.id}" }) { run ->
                        WorkflowRunCard(run)
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
            AppSnackbar(
                message = viewModel.statusMessage,
                type = viewModel.statusType,
                onDismiss = { viewModel.clearStatus() }
            )
        }
    }

    if (showRunDialog && viewModel.selectedWorkflow != null) {
        WorkflowRunDialog(
            viewModel = viewModel,
            onDismiss = {
                showRunDialog = false
                viewModel.closeWorkflowRunDialog()
            },
            onRun = { ref, inputs ->
                showRunDialog = false
                viewModel.runWorkflow(ref, inputs)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkflowRunDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onRun: (String, Map<String, String>) -> Unit
) {
    val workflow = viewModel.selectedWorkflow ?: return
    val repository = viewModel.githubWorkflowRepository
    var ref by remember(workflow.id, repository?.ref) { mutableStateOf(repository?.ref ?: "main") }
    var inputValues by remember(workflow.id, viewModel.githubWorkflowInputs) {
        mutableStateOf(viewModel.githubWorkflowInputs.associate { it.name to it.defaultValue })
    }
    val context = LocalContext.current
    var validationError by remember(workflow.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workflow_run_dialog_title, workflow.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (viewModel.isWorkflowInputLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).height(20.dp))
                        Text(stringResource(R.string.workflow_loading_inputs))
                    }
                } else {
                    OutlinedTextField(
                        value = ref,
                        onValueChange = { ref = it },
                        label = { Text(stringResource(R.string.workflow_ref)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    viewModel.githubWorkflowInputs.forEach { input ->
                        WorkflowInputField(
                            input = input,
                            value = inputValues[input.name].orEmpty(),
                            onValueChange = { value -> inputValues = inputValues + (input.name to value) }
                        )
                    }
                    if (viewModel.githubWorkflowInputs.isEmpty()) {
                        Text(stringResource(R.string.workflow_no_inputs), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (validationError.isNotBlank()) Text(validationError, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val missing = viewModel.githubWorkflowInputs.firstOrNull { it.required && inputValues[it.name].orEmpty().isBlank() }
                    if (missing != null) {
                        validationError = context.getString(R.string.workflow_required_input, missing.name)
                    } else {
                        onRun(ref, inputValues.filterValues { it.isNotBlank() })
                    }
                },
                enabled = !viewModel.isWorkflowInputLoading && !viewModel.isWorkflowRunning && ref.isNotBlank()
            ) {
                if (viewModel.isWorkflowRunning) CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Text(stringResource(R.string.workflow_execute))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkflowInputField(
    input: GitHubWorkflowInput,
    value: String,
    onValueChange: (String) -> Unit
) {
    val label = if (input.required) "${input.name} *" else input.name
    when {
        input.type.equals("boolean", ignoreCase = true) -> {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, fontWeight = FontWeight.SemiBold)
                    if (input.description.isNotBlank()) Text(input.description, style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = value.equals("true", ignoreCase = true), onCheckedChange = { onValueChange(it.toString()) })
            }
        }
        input.type.equals("choice", ignoreCase = true) && input.options.isNotEmpty() -> {
            var expanded by remember(input.name) { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(label) },
                    supportingText = { if (input.description.isNotBlank()) Text(input.description) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    input.options.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { onValueChange(option); expanded = false })
                    }
                }
            }
        }
        else -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                supportingText = { if (input.description.isNotBlank()) Text(input.description) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = input.type.equals("string", ignoreCase = true)
            )
        }
    }
}

@Composable
private fun WorkflowRunCard(run: GitHubWorkflowRun) {
    val icon = when {
        run.conclusion.equals("success", ignoreCase = true) -> Icons.Default.CheckCircle
        run.conclusion.equals("failure", ignoreCase = true) -> Icons.Default.Error
        else -> Icons.Default.Refresh
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(horizontal = 6.dp))
            Column(Modifier.weight(1f)) {
                Text("${run.name} #${run.runNumber}", fontWeight = FontWeight.SemiBold)
                Text(
                    text = stringResource(R.string.workflow_status_fmt, run.status, run.conclusion.ifBlank { "-" }, run.branch.ifBlank { "-" }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (run.createdAt.isNotBlank()) Text(run.createdAt, style = MaterialTheme.typography.labelSmall)
            }
        }
        HorizontalDivider()
    }
}
