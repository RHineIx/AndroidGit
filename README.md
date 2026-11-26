# AndroidGit

A native Android Git client built with Kotlin and Jetpack Compose, featuring a ColorOS 17 interface powered by **COUI**, **Haze**, and **Tabler Icons**.

Manage repositories, branches, commits, SSH authentication, and GitHub Actions from a consistent mobile interface.

## Interface

- **COUI** provides components, themes, cards, and controls.
- **Haze** adds frosted headers, dialogs, and menus.
- **Tabler Icons** provides a consistent set of outline icons.

The interface supports light, dark, and system themes, with centered progressive-blur headers, compact loading indicators, and dropdown menus that dismiss when tapping outside.

See the [UI implementation notes](docs/UI_REFACTOR.md) for library versions and design details.

## Build

Build a Release APK using a full JDK and Android SDK 37:

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

## Releases

Push a version tag on main, for example `v7.1.0` or `v7.1.0-stable`. GitHub Actions builds and signs the arm64-v8a APK, checks its version, ABI and signature, and publishes the APK and SHA-256 checksum under that exact tag. Other suffixes (for example `-rc.1`) publish as prereleases.

Version name is the tag without the initial v; versionCode is major × 10000 + minor × 100 + patch (minor/patch 0–99). Suffixes share the same numeric code; use a higher numeric version for a strictly increasing code.

For internal testing, manually run **AndroidGit Release** on a branch. The optional version_tag input overrides the nearest reachable release tag; no tag or GitHub Release is created. Download the signed APK from Actions artifacts.

Local builds use the default version or `./gradlew :app:assembleRelease -PreleaseTag=v7.1.0`. CI never rewrites version fields or pushes version-bump commits.
