package com.android.git.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.git.R
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

val LocalPageHazeState = staticCompositionLocalOf<HazeState?> { null }

/** All Git pages share one fixed, centered, edge-to-edge toolbar. */
@Composable
fun GitScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String = "",
    backEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val localHazeState = rememberHazeState()
    val hazeState = LocalPageHazeState.current ?: localHazeState
    CompositionLocalProvider(LocalPageHazeState provides hazeState) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenteredPageBar(title, subtitle, onBack, backEnabled, hazeState, actions)
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
private fun CenteredPageBar(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?,
    backEnabled: Boolean,
    hazeState: HazeState,
    actions: @Composable RowScope.() -> Unit
) {
    val page = MaterialTheme.colorScheme.background
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val fontScale = LocalDensity.current.fontScale
    val toolbarHeight = ((if (subtitle.isBlank()) 60f else 72f) * maxOf(1f, fontScale / 1.25f)).dp
    val blurStyle = remember {
        HazeBlurStyle {
            blurRadius(32.dp)
            noiseFactor(0f)
            progressive(HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f))
            mask(Brush.verticalGradient(0f to Color.Black, 0.72f to Color.Black, 1f to Color.Transparent))
        }
    }
    val veil = remember(page, supportsBlur) {
        Brush.verticalGradient(
            0f to page.copy(alpha = if (supportsBlur) 0.84f else 1f),
            0.70f to page.copy(alpha = if (supportsBlur) 0.62f else 0.96f),
            1f to page.copy(alpha = 0f)
        )
    }
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier.matchParentSize()
                .then(if (supportsBlur) Modifier.hazeBlur(HazeInput.Sources(hazeState), style = blurStyle) else Modifier)
                .background(veil)
        )
        // The blur layer is a sibling, so neither the title nor toolbar icons are blurred.
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Layout(
                modifier = Modifier.fillMaxWidth()
                    .height(toolbarHeight)
                    .padding(horizontal = 8.dp),
                content = {
                    Box(contentAlignment = Alignment.Center) {
                        if (onBack != null) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f)),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    enabled = backEnabled,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        GitIcons.Default.ArrowBack,
                                        contentDescription = stringResource(R.string.action_back),
                                        modifier = Modifier.size(24.dp).graphicsLayer { scaleX = if (isRtl) -1f else 1f },
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            title, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle.isNotBlank()) Text(
                            subtitle, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, content = actions)
                }
            ) { measurables, constraints ->
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                val gap = 6.dp.roundToPx()
                val sideConstraints = Constraints(maxWidth = (width * 0.42f).toInt(), maxHeight = height)
                val start = measurables[0].measure(sideConstraints)
                val end = measurables[2].measure(sideConstraints)
                val reserve = maxOf(start.width, end.width) + gap
                val center = measurables[1].measure(
                    Constraints(maxWidth = (width - reserve * 2).coerceAtLeast(0), maxHeight = height)
                )
                layout(width, height) {
                    start.placeRelative(0, (height - start.height) / 2)
                    center.placeRelative((width - center.width) / 2, (height - center.height) / 2)
                    end.placeRelative(width - end.width, (height - end.height) / 2)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier = modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth(), content = content)
}
