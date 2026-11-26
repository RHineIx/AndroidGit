package com.android.git.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.data.AppearanceSettings
import com.android.git.data.DarkSurface
import com.android.git.data.ThemeMode
import com.android.git.data.ThemePalette
import java.util.Locale

private val accentPresets = listOf(
    0xFF0080FF.toInt(), 0xFF6750A4.toInt(), 0xFF007A59.toInt(), 0xFFBD3858.toInt(),
    0xFFC15D13.toInt(), 0xFF007F91.toInt(), 0xFF9C45A9.toInt(), 0xFF526170.toInt()
)

@Composable
internal fun AppearancePreferences(
    mode: ThemeMode,
    appearance: AppearanceSettings,
    onModeChange: (ThemeMode) -> Unit,
    onAppearanceChange: (AppearanceSettings) -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.appearance_section))
        PreferenceGroup {
            AppearanceChoice(
                title = stringResource(R.string.settings_theme_title),
                choices = ThemeMode.entries,
                selected = mode,
                label = { choice ->
                    when (choice) {
                        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                    }
                },
                onSelect = onModeChange
            )
            PreferenceDivider()
            AppearanceChoice(
                title = stringResource(R.string.appearance_palette),
                choices = ThemePalette.entries,
                selected = appearance.palette,
                label = { choice ->
                    when (choice) {
                        ThemePalette.DEFAULT -> stringResource(R.string.appearance_default)
                        ThemePalette.SYSTEM -> stringResource(R.string.appearance_system_colors)
                        ThemePalette.CUSTOM -> stringResource(R.string.appearance_custom)
                    }
                },
                onSelect = { onAppearanceChange(appearance.copy(palette = it)) }
            )
            if (appearance.palette == ThemePalette.SYSTEM && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Text(
                    stringResource(R.string.appearance_dynamic_fallback),
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PreferenceDivider()
            AppearanceChoice(
                title = stringResource(R.string.appearance_dark_surface),
                choices = DarkSurface.entries,
                selected = appearance.darkSurface,
                label = { choice ->
                    when (choice) {
                        DarkSurface.STANDARD -> stringResource(R.string.appearance_standard)
                        DarkSurface.AMOLED -> stringResource(R.string.appearance_amoled)
                    }
                },
                onSelect = { onAppearanceChange(appearance.copy(darkSurface = it)) }
            )
            Text(
                stringResource(R.string.appearance_dark_surface_hint),
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            if (appearance.palette == ThemePalette.CUSTOM) {
                PreferenceDivider()
                AccentEditor(appearance.customAccent) {
                    onAppearanceChange(appearance.copy(customAccent = it))
                }
            }
        }
        PreferenceGroup {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.appearance_preview), style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.background).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.appearance_preview_title), color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            hexColor(MaterialTheme.colorScheme.primary.toArgb()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(GitIcons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun <T> AppearanceChoice(
    title: String,
    choices: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val summary = label(selected)
    Box {
        PreferenceRow(
            title = title,
            summary = summary,
            onClick = { expanded = true },
            trailing = { Icon(GitIcons.Default.ArrowDropDown, null) }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            width = 252.dp
        ) {
            choices.forEachIndexed { index, choice ->
                DropdownMenuItem(
                    text = {
                        Text(label(choice), color = if (choice == selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface)
                    },
                    leadingIcon = {
                        if (choice == selected) Icon(GitIcons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        else Spacer(Modifier.size(24.dp))
                    },
                    showDivider = index > 0,
                    onClick = {
                        expanded = false
                        onSelect(choice)
                    }
                )
            }
        }
    }
}

@Composable
private fun AccentEditor(accent: Int, onAccentChange: (Int) -> Unit) {
    var input by remember(accent) { mutableStateOf(hexColor(accent)) }
    val normalized = input.trim().removePrefix("#")
    val parsed = normalized.takeIf { it.length == 6 && it.all { ch -> ch in '0'..'9' || ch.lowercaseChar() in 'a'..'f' } }
        ?.toLongOrNull(16)?.let { (it or 0xFF000000L).toInt() }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.appearance_primary_color), style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            accentPresets.chunked(4).forEach { colors ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    colors.forEach { argb ->
                        val color = Color(argb)
                        val selected = accent == argb
                        Box(
                            Modifier.size(48.dp)
                                .clip(CircleShape)
                                .selectable(selected = selected, role = Role.RadioButton, onClick = { onAccentChange(argb) })
                                .semantics { contentDescription = hexColor(argb) }
                                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                .padding(5.dp).background(color, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Icon(
                                GitIcons.Default.Check, null,
                                tint = if (color.luminance() > 0.45f) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { if (it.length <= 7) input = it },
            label = { Text(stringResource(R.string.appearance_hex)) },
            modifier = Modifier.fillMaxWidth(),
            isError = input.isNotBlank() && parsed == null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            leadingIcon = {
                Box(Modifier.size(22.dp).clip(CircleShape).background(parsed?.let { Color(it) } ?: Color(accent)))
            }
        )
        Button(
            onClick = { parsed?.let(onAccentChange) },
            enabled = parsed != null && parsed != accent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.appearance_apply_color))
        }
        Text(
            stringResource(R.string.appearance_contrast_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun hexColor(argb: Int): String = String.format(Locale.ROOT, "#%06X", argb and 0xFFFFFF)
