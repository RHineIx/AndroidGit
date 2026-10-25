# AndroidGit

A native Android Git client built with Kotlin and Jetpack Compose, featuring a ColorOS 16 interface powered by **COUI**, **Haze**, and **Tabler Icons**.

Manage repositories, branches, commits, SSH authentication, and GitHub Actions from a consistent mobile interface.

## Interface

- **COUI** provides components, themes, cards, and controls.
- **Haze** adds frosted headers, dialogs, and menus.
- **Tabler Icons** provides a consistent set of outline icons.

The interface supports light, dark, and system themes, with collapsing headers, compact loading indicators, and dropdown menus that dismiss when tapping outside.

See the [UI implementation notes](docs/UI_REFACTOR.md) for library versions and design details.

## Build

Build a Release APK using a full JDK and Android SDK 37:

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

Release signing and publishing are handled by the manually triggered GitHub Actions workflow.
