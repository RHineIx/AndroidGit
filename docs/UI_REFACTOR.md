# AndroidGit UI implementation

AndroidGit uses COUI, Haze, and Tabler Icons to provide a consistent ColorOS 16 interface across its Git tools and settings.

## UI libraries

| Library | Version | Purpose |
| --- | --- | --- |
| COUI UI | `1.1.0` | Themes, headers, cards, preference rows, buttons, fields, dialogs, menus, and bottom sheets |
| Haze and Haze Blur | `2.0.1` | Frosted surfaces on Android 12 and later, with solid backgrounds on older versions |
| Tabler Icons | `3.49.0` | Outline icons shared through `GitIcons` |

Jetpack Compose, AndroidX Navigation, and the existing ViewModel architecture continue to support the screens and their behavior.

## Shared components

- `ui/theme` configures COUI themes and matching Material colors for specialized Git controls.
- `OplusPage.kt` provides the page layout, collapsing header, and shared blur source.
- `OplusPreference.kt` provides grouped cards, preference rows, and dividers.
- `OplusWidgets.kt` adapts the screen components to COUI controls while preserving callbacks and validation.
- `GitIcons.kt` maps icon names to Tabler outline icons.

These components cover repository selection, cloning, the dashboard, changes, branches, history, stashes, conflicts, the gitignore editor, GitHub Actions, and settings. Git operations, authentication, SSH, Gemini, and confirmation flows retain their existing behavior.

## Appearance

| Element | Light | Dark |
| --- | --- | --- |
| Page background | `#F0F1F2` | `#000000` |
| Card background | `#FFFFFF` | `#1A1A1A` |
| Accent | `#0066FF` | `#247CFF` |
| Primary text | `#E6000000` | `#E6FFFFFF` |
| Secondary text | `#66000000` | `#66FFFFFF` |
| Error | `#DB382C` | `#FF6C61` |

The app supports light, dark, and system themes with fixed accent colors. Typography uses the system font, including semibold expanded titles and medium collapsed titles.

Shared layout and interaction details:

- Cards use a 12dp corner radius, with 12dp page margins and thin inset dividers.
- Preference rows use 22dp leading icons and an 8dp gap before the text.
- Headers collapse while scrolling and use a 96% opaque surface to keep content legible.
- Frosted dialogs and menus use 24dp blur and 94% opaque surfaces. Dialogs use stronger field outlines and clearer secondary text.
- Dropdown panels have bounded dimensions, while their outside-tap area covers the full window. Theme and Gemini menus dismiss when tapping outside.
- Indeterminate loading indicators are capped at 26dp and shrink to fit smaller controls.
- Editable field text explicitly uses the current surface text color, including inside workflow dialogs.
- Commit details use full-width rows to accommodate long hashes and dates.

## Build and review

Use a full JDK and Android SDK 37:

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

Release builds use code and resource shrinking. Signing and publishing are configured through the manually triggered GitHub Actions workflow; local signing credentials and SDK paths are kept outside version control.

The UI changes passed Release compilation and lint with no errors and 108 warnings. Final visual review is performed on the project owner's device. Private screenshots and account details are not included in the repository.
