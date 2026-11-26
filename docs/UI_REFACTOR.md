# AndroidGit UI implementation

AndroidGit uses native Jetpack Compose Material 3, Haze 2.1.0 and Tabler Icons 3.50.0. The UI redesign restores the original Material card-based dashboard and keeps current Git operations, authentication, settings and release automation.

## Shared design system

- `GitPage.kt`: fixed, centered toolbar with progressive 32dp backdrop blur and gradient scrim. The header respects system insets, RTL and increased font size; older Android versions use a scrim fallback.
- `MaterialComponents.kt`: native Material controls with shared sizing, rounded cards, segmented tabs, menus and dialogs. Primary actions target a 52dp minimum, icon actions a 48dp touch target, and text fields a 56dp minimum.
- `PreferenceComponents.kt`: grouped cards, 64dp minimum preference rows, consistent spacing and accessible native click handling.
- `ActionTile.kt`: 116dp minimum quick-action tiles with clear icon and label hierarchy.
- `GitAuthenticationPanel.kt`: common form state and credential controls for Settings, Repository Settings and Clone.
- `AppearancePreferences.kt`: System/Light/Dark modes, default Material palette, Android system dynamic colors (API 31+), custom accent and standard/AMOLED dark surfaces. Selection persists without restarting.

The app uses the original Material-inspired palette, 16dp page margins, 18dp medium corners, 24dp large corners, and accessible typography. Dynamic colors fall back to the default palette on older devices.

## Build and release

Use a full JDK and Android SDK 37:

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

Official releases are triggered by version tags on main; manual workflow dispatch builds signed internal artifacts only. Version fields are derived from the tag without modifying repository files. Release builds retain code/resource shrinking, signing and arm64 verification.

## Verification

The preceding build at cd8f163 passed CI. This UI redesign requires a fresh compilation and on-device visual verification of scrolling, dialogs, keyboard, theme changes and authentication.
