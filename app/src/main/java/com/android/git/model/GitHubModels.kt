package com.android.git.model

/** Authenticated GitHub profile returned by GET /user. */
data class GitHubAccount(
    val login: String,
    val name: String,
    val avatarUrl: String,
    val htmlUrl: String,
    val bio: String,
    val publicRepos: Int,
    val privateRepos: Int,
    val followers: Int,
    val following: Int
) {
    val totalRepositories: Int
        get() = publicRepos + privateRepos
}

data class GitHubRepository(
    val id: Long,
    val name: String,
    val fullName: String,
    val ownerLogin: String,
    val description: String,
    val isPrivate: Boolean,
    val htmlUrl: String,
    val cloneUrl: String,
    val sshUrl: String,
    val defaultBranch: String,
    val language: String,
    val stars: Int,
    val forks: Int,
    val updatedAt: String
)

data class GitHubAccountSnapshot(
    val account: GitHubAccount,
    val repositories: List<GitHubRepository>
)

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
    val runNumber: Int,
    val status: String,
    val conclusion: String,
    val branch: String,
    val event: String,
    val htmlUrl: String,
    val createdAt: String
)
