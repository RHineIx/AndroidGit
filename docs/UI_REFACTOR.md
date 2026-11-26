# AndroidGit UI implementation

AndroidGit uses COUI, Haze, and Tabler Icons to provide a consistent ColorOS 17 interface across its Git tools and settings.

## UI libraries

| Library | Version | Purpose |
| --- | --- | --- |
| COUI UI | `2.1.0` | Themes, headers, cards, preference rows, buttons, fields, dialogs, menus, and bottom sheets |
| Haze and Haze Blur | `2.1.0` | Frosted surfaces on Android 12 and later, with solid backgrounds on older versions |
| Tabler Icons | `3.50.0` | Outline icons shared through `GitIcons` |

Jetpack Compose, AndroidX Navigation, and the existing ViewModel architecture continue to support the screens and their behavior.

## Shared components

- `ui/theme` configures COUI themes and matching Material colors for specialized Git controls.
- `OplusPage.kt` provides the page layout, fixed centered header, and shared blur source.
- `OplusPreference.kt` provides grouped cards, preference rows, and dividers.
- `OplusActionCard.kt` provides standalone icon-and-label cards for repository entry points and quick actions.
- `OplusWidgets.kt` adapts the screen components to COUI controls while preserving callbacks and validation.
- `GitIcons.kt` maps icon names to Tabler outline icons.

These components cover repository selection, cloning, the dashboard, changes, branches, history, stashes, conflicts, the gitignore editor, GitHub Actions, and settings. Git operations, authentication, SSH, Gemini, and confirmation flows retain their existing behavior.

## Appearance

The app uses the COUI 17 color scheme and typography, with corresponding Material colors for specialized Git controls. The Appearance section lets the user independently select System / Light / Dark brightness, Default COUI / Android dynamic system colors / Custom accent, and Standard / AMOLED dark surfaces. Choices persist and apply immediately. Dynamic system colors require Android 12 or later; older devices fall back to COUI. The custom accent supports presets and any six-digit RGB hex value, with contrast adjustment.

Shared layout and interaction details:

- Cards use a 12dp corner radius, with 12dp page margins and thin inset dividers.
- Clone Repo and Open Local use equal-width cards in one row. Dashboard Pull and Push use the same compact layout: 84dp minimum height, 24dp icons, 12dp padding and corners, and an 8dp gap between cards. Labels wrap and cards grow for larger font sizes; the force-push switch remains in its own card and retains its confirmation dialog.
- Preference rows use 22dp leading icons and an 8dp gap before the text.
- Headers remain fixed and center their title and optional subtitle in the available safe width, reserving symmetric space for navigation and action buttons. A 32dp Haze progressive blur fades from the status bar through the bottom edge; Android versions below API 31 use a gradient scrim instead. Titles and controls are rendered separately from the blur.
- Frosted dialogs and menus use 24dp blur and 94% opaque surfaces. Dialogs use stronger field outlines and clearer secondary text.
- Dropdown panels have bounded dimensions, while their outside-tap area covers the full window. Theme and Gemini menus dismiss when tapping outside.
- Indeterminate loading indicators are capped at 26dp and shrink to fit smaller controls.
- Editable field text explicitly uses the current surface text color, including inside workflow dialogs.
- The app root applies and consumes IME padding once, so all pages resize above the keyboard, including SSH forms, the gitignore editor, and the full-screen commit editor. The commit bar uses this same viewport without adding another keyboard inset. Focused SSH fields also request visibility after their expansion animation. COUI dialogs and bottom sheets keep their independent window-level IME handling.
- Commit details use full-width rows to accommodate long hashes and dates.

## Build and review

Use a full JDK and Android SDK 37:

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

Release builds use code and resource shrinking. Signing and publishing are configured through the manually triggered GitHub Actions workflow; local signing credentials and SDK paths are kept outside version control.

COUI 2.1.0 changes color and text style slots and component motion. Release compilation, lint and visual review on a device are required for this upgrade.

## Dependency upgrade policy

- All direct dependency versions are pinned in `gradle/libs.versions.toml`.
- Kotlin/Compose compiler 2.4.21, AGP 9.3.1 and Gradle 9.7.0 follow the documented fully supported Kotlin compatibility range; AGP 9.3 also supports API 37.
- Activity Compose 1.13.0 is retained as the current stable release. The legacy Gemini Android client remains at 0.9.0 pending a separate migration; security-crypto 1.1.0 is retained pending a separate credential-storage migration.
- JGit and its SSH module share one version; Apache SSHD stays on stable 2.x. Test SSH clone, fetch, pull and push on Android before merging.
- Authentication uses one shared COUI card and state holder for Settings, Repository Settings and Clone. The settings screens share Save; Clone retains its Clone action.

References: [COUI upgrade guide](https://github.com/suqi8/coui/blob/v2.1.0/docs/guide/upgrading.md), [Kotlin compatibility](https://kotlinlang.org/docs/gradle-configure-project.html), [Haze changelog](https://github.com/chrisbanes/haze/blob/2.1.0/CHANGELOG.md).
