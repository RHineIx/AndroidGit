# AndroidGit

A native Android Git client built with Kotlin, Jetpack Compose Material 3, and Tabler Icons.

Manage local repositories, branches, commits, SSH authentication, and GitHub Actions. Supports light, dark and system themes.

## Build

Use a full JDK and Android SDK 37:

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

Release signing and publishing use the manually triggered GitHub Actions workflow.
