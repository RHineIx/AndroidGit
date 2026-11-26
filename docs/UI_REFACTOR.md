# AndroidGit UI implementation

AndroidGit uses native Jetpack Compose Material 3, Haze 2.1.0 and Tabler Icons 3.50.0. The UI redesign restores the original Material card-based dashboard and keeps current Git operations, authentication, settings and release automation.

## Shared design system

- `GitPage.kt`: fixed, centered toolbar with progressive 32dp backdrop blur and gradient scrim. The header respects system insets, RTL and increased font size; older Android versions use a scrim fallback.
- `MaterialComponents.kt`: native Material controls with shared sizing, rounded cards, segmented tabs, menus and dialogs. Primary actions target a 52dp minimum, icon actions a 48dp touch target, and text fields a 56dp minimum.
- `PreferenceComponents.kt`: grouped cards, 64dp minimum preference rows, consistent spacing and accessible native click handling.
- `ActionTile.kt`: 84dp minimum quick-action tiles with clear icon and label hierarchy.
- `GitAuthenticationPanel.kt`: common form state and credential controls for Settings, Repository Settings and Clone.
- `AppearancePreferences.kt`: System/Light/Dark modes and an independent Android system colors switch (API 31+). Uses the GitHub Primer Tritanopia-inspired palette when disabled. Selection persists without restarting; obsolete experimental appearance preferences are migrated.

The app uses the original Material palette, 16dp page margins, 18dp medium corners, 24dp large corners, and accessible typography. Dynamic colors fall back to the default palette on older devices.

## Build and release

Use a full JDK and Android SDK 37:

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

Official releases are triggered by version tags on main; manual workflow dispatch builds signed internal artifacts only. Version fields are derived from the tag without modifying repository files. Release builds retain code/resource shrinking, signing and arm64 verification.

## Verification

The preceding build at cd8f163 passed CI. This UI redesign requires a fresh compilation and on-device visual verification of scrolling, dialogs, keyboard, theme changes and authentication.

### Compact layout and selection controls

Repository status cards share the primary container color and 8dp spacing. Action tiles use an 84dp minimum height, with a 60dp full-width Workflow action. Theme brightness uses a segmented control and dynamic colors use a switch. Selection menus anchor to the trailing button; page-local Haze sources prevent cross-destination sampling during short fade transitions.

### Visual polish

The default palette maps GitHub Primer Tritanopia neutral, blue and red colors to Material 3 roles. Android dynamic colors remain an independent switch. Filled text fields use the native filled Material component, avoiding the outlined label cutout over a colored container. Segmented controls animate one RTL-aware selection pill. Navigation Compose 2.10 overrides both regular and predictive pop transitions with full-size fade/slide transitions, without scaling destinations or disabling predictive back. Source: https://github.com/primer/primitives/tree/v10.0.0/src/tokens/functional/color.

### Compact segmented controls and refined Tritanopia palette

SegmentedTabs draws a single animated pill with a 220ms non-overshooting transition. The track is at least 48dp high (40dp visible pill at default size), retains 48dp touch targets and expands for large text. Per-segment ripple is intentionally disabled; keyboard focus retains a border, and label colors follow indicator position. RTL, disabled state and accessibility selection semantics remain supported.

Dark colors use the UI tokens from [GitHub Dark Tritanopia for VS Code](https://github.com/moelzanaty3/github-dark-tritanopia/blob/main/themes/github-dark-tritanopia-color-theme.json) (blob 1ffaf3fd098f090c353b3b0c42f81f854d91e2e3): #0D1117 editor, #010409 panel, #4493F8 accent, #F0F6FC text, #9198A1 muted text, #3D444D divider and #F85149 error. The translucent blue/neutral selection colors are composited over #151B23 for mobile surfaces. Light mode retains official Primer light tokens; the reference extension contains no light theme. System colors remain optional. Stronger interactive outlines are retained for readability.

### GitHub web screenshot calibration

Dark palette calibrated against seven supplied mobile GitHub screenshots. Large flat JPEG regions yield #0D1017 (page), #010409 (menu/header), #141B23 (field/table header), #212830 (secondary buttons), and approximately #1F6FEB (solid blue Code button). Normalize the first and third to #0D1117 and #151B23; compression causes small deviations. Keep #4493F8 as the readable link/accent token and use #1F6FEB with white for solid actions and selected segments (4.63:1 contrast). Use #121D2F for subtle selected surfaces, #3D444D for dividers and #656C76 for accessible interactive outlines. The header uses the darker GitHub menu surface as its progressive blur scrim. The supplied screenshots do not show light mode, so the existing Primer-based light scheme is retained. Android system colors bypass the GitHub-specific button and toolbar overrides. No geometry, animation, dependencies or release workflow changes.
