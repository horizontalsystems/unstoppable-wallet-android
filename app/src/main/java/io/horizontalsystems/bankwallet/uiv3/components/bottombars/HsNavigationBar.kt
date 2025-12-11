/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.horizontalsystems.bankwallet.uiv3.components.bottombars

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

@Composable
fun RowScope.HsNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    colors: HsNavigationBarItemColors = HsNavigationBarItemDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val styledIcon = @Composable {
        val iconColor by animateColorAsState(
            targetValue = colors.iconColor(selected = selected, enabled = enabled),
            animationSpec = TweenSpec(durationMillis = 150)
        )
        // If there's a label, don't have a11y services repeat the icon description.
        val clearSemantics = label != null && (alwaysShowLabel || selected)
        Box(modifier = if (clearSemantics) Modifier.clearAndSetSemantics {} else Modifier) {
            CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
        }
    }

    val styledLabel: @Composable (() -> Unit)? = label?.let {
        @Composable {
            val style = MaterialTheme.typography.labelSmall
            val textColor by animateColorAsState(
                targetValue = colors.textColor(selected = selected, enabled = enabled),
                animationSpec = TweenSpec(durationMillis = 150)
            )
            ProvideTextStyle(value = style, content = {
                CompositionLocalProvider(
                    LocalContentColor provides textColor,
                    content = label
                )
            })
        }
    }

    var itemWidth by remember { mutableIntStateOf(0) }

    val baseModifier = modifier
        .weight(1f)
        .onSizeChanged {
            itemWidth = it.width
        }

    val clickableModifier = if (onLongClick != null) {
        baseModifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
            onDoubleClick = onLongClick,
            enabled = enabled,
            role = Role.Tab,
            interactionSource = interactionSource,
            indication = null
        )
    } else {
        baseModifier.selectable(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            role = Role.Tab,
            interactionSource = interactionSource,
            indication = null,
        )
    }

    Box(
        modifier = clickableModifier,
        contentAlignment = Alignment.Center,
    ) {
        val animationProgress: State<Float> = animateFloatAsState(
            targetValue = if (selected) 1f else 0f,
            animationSpec = TweenSpec(durationMillis = 150)
        )

        // The entire item is selectable, but only the indicator pill shows the ripple. To achieve
        // this, we re-map the coordinates of the item's InteractionSource into the coordinates of
        // the indicator.
        val deltaOffset: Offset
        with(LocalDensity.current) {
            val indicatorWidth = 64.dp.roundToPx()
            deltaOffset = Offset((itemWidth - indicatorWidth).toFloat() / 2, 12.dp.toPx())
        }

        val offsetInteractionSource = remember(interactionSource, deltaOffset) {
            MappedInteractionSource(interactionSource, deltaOffset)
        }

        val indicatorRipple = @Composable {
            Box(
                Modifier
                    .layoutId(IndicatorRippleLayoutIdTag)
                    .clip(MaterialTheme.shapes.medium)
                    .indication(offsetInteractionSource, ripple())
            )
        }
        val indicator = @Composable {
            Box(
                Modifier
                    .layoutId(IndicatorLayoutIdTag)
                    .graphicsLayer { alpha = animationProgress.value }
                    .background(
                        color = colors.indicatorColor,
                        shape = MaterialTheme.shapes.medium,
                    )
            )
        }

        NavigationBarItemLayout(
            indicatorRipple = indicatorRipple,
            indicator = indicator,
            icon = styledIcon,
            label = styledLabel,
            alwaysShowLabel = alwaysShowLabel,
            animationProgress = { animationProgress.value },
        )
    }
}

object HsNavigationBarItemDefaults {
    @Composable
    fun colors(
        selectedIconColor: Color = MaterialTheme.colorScheme.primary,
        selectedTextColor: Color = MaterialTheme.colorScheme.onSurface,
        indicatorColor: Color = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledIconColor: Color = unselectedIconColor.copy(alpha = 0.38f),
        disabledTextColor: Color = unselectedTextColor.copy(alpha = 0.38f)
    ): HsNavigationBarItemColors =
        HsNavigationBarItemColors(
            selectedIconColor = selectedIconColor,
            selectedTextColor = selectedTextColor,
            selectedIndicatorColor = indicatorColor,
            unselectedIconColor = unselectedIconColor,
            unselectedTextColor = unselectedTextColor,
            disabledIconColor = disabledIconColor,
            disabledTextColor = disabledTextColor
        )
}

@Stable
class HsNavigationBarItemColors(
    val selectedIconColor: Color,
    val selectedTextColor: Color,
    val selectedIndicatorColor: Color,
    val unselectedIconColor: Color,
    val unselectedTextColor: Color,
    val disabledIconColor: Color,
    val disabledTextColor: Color,
) {
    @Stable
    internal fun iconColor(selected: Boolean, enabled: Boolean): Color =
        when {
            !enabled -> disabledIconColor
            selected -> selectedIconColor
            else -> unselectedIconColor
        }

    @Stable
    internal fun textColor(selected: Boolean, enabled: Boolean): Color =
        when {
            !enabled -> disabledTextColor
            selected -> selectedTextColor
            else -> unselectedTextColor
        }

    internal val indicatorColor: Color
        get() = selectedIndicatorColor

}

@Composable
private fun NavigationBarItemLayout(
    indicatorRipple: @Composable () -> Unit,
    indicator: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    alwaysShowLabel: Boolean,
    animationProgress: () -> Float,
) {
    Layout(
        content = {
            indicatorRipple()
            indicator()

            Box(Modifier.layoutId(IconLayoutIdTag)) { icon() }

            if (label != null) {
                Box(
                    Modifier
                        .layoutId(LabelLayoutIdTag)
                        .graphicsLayer {
                            alpha = if (alwaysShowLabel) 1f else animationProgress()
                        }
                ) {
                    label()
                }
            }
        },
    ) { measurables, constraints ->
        val animationProgressValue = animationProgress().coerceAtLeast(0f)
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val iconPlaceable =
            measurables.first { it.layoutId == IconLayoutIdTag }.measure(looseConstraints)

        val totalIndicatorWidth = (64.dp).roundToPx()
        val animatedIndicatorWidth = (totalIndicatorWidth * animationProgressValue).roundToInt()
        val indicatorHeight = (32.dp).roundToPx()
        val indicatorRipplePlaceable =
            measurables
                .first { it.layoutId == IndicatorRippleLayoutIdTag }
                .measure(Constraints.fixed(width = totalIndicatorWidth, height = indicatorHeight))
        val indicatorPlaceable =
            measurables
                .firstOrNull { it.layoutId == IndicatorLayoutIdTag }
                ?.measure(
                    Constraints.fixed(width = animatedIndicatorWidth, height = indicatorHeight)
                )

        val labelPlaceable =
            label?.let {
                measurables.first { it.layoutId == LabelLayoutIdTag }.measure(looseConstraints)
            }

        if (label == null) {
            placeIcon(iconPlaceable, indicatorRipplePlaceable, indicatorPlaceable, constraints)
        } else {
            placeLabelAndIcon(
                labelPlaceable!!,
                iconPlaceable,
                indicatorRipplePlaceable,
                indicatorPlaceable,
                constraints,
                alwaysShowLabel,
                animationProgressValue,
            )
        }
    }
}

private fun MeasureScope.placeIcon(
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable?,
    constraints: Constraints,
): MeasureResult {
    val width = constraints.maxWidth
    val height = constraints.maxHeight

    val iconX = (width - iconPlaceable.width) / 2
    val iconY = (height - iconPlaceable.height) / 2

    val rippleX = (width - indicatorRipplePlaceable.width) / 2
    val rippleY = (height - indicatorRipplePlaceable.height) / 2

    return layout(width, height) {
        indicatorPlaceable?.let {
            val indicatorX = (width - it.width) / 2
            val indicatorY = (height - it.height) / 2
            it.placeRelative(indicatorX, indicatorY)
        }
        iconPlaceable.placeRelative(iconX, iconY)
        indicatorRipplePlaceable.placeRelative(rippleX, rippleY)
    }
}

private fun MeasureScope.placeLabelAndIcon(
    labelPlaceable: Placeable,
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable?,
    constraints: Constraints,
    alwaysShowLabel: Boolean,
    animationProgress: Float,
): MeasureResult {
    val height = constraints.maxHeight

    val iconY = 12.dp.toPx()
    val labelY = 16.dp.toPx() + iconPlaceable.height

    val unselectedIconY = (height - iconPlaceable.height) / 2
    val iconDistance = unselectedIconY - iconY
    val offset = (iconDistance * (1 - animationProgress)).roundToInt()

    val containerWidth = constraints.maxWidth

    val labelX = (containerWidth - labelPlaceable.width) / 2
    val iconX = (containerWidth - iconPlaceable.width) / 2

    val rippleX = (containerWidth - indicatorRipplePlaceable.width) / 2
    val rippleY = 12.dp.toPx() - 4.dp.toPx()

    return layout(containerWidth, height) {
        indicatorPlaceable?.let {
            val indicatorX = (containerWidth - it.width) / 2
            val indicatorY = 12.dp.toPx() - 4.dp.toPx()
            it.placeRelative(indicatorX, indicatorY.roundToInt() + offset)
        }
        if (alwaysShowLabel || animationProgress != 0f) {
            labelPlaceable.placeRelative(labelX, labelY.roundToInt() + offset)
        }
        iconPlaceable.placeRelative(iconX, iconY.roundToInt() + offset)
        indicatorRipplePlaceable.placeRelative(rippleX, rippleY.roundToInt() + offset)
    }
}

private const val IndicatorRippleLayoutIdTag: String = "indicatorRipple"
private const val IndicatorLayoutIdTag: String = "indicator"
private const val IconLayoutIdTag: String = "icon"
private const val LabelLayoutIdTag: String = "label"

@Stable
private class MappedInteractionSource(
    private val underlying: InteractionSource,
    private val delta: Offset
) : InteractionSource {
    override val interactions: Flow<Interaction> = underlying.interactions.map { interaction ->
        if (interaction is PressInteraction.Press) {
            PressInteraction.Press(interaction.pressPosition - delta)
        } else {
            interaction
        }
    }
}
