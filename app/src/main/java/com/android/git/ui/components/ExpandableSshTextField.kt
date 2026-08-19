package com.android.git.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun ExpandableSshTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    leadingIcon: ImageVector,
    enabled: Boolean,
    isSecret: Boolean = false,
    isError: Boolean = false,
    supportingText: (@Composable (() -> Unit))? = null,
    expandDescription: String,
    collapseDescription: String,
    showDescription: String,
    hideDescription: String,
    modifier: Modifier = Modifier,
    maxExpandedLines: Int = 8,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default
) {
    var manuallyExpanded by rememberSaveable { mutableStateOf(false) }
    var focused by rememberSaveable { mutableStateOf(false) }
    var visible by rememberSaveable { mutableStateOf(false) }
    val expanded = manuallyExpanded || focused

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = {
            Row {
                if (isSecret) {
                    IconButton(onClick = { visible = !visible }, enabled = enabled) {
                        Icon(
                            imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (visible) hideDescription else showDescription
                        )
                    }
                }
                IconButton(
                    onClick = { manuallyExpanded = !manuallyExpanded },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) collapseDescription else expandDescription
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .onFocusChanged { focused = it.isFocused },
        singleLine = !expanded,
        minLines = if (expanded) 3 else 1,
        maxLines = if (expanded) maxExpandedLines else 1,
        visualTransformation = if (isSecret && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        isError = isError,
        supportingText = supportingText,
        enabled = enabled
    )
}
