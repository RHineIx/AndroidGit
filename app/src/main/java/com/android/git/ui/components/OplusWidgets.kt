@file:Suppress("UNUSED_PARAMETER")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.android.git.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import io.github.suqi8.coui.kmp.basic.Button as CouiButton
import io.github.suqi8.coui.kmp.basic.ButtonColors as CouiButtonColors
import io.github.suqi8.coui.kmp.basic.Card as CouiCard
import io.github.suqi8.coui.kmp.basic.CardDefaults as CouiCardDefaults
import io.github.suqi8.coui.kmp.basic.Checkbox as CouiCheckbox
import io.github.suqi8.coui.kmp.basic.IconButton as CouiIconButton
import io.github.suqi8.coui.kmp.basic.InfiniteProgressIndicator
import io.github.suqi8.coui.kmp.basic.LinearProgressIndicator as CouiLinearProgressIndicator
import io.github.suqi8.coui.kmp.basic.CircularProgressIndicator as CouiCircularProgressIndicator
import io.github.suqi8.coui.kmp.basic.ProgressIndicatorDefaults as CouiProgressDefaults
import io.github.suqi8.coui.kmp.basic.RadioButton as CouiRadioButton
import io.github.suqi8.coui.kmp.basic.Switch as CouiSwitch
import io.github.suqi8.coui.kmp.basic.TextField as CouiTextField
import io.github.suqi8.coui.kmp.basic.TextFieldMode
import io.github.suqi8.coui.kmp.basic.PreciseClickState
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.window.WindowBottomSheet
import io.github.suqi8.coui.kmp.window.WindowDialog
import io.github.suqi8.coui.kmp.window.WindowListPopup

private val LocalDialogAction = staticCompositionLocalOf { false }

/**
 * Native COUI controls with the argument names used by the existing Git screens.
 * Keeping this boundary lets screen callbacks and validation remain unchanged while
 * one set of controls owns geometry, press animations and dark-mode treatment.
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CouiCard(
        modifier = modifier,
        colors = CouiCardDefaults.defaultColors(
            color = colors.containerColor,
            contentColor = colors.contentColor,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.contentColor) { content() }
    }
}

@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    elevation: CardElevation = CardDefaults.cardElevation(),
    content: @Composable ColumnScope.() -> Unit,
) = Card(modifier, shape, colors, elevation, content = content)

@Composable
fun ElevatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    elevation: CardElevation = CardDefaults.cardElevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CouiCard(
        modifier = modifier,
        colors = CouiCardDefaults.defaultColors(
            color = if (enabled) colors.containerColor else colors.disabledContainerColor,
            contentColor = if (enabled) colors.contentColor else colors.disabledContentColor,
        ),
        onClick = if (enabled) onClick else null,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) colors.contentColor else colors.disabledContentColor,
        ) { content() }
    }
}

@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) = Card(modifier, shape, colors, elevation, border, content)

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val dialogAction = LocalDialogAction.current
    val actionContentColor = if (dialogAction && colors.containerColor.alpha > 0f) colors.containerColor else colors.contentColor
    val disabledActionColor = if (dialogAction) COUITheme.colorScheme.disabledOnSurface else colors.disabledContentColor
    CouiButton(
        onClick = onClick,
        modifier = if (dialogAction) modifier.fillMaxWidth() else modifier,
        enabled = enabled,
        pressScaleEnabled = !dialogAction,
        cornerRadius = if (dialogAction) 0.dp else 22.dp,
        minHeight = if (dialogAction) 58.dp else 44.dp,
        colors = CouiButtonColors(
            color = if (dialogAction) Color.Transparent else colors.containerColor,
            disabledColor = if (dialogAction) Color.Transparent else colors.disabledContainerColor,
            contentColor = actionContentColor,
            disabledContentColor = disabledActionColor,
        ),
        insideMargin = contentPadding,
        interactionSource = interactionSource,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) actionContentColor else disabledActionColor,
        ) {
            ProvideTextStyle(COUITheme.textStyles.button) { content() }
        }
    }
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = Button(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)

@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = Button(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)

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
) = Button(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource, content)

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    CouiIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minWidth = 40.dp,
        minHeight = 40.dp,
        cornerRadius = 20.dp,
        backgroundColor = colors.containerColor,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) colors.contentColor else colors.disabledContentColor,
        ) { content() }
    }
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
    thumbContent: (@Composable () -> Unit)? = null,
) = CouiSwitch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
            .then(
                if (onCheckedChange != null) Modifier.toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange,
                ) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CouiCheckbox(
            state = if (checked) ToggleableState.On else ToggleableState.Off,
            onClick = null,
            enabled = enabled,
        )
    }
}

@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) = CouiRadioButton(selected = selected, onClick = onClick, modifier = modifier, enabled = enabled)

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
) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        if (label != null) {
            CompositionLocalProvider(
                LocalContentColor provides if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                    Box(Modifier.padding(bottom = 8.dp)) { label() }
                }
            }
        }
        Box {
            CouiTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                backgroundMode = TextFieldMode.Rectangle,
                enabled = enabled,
                readOnly = readOnly,
                isError = isError,
                // Window-level COUI dialogs do not inherit Material's content local.
                // Resolve the editor color explicitly, including in dark mode.
                textStyle = textStyle.copy(color = if (textStyle.color.isSpecified) textStyle.color else MaterialTheme.colorScheme.onSurface),
                leadingIcon = leadingIcon?.let { slot ->
                    {
                        Box(Modifier.width(48.dp).heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { slot() }
                        }
                    }
                },
                trailingIcon = trailingIcon?.let { slot ->
                    {
                        Box(Modifier.padding(end = 4.dp).widthIn(min = 40.dp).heightIn(min = 48.dp), contentAlignment = Alignment.Center) { slot() }
                    }
                },
                singleLine = singleLine,
                maxLines = maxLines,
                minLines = minLines,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                interactionSource = interactionSource,
            )
            if (value.isEmpty() && placeholder != null) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    ProvideTextStyle(textStyle) {
                        Box(Modifier.matchParentSize().padding(
                            start = if (leadingIcon == null) 16.dp else 48.dp,
                            end = if (trailingIcon == null) 16.dp else 44.dp,
                            top = if (singleLine) 0.dp else 12.dp
                        ), contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart) {
                            placeholder()
                        }
                    }
                }
            }
        }
        if (supportingText != null) {
            CompositionLocalProvider(
                LocalContentColor provides if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                    Box(Modifier.padding(start = 2.dp, top = 4.dp)) { supportingText() }
                }
            }
        }
    }
}

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
) = OutlinedTextField(
    value = value, onValueChange = onValueChange, modifier = modifier,
    enabled = enabled, readOnly = readOnly, textStyle = textStyle, label = label,
    placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
    supportingText = supportingText, isError = isError, visualTransformation = visualTransformation,
    keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
    singleLine = singleLine, maxLines = maxLines, minLines = minLines,
    interactionSource = interactionSource, shape = shape, colors = colors,
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
    containerColor: Color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF3D3D3D) else Color.White,
    iconContentColor: Color = MaterialTheme.colorScheme.primary,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tonalElevation: Dp = 0.dp,
    properties: DialogProperties = DialogProperties(),
) {
    // COUI exposes a single dismiss callback. Keep the platform's independent
    // dismissal controls when a caller explicitly requests a restricted dialog.
    if (!properties.dismissOnBackPress || !properties.dismissOnClickOutside) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                Column {
                    neutralButton?.invoke()
                    confirmButton()
                }
            },
            modifier = modifier,
            dismissButton = dismissButton,
            icon = icon,
            title = title,
            text = text,
            shape = shape,
            containerColor = containerColor,
            iconContentColor = iconContentColor,
            titleContentColor = titleContentColor,
            textContentColor = textContentColor,
            tonalElevation = tonalElevation,
            properties = properties,
        )
        return
    }
    val hazeState = LocalOplusHazeState.current
    val frosted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null
    val blurStyle = remember { HazeBlurStyle { blurRadius(24.dp); noiseFactor(0f) } }
    val maxDialogHeight = LocalWindowInfo.current.containerDpSize.height * 0.85f
    val largeText = LocalDensity.current.fontScale > 1.25f
    WindowDialog(
        show = true,
        modifier = modifier.then(
            if (frosted) Modifier.clip(RoundedCornerShape(19.dp))
                .hazeBlur(HazeInput.Sources(hazeState), style = blurStyle) else Modifier,
        ),
        backgroundColor = if (frosted) containerColor.copy(alpha = 0.6f) else containerColor,
        onDismissRequest = onDismissRequest,
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxDialogHeight).padding(top = 24.dp)) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                if (icon != null) {
                    CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                        Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp)) { icon() }
                    }
                }
                if (title != null) {
                    CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                        ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)) {
                            Box(
                                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) { title() }
                        }
                    }
                }
                if (text != null) {
                    CompositionLocalProvider(LocalContentColor provides textContentColor) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                            Box(Modifier.padding(horizontal = 24.dp)) { text() }
                        }
                    }
                }
            }
            CompositionLocalProvider(LocalDialogAction provides true) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    if (neutralButton != null || largeText || maxWidth < 320.dp) {
                        Column(Modifier.fillMaxWidth()) {
                            confirmButton()
                            neutralButton?.invoke()
                            dismissButton?.invoke()
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (dismissButton != null) {
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { dismissButton() }
                                Box(
                                    Modifier.padding(top = 17.dp, bottom = 21.dp)
                                        .fillMaxHeight().width(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant),
                                )
                            }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { confirmButton() }
                        }
                    }
                }
            }
        }
    }
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
    preciseClickState: PreciseClickState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hazeState = LocalOplusHazeState.current
    val frosted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null
    val blurStyle = remember { HazeBlurStyle { blurRadius(24.dp); noiseFactor(0f) } }
    val popupSurface = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF3D3D3D) else Color.White
    val windowSize = LocalWindowInfo.current.containerDpSize
    // Bound before applying caller modifiers: a weighted menu row must never
    // expand into the full dialog window or cross a narrow screen's edges.
    val panelWidth = minOf(width, (windowSize.width - 24.dp).coerceAtLeast(1.dp))
    val panelHeight = minOf(maxHeight, (windowSize.height - 48.dp).coerceAtLeast(1.dp))
    WindowListPopup(
        show = expanded,
        popupModifier = Modifier.width(panelWidth).then(modifier),
        minWidth = minOf(168.dp, panelWidth),
        maxHeight = panelHeight,
        preciseClickState = preciseClickState,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = panelHeight).clip(shape).then(
                if (frosted) Modifier
                    .hazeBlur(HazeInput.Sources(hazeState), style = blurStyle)
                    .background(popupSurface.copy(alpha = 0.72f))
                else Modifier.background(containerColor),
            ).verticalScroll(rememberScrollState()),
            content = content,
        )
    }
}

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
    if (showDivider) {
        io.github.suqi8.coui.kmp.basic.HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.33.dp,
            color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f)
        )
    }
    val textColor = if (enabled) colors.textColor else colors.disabledTextColor
    CompositionLocalProvider(LocalContentColor provides textColor) {
        Row(
            modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled, onClick = onClick).padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leadingIcon != null) Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { leadingIcon() }
            Box(Modifier.weight(1f)) {
                ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal)) { text() }
            }
            trailingIcon?.invoke()
        }
    }
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
) {
    WindowBottomSheet(
        show = true,
        title = title,
        endAction = endAction?.let { action ->
            { CompositionLocalProvider(LocalContentColor provides contentColor) { action() } }
        },
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        backgroundColor = containerColor,
        insideMargin = DpSize(0.dp, 0.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Column(content = content)
        }
    }
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp,
    trackColor: Color = Color.Transparent,
) {
    // Native COUI uses a fixed ring diameter. Feed the measured square back into
    // the component so compact button spinners scale with Modifier.size/height.
    BoxWithConstraints(modifier.defaultMinSize(minWidth = 26.dp, minHeight = 26.dp)) {
        val diameter = minOf(
            if (constraints.hasBoundedWidth) maxWidth else 26.dp,
            if (constraints.hasBoundedHeight) maxHeight else 26.dp,
        )
        InfiniteProgressIndicator(color = color, size = diameter, strokeWidth = strokeWidth)
    }
}

@Composable
fun CircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp,
    trackColor: Color = Color.Transparent,
) = CouiCircularProgressIndicator(
    modifier = modifier,
    progress = progress(),
    strokeWidth = strokeWidth,
    colors = CouiProgressDefaults.circularProgressIndicatorColors(),
)

@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) = CouiLinearProgressIndicator(modifier = modifier)

@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) = CouiLinearProgressIndicator(modifier = modifier, progress = progress())
