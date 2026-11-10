package io.horizontalsystems.designkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import io.horizontalsystems.designkit.theme.ComposeAppTheme

internal const val PressedAlpha = 0.6f

internal class ButtonAppearance(
    val container: Color,
    val content: Color,
    val border: Color?,
)

/**
 * The colours shared by every button control. Secondary solid is the only
 * combination that depends on the size: the medium one is the inverted
 * surface, the small one sits on blade.
 */
@Composable
internal fun buttonAppearance(
    variant: ButtonVariant,
    style: ButtonStyle,
    size: ButtonSize,
    enabled: Boolean,
): ButtonAppearance {
    val colors = ComposeAppTheme.colors

    if (!enabled) {
        return when (style) {
            ButtonStyle.Solid -> ButtonAppearance(colors.blade, colors.andy, null)
            ButtonStyle.Transparent -> ButtonAppearance(Color.Transparent, colors.andy, colors.andy)
        }
    }

    return when (style) {
        ButtonStyle.Solid -> when (variant) {
            ButtonVariant.Primary -> ButtonAppearance(colors.orange, colors.white, null)
            ButtonVariant.Secondary -> when (size) {
                ButtonSize.Medium -> ButtonAppearance(colors.leah, colors.lawrence, null)
                ButtonSize.Small -> ButtonAppearance(colors.blade, colors.leah, null)
            }
            ButtonVariant.Destructive -> ButtonAppearance(colors.lucian, colors.lawrence, null)
        }

        ButtonStyle.Transparent -> when (variant) {
            ButtonVariant.Primary -> ButtonAppearance(Color.Transparent, colors.orange, colors.orange)
            ButtonVariant.Secondary -> ButtonAppearance(Color.Transparent, colors.leah, colors.andy)
            ButtonVariant.Destructive -> ButtonAppearance(Color.Transparent, colors.lucian, colors.lucian)
        }
    }
}

/**
 * The clickable shape under every button control. A press dims the whole
 * button instead of drawing a ripple.
 *
 * The content gets [startPadding] and [endPadding] when there is room. When
 * the button is given less width than that, the padding shrinks down to
 * [minHorizontalPadding] before the content is cut, so a long title in a
 * narrow button loses padding first and text last.
 */
@Composable
internal fun ButtonSurface(
    modifier: Modifier,
    shape: Shape,
    appearance: ButtonAppearance,
    enabled: Boolean,
    onClick: () -> Unit,
    startPadding: Dp = 0.dp,
    endPadding: Dp = startPadding,
    minHorizontalPadding: Dp = minOf(startPadding, endPadding),
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Layout(
        content = {
            CompositionLocalProvider(
                LocalContentColor provides appearance.content,
                LocalContentAlpha provides appearance.content.alpha,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        },
        modifier = modifier
            .graphicsLayer { alpha = if (pressed) PressedAlpha else 1f }
            .clip(shape)
            .background(appearance.container)
            .then(
                if (appearance.border != null) {
                    Modifier.border(1.dp, appearance.border, shape)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) { measurables, constraints ->
        val start = startPadding.roundToPx()
        val end = endPadding.roundToPx()
        val minPadding = minHorizontalPadding.roundToPx()

        val placeable = measurables.first().measure(
            constraints
                .copy(minWidth = 0, minHeight = 0)
                .offset(horizontal = -2 * minPadding)
        )

        val width = (placeable.width + start + end)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = placeable.height
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            val spare = width - placeable.width - start - end
            placeable.placeRelative(
                x = start + spare / 2,
                y = (height - placeable.height) / 2,
            )
        }
    }
}
