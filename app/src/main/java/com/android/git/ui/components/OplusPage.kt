package com.android.git.ui.components

import android.os.Build
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontFamily
import com.android.git.R
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.TopAppBar
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.lyxnx.compose.ui.tablericons.TablerIcons
import io.github.lyxnx.compose.ui.tablericons.outline.ArrowLeft

val LocalOplusHazeState = staticCompositionLocalOf<HazeState?> { null }

/** One page owns its scrolling chrome, blur sample, and COUI popup host. */
@Composable
fun OplusScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String = "",
    backEnabled: Boolean = true,
    collapsible: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = COUIScrollBehavior(canScroll = { collapsible })
    val localHazeState = rememberHazeState()
    val hazeState = LocalOplusHazeState.current ?: localHazeState
    val blurStyle = remember { HazeBlurStyle { blurRadius(24.dp); noiseFactor(0f) } }
    val surface = COUITheme.colorScheme.surface
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val collapsed by remember { derivedStateOf { scrollBehavior.state.collapsedFraction >= 0.99f } }
    // ColorOS may resolve the app and Settings host's default font differently.
    // Resolve an explicit weighted system face for the title only. Subtitle and
    // preference fonts continue to use their own regular/medium COUI styles.
    val titleFamily = remember(collapsed) {
        FontFamily(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Typeface.create(Typeface.SANS_SERIF, if (collapsed) 500 else 600, false)
            } else Typeface.create("sans-serif", if (collapsed) Typeface.NORMAL else Typeface.BOLD)
        )
    }
    val baseTextStyles = COUITheme.textStyles
    val headerTextStyles = remember(baseTextStyles, titleFamily) {
        baseTextStyles.copy(main = baseTextStyles.main.copy(fontFamily = titleFamily))
    }
    CompositionLocalProvider(LocalOplusHazeState provides hazeState) {
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = surface,
            topBar = {
                COUITheme(textStyles = headerTextStyles) {
                TopAppBar(
                    title = title,
                    subtitle = subtitle,
                    hideSubtitleOnCollapse = false,
                    scrollBehavior = scrollBehavior,
                    color = surface.copy(alpha = surface.alpha * if (supportsBlur) 0.96f else 1f),
                    modifier = Modifier.clipToBounds().then(
                        if (supportsBlur) Modifier.hazeBlur(input = HazeInput.Sources(hazeState), style = blurStyle) else Modifier
                    ),
                    navigationIconPadding = 16.dp,
                    actionIconPadding = 24.dp,
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.padding(start = 8.dp),
                                enabled = backEnabled,
                                minWidth = 40.dp,
                                minHeight = 40.dp,
                                cornerRadius = 20.dp
                            ) {
                                Icon(
                                    TablerIcons.Outline.ArrowLeft,
                                    contentDescription = stringResource(R.string.action_back),
                                    modifier = Modifier.graphicsLayer { scaleX = if (isRtl) -1f else 1f }
                                )
                            }
                        }
                    },
                    actions = actions
                )
                }
            },
            floatingActionButton = floatingActionButton,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost
        ) { padding ->
            Box(Modifier.fillMaxSize().hazeSource(hazeState)) { content(padding) }
        }
    }
}

@Composable
fun OplusSectionTitle(title: String, modifier: Modifier = Modifier) {
    // Pages already supply the 12dp side margin; category text starts at 32dp.
    SmallTitle(text = title, modifier = modifier, insideMargin = PaddingValues(horizontal = 20.dp, vertical = 8.dp))
}

@Composable
fun OplusCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth(), content = content)
}
