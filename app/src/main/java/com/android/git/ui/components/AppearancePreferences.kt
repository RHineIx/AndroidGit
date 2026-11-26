package com.android.git.ui.components

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.data.AppearanceSettings
import com.android.git.data.ThemeMode
import com.android.git.data.ThemePalette

/** Brightness and Android dynamic colors are independent, just like the original Material theme. */
@Composable
internal fun AppearancePreferences(
    mode: ThemeMode,
    appearance: AppearanceSettings,
    onModeChange: (ThemeMode) -> Unit,
    onAppearanceChange: (AppearanceSettings) -> Unit
) {
    SectionTitle(stringResource(R.string.appearance_section))
    PreferenceGroup {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_theme_title), style = MaterialTheme.typography.titleMedium)
            SegmentedTabs(
                tabs = ThemeMode.entries.map { choice ->
                    when (choice) {
                        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                    }
                },
                selectedTabIndex = mode.ordinal,
                onTabSelected = { onModeChange(ThemeMode.entries[it]) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        PreferenceDivider()
        val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val dynamic = appearance.palette == ThemePalette.SYSTEM
        val toggle = { checked: Boolean ->
            onAppearanceChange(appearance.copy(palette = if (checked) ThemePalette.SYSTEM else ThemePalette.DEFAULT))
        }
        PreferenceRow(
            title = stringResource(R.string.appearance_system_colors),
            summary = stringResource(if (dynamicSupported) R.string.appearance_system_colors_hint else R.string.appearance_dynamic_fallback),
            enabled = dynamicSupported,
            modifier = Modifier.toggleable(value = dynamic && dynamicSupported, enabled = dynamicSupported, role = Role.Switch, onValueChange = toggle),
            trailing = { Switch(checked = dynamic && dynamicSupported, onCheckedChange = null, enabled = dynamicSupported) }
        )
    }
}
