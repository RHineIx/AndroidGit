package com.android.git.model

data class GitHubWorkflow(
    val id: Long,
    val name: String,
    val path: String,
    val state: String,
    val htmlUrl: String
)

data class GitHubWorkflowRun(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String,
    val branch: String,
    val runNumber: Int,
    val createdAt: String,
    val htmlUrl: String
)

data class GitHubWorkflowInput(
    val name: String,
    val description: String,
    val type: String,
    val required: Boolean,
    val defaultValue: String,
    val options: List<String>
)

data class GitHubRepositoryRef(
    val owner: String,
    val repository: String,
    val ref: String
)
