package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun TabButtonSecondary(
    title: String,
    onSelect: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true
) {
    TabBox(
        colors = TabDefaults.textButtonColors(
            backgroundColor = ComposeAppTheme.colors.steel20,
        ),
        content = { Text(title) },
        selected = selected,
        enabled = enabled,
        onSelect = onSelect
    )
}

@Composable
fun TabButtonSecondaryTransparent(
    title: String,
    onSelect: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true
) {
    TabBox(
        colors = TabDefaults.textButtonColors(),
        content = { Text(title) },
        selected = selected,
        enabled = enabled,
        onSelect = onSelect
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TabBox(
    selected: Boolean = false,
    shape: Shape = RoundedCornerShape(14.dp),
    enabled: Boolean = true,
    colors: DefaultTabColors = TabDefaults.textButtonColors(),
    contentPadding: PaddingValues = TabDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
    onSelect: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
    ) {
        val contentColor by if (enabled) colors.contentColor(selected) else colors.contentColorDisabled()
        val backgroundColor by if (enabled) colors.backgroundColor(selected) else colors.backgroundColor(false)
        Surface(
            onClick = onSelect,
            color = backgroundColor,
            shape = shape,
            contentColor = contentColor,
            enabled = enabled
        ) {
            // Text is a predefined composable that does exactly what you'd expect it to -
            // display text on the screen. It allows you to customize its appearance using the
            // style property.
            CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
                ProvideTextStyle(
                    value = ComposeAppTheme.typography.subhead1
                ) {
                    Row(
                        Modifier
                            .defaultMinSize(
                                minWidth = TabDefaults.MinWidth,
                                minHeight = TabDefaults.MinHeight
                            )
                            .height(TabDefaults.MinHeight)
                            .padding(contentPadding),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        content = content
                    )
                }
            }
        }
    }
}

object TabDefaults {
    private val ButtonHorizontalPadding = 16.dp

    val ContentPadding = PaddingValues(
        start = ButtonHorizontalPadding,
        end = ButtonHorizontalPadding,
    )


    /**
     * The default min width applied for the [Button].
     * Note that you can override it by applying Modifier.widthIn directly on [Button].
     */
    val MinWidth = 50.dp

    /**
     * The default min width applied for the [Button].
     * Note that you can override it by applying Modifier.heightIn directly on [Button].
     */
    val MinHeight = 28.dp

    @Composable
    fun textButtonColors(
        backgroundColor: Color = ComposeAppTheme.colors.transparent,
        contentColor: Color = ComposeAppTheme.colors.oz,
        selectedBackgroundColor: Color = ComposeAppTheme.colors.yellowD,
        selectedContentColor: Color = ComposeAppTheme.colors.dark,
        disabledContentColor: Color = ComposeAppTheme.colors.grey50,
    ): DefaultTabColors = DefaultTabColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        selectedBackgroundColor = selectedBackgroundColor,
        selectedContentColor = selectedContentColor,
        disabledContentColor = disabledContentColor
    )
}

@Immutable
class DefaultTabColors(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val selectedBackgroundColor: Color,
    private val selectedContentColor: Color,
    private val disabledContentColor: Color
) {
    @Composable
    fun backgroundColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) selectedBackgroundColor else backgroundColor)
    }

    @Composable
    fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) selectedContentColor else contentColor)
    }

    @Composable
    fun contentColorDisabled(): State<Color> {
        return rememberUpdatedState(disabledContentColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultTabColors

        if (backgroundColor != other.backgroundColor) return false
        if (contentColor != other.contentColor) return false
        if (selectedBackgroundColor != other.selectedBackgroundColor) return false
        if (selectedContentColor != other.selectedContentColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + selectedBackgroundColor.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        return result
    }
}
