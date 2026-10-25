# AndroidGit

Android Git client built with Kotlin and Jetpack Compose, with a ColorOS16 interface using COUI, Haze and Tabler icons.

The UI follows [OPlusKey-lsposed](https://github.com/lo0kie/OPlusKey-lsposed) while retaining AndroidGit's repository, branch, commit, SSH and GitHub Actions features. The [UI analysis and implementation notes](docs/UI_REFACTOR.md) document the reference versions, colors, geometry and remaining differences.

Build a debug APK using a full JDK and Android SDK37:

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

Release signing and publishing remain in the existing manually invoked GitHub Actions workflow.
