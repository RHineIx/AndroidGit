package com.android.git.ui.components

import com.android.git.ui.components.GitIcons as Icons
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.android.git.R
import com.android.git.data.CloneAuthChoice
import com.android.git.data.CloneAuthError

/** Clone-only controls. Secrets remain in the destination's in-memory state. */
@Composable
internal fun CloneAuthentication(
    choice: CloneAuthChoice,
    onChoiceChange: (CloneAuthChoice) -> Unit,
    isSsh: Boolean,
    urlEntered: Boolean,
    savedHttpsAvailable: Boolean,
    savedSshAvailable: Boolean,
    customToken: String,
    onTokenChange: (String) -> Unit,
    customKey: String,
    onKeyChange: (String) -> Unit,
    customPassphrase: String,
    onPassphraseChange: (String) -> Unit,
    error: CloneAuthError?,
    enabled: Boolean
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.settings_auth_section))
        PreferenceGroup {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.selectableGroup()) {
                    CloneAuthChoice.entries.forEach { option ->
                        val title = when (option) {
                            CloneAuthChoice.SAVED -> R.string.clone_auth_saved
                            CloneAuthChoice.NONE -> R.string.clone_auth_none
                            CloneAuthChoice.CUSTOM -> R.string.clone_auth_custom
                        }
                        val summary = when (option) {
                            CloneAuthChoice.SAVED -> R.string.clone_auth_saved_hint
                            CloneAuthChoice.NONE -> R.string.clone_auth_none_hint
                            CloneAuthChoice.CUSTOM -> R.string.clone_auth_custom_hint
                        }
                        Row(
                            Modifier.fillMaxWidth().selectable(
                                selected = option == choice, enabled = enabled, role = Role.RadioButton,
                                onClick = { onChoiceChange(option) }
                            ).heightIn(min = 52.dp).padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = option == choice, onClick = null, enabled = enabled)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(summary), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (choice == CloneAuthChoice.SAVED && urlEntered) {
                    HorizontalDivider()
                    val available = if (isSsh) savedSshAvailable else savedHttpsAvailable
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (available) Icons.Default.CheckCircle else Icons.Default.Info, null,
                            tint = if (available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (isSsh) R.string.auth_mode_ssh else R.string.auth_mode_https),
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (choice == CloneAuthChoice.CUSTOM) {
                    HorizontalDivider()
                    if (isSsh) {
                        ExpandableSshTextField(
                            value = customKey, onValueChange = onKeyChange,
                            label = { Text(stringResource(R.string.ssh_private_key_label)) },
                            leadingIcon = Icons.Default.Key, enabled = enabled, isSecret = true,
                            showDescription = stringResource(R.string.ssh_show_value),
                            hideDescription = stringResource(R.string.ssh_hide_value), maxExpandedLines = 10
                        )
                        ExpandableSshTextField(
                            value = customPassphrase, onValueChange = onPassphraseChange,
                            label = { Text(stringResource(R.string.ssh_passphrase_label)) },
                            leadingIcon = Icons.Default.Lock, enabled = enabled, isSecret = true,
                            showDescription = stringResource(R.string.ssh_show_value),
                            hideDescription = stringResource(R.string.ssh_hide_value),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), maxExpandedLines = 3
                        )
                    } else {
                        ExpandableSshTextField(
                            value = customToken, onValueChange = onTokenChange,
                            label = { Text(stringResource(R.string.repo_settings_label_token)) },
                            leadingIcon = Icons.Default.VpnKey, enabled = enabled, isSecret = true,
                            showDescription = stringResource(R.string.ssh_show_value),
                            hideDescription = stringResource(R.string.ssh_hide_value),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), maxExpandedLines = 3
                        )
                    }
                }
                val message = when (error) {
                    CloneAuthError.SSH_REQUIRED -> R.string.clone_auth_ssh_required
                    CloneAuthError.TOKEN_REQUIRED -> R.string.clone_auth_token_required
                    CloneAuthError.KEY_REQUIRED -> R.string.clone_auth_key_required
                    CloneAuthError.HTTPS_REQUIRED -> R.string.clone_auth_https_required
                    CloneAuthError.SAVED_GITHUB_ONLY -> R.string.clone_auth_github_only
                    else -> null
                }
                if (message != null) Text(stringResource(message), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
