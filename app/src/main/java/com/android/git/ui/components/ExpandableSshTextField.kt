package com.android.git.ui.components

import com.android.git.ui.components.GitIcons as Icons

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch

@Composable
fun ExpandableSshTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    leadingIcon: ImageVector,
    enabled: Boolean,
    showDescription: String,
    hideDescription: String,
    modifier: Modifier = Modifier,
    isSecret: Boolean = false,
    isError: Boolean = false,
    supportingText: (@Composable (() -> Unit))? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    maxExpandedLines: Int = 8,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var focused by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val expanded = focused
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(focused) {
        if (focused) {
            withFrameNanos { }
            bringIntoViewRequester.bringIntoView()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = {
            if (isSecret) {
                IconButton(onClick = { visible = !visible }, enabled = enabled) {
                    Icon(
                        imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (visible) hideDescription else showDescription
                    )
                }
            }
        },
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .animateContentSize(finishedListener = { _, _ ->
                // Expanding a key can move its last lines below the current viewport.
                if (focused) scope.launch { bringIntoViewRequester.bringIntoView() }
            })
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
