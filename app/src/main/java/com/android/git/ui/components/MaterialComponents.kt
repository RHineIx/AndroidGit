@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.android.git.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (border == null) androidx.compose.material3.Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation, content = content)
    else androidx.compose.material3.OutlinedCard(modifier = modifier, shape = shape, colors = colors, elevation = elevation, border = border, content = content)
}

@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    content: @Composable ColumnScope.() -> Unit,
) = androidx.compose.material3.ElevatedCard(modifier = modifier, shape = shape, colors = colors, elevation = elevation, content = content)

@Composable
fun ElevatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) = androidx.compose.material3.ElevatedCard(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors, elevation = elevation, interactionSource = interactionSource, content = content)

@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) = androidx.compose.material3.OutlinedCard(modifier = modifier, shape = shape, colors = colors, elevation = elevation, border = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), content = content)

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.Button(
    onClick = onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled,
    shape = shape, colors = colors, elevation = elevation, border = border,
    contentPadding = contentPadding, interactionSource = interactionSource, content = content
)

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.OutlinedButton(
    onClick = onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled,
    shape = shape, colors = colors, border = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    contentPadding = contentPadding, interactionSource = interactionSource, content = content
)

@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.FilledTonalButton(
    onClick = onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled,
    shape = shape, colors = colors, elevation = elevation, border = border,
    contentPadding = contentPadding, interactionSource = interactionSource, content = content
)

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.TextButton(
    onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled,
    shape = shape, colors = colors, elevation = elevation, border = border,
    contentPadding = contentPadding, interactionSource = interactionSource, content = content
)

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = androidx.compose.material3.IconButton(onClick = onClick, modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), enabled = enabled, colors = colors, interactionSource = interactionSource, content = content)

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
    thumbContent: (@Composable () -> Unit)? = null,
) = androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, thumbContent = thumbContent, enabled = enabled, colors = colors, interactionSource = interactionSource)

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) = androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled, colors = colors, interactionSource = interactionSource)

@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) = androidx.compose.material3.RadioButton(selected = selected, onClick = onClick, modifier = modifier, enabled = enabled, colors = colors, interactionSource = interactionSource)

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = MaterialTheme.shapes.small,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) = androidx.compose.material3.OutlinedTextField(
    value = value, onValueChange = onValueChange, modifier = modifier.heightIn(min = 56.dp),
    enabled = enabled, readOnly = readOnly, textStyle = textStyle, label = label,
    placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
    prefix = prefix, suffix = suffix, supportingText = supportingText,
    isError = isError, visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions, singleLine = singleLine, maxLines = maxLines, minLines = minLines,
    interactionSource = interactionSource, shape = shape, colors = colors
)

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = MaterialTheme.shapes.small,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) = androidx.compose.material3.OutlinedTextField(
    value = value, onValueChange = onValueChange, modifier = modifier.heightIn(min = 56.dp),
    enabled = enabled, readOnly = readOnly, textStyle = textStyle, label = label,
    placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
    supportingText = supportingText,
    isError = isError, visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions, singleLine = singleLine, maxLines = maxLines, minLines = minLines,
    interactionSource = interactionSource, shape = shape, colors = colors
)

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    neutralButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    iconContentColor: Color = MaterialTheme.colorScheme.primary,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    properties: DialogProperties = DialogProperties(),
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest, confirmButton = confirmButton, modifier = modifier,
        dismissButton = if (neutralButton != null || dismissButton != null) {
            { Column(horizontalAlignment = Alignment.End) { neutralButton?.invoke(); dismissButton?.invoke() } }
        } else null,
        icon = icon, title = title, text = text, shape = shape, containerColor = containerColor,
        iconContentColor = iconContentColor, titleContentColor = titleContentColor,
        textContentColor = textContentColor, tonalElevation = tonalElevation, properties = properties
    )
}

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    width: Dp = 192.dp,
    maxHeight: Dp = 360.dp,
    content: @Composable ColumnScope.() -> Unit,
) = androidx.compose.material3.DropdownMenu(
    expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier.widthIn(max = width).heightIn(max = maxHeight),
    offset = offset, properties = properties, shape = shape, containerColor = containerColor, content = content
)

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    interactionSource: MutableInteractionSource? = null,
    showDivider: Boolean = false,
) {
    if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    androidx.compose.material3.DropdownMenuItem(text = text, onClick = onClick, modifier = modifier.heightIn(min = 48.dp),
        leadingIcon = leadingIcon, trailingIcon = trailingIcon, enabled = enabled,
        colors = colors, contentPadding = contentPadding, interactionSource = interactionSource)
}

@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    sheetMaxWidth: Dp = Dp.Unspecified,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = Color.Black.copy(alpha = 0.4f),
    dragHandle: (@Composable () -> Unit)? = null,
    title: String? = null,
    endAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = androidx.compose.material3.ModalBottomSheet(
    onDismissRequest = onDismissRequest, modifier = modifier, sheetState = sheetState,
    sheetMaxWidth = sheetMaxWidth, shape = shape, containerColor = containerColor,
    contentColor = contentColor, tonalElevation = tonalElevation, scrimColor = scrimColor,
    dragHandle = dragHandle ?: { BottomSheetDefaults.DragHandle() }
) {
    if (title != null || endAction != null) Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title.orEmpty(), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        endAction?.invoke()
    }
    content()
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp,
    trackColor: Color = Color.Transparent,
) = androidx.compose.material3.CircularProgressIndicator(modifier = modifier.size(28.dp), color = color, strokeWidth = strokeWidth, trackColor = trackColor)

@Composable
fun CircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp,
    trackColor: Color = Color.Transparent,
) = androidx.compose.material3.CircularProgressIndicator(progress = progress, modifier = modifier, color = color, strokeWidth = strokeWidth, trackColor = trackColor)

@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) = androidx.compose.material3.LinearProgressIndicator(modifier = modifier, color = color, trackColor = trackColor)

@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) = androidx.compose.material3.LinearProgressIndicator(progress = progress, modifier = modifier, color = color, trackColor = trackColor)

/** Reusable native segmented control for authentication and branch filtering. */
@Composable
fun SegmentedTabs(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .selectableGroup().padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = selectedTabIndex == index
            Box(
                Modifier.weight(1f).clip(MaterialTheme.shapes.large)
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = { onTabSelected(index) })
                    .heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = (if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        .copy(alpha = if (enabled) 1f else 0.38f))
            }
        }
    }
}
