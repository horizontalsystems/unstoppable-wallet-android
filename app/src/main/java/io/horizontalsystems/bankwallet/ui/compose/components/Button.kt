package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.*
import io.horizontalsystems.bankwallet.ui.compose.Grey50

@Composable
fun ButtonPrimaryDefault(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    ButtonPrimary(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = ComposeAppTheme.colors.leah,
        ),
        content = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        enabled = enabled
    )
}

@Composable
fun ButtonPrimaryYellow(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    ButtonPrimary(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = YellowD,
        ),
        content = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        enabled = enabled
    )
}

@Composable
fun ButtonPrimaryRed(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    ButtonPrimary(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = RedD,
        ),
        content = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        enabled = enabled
    )
}

@Composable
fun ButtonPrimaryYellowWithSpinner(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    showSpinner: Boolean = false,
    enabled: Boolean = true
) {

    ButtonPrimary(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = YellowD,
        ),
        content = {
            if (showSpinner) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Grey,
                    strokeWidth = 2.dp
                )
            } else {
                Text(title)
            }
        },
        enabled = enabled
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ButtonPrimary(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(25.dp),
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val contentColor by colors.contentColor(enabled)
    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.backgroundColor(enabled).value,
        contentColor = contentColor.copy(alpha = 1f),
        border = border,
        elevation = 0.dp,
        onClick = onClick,
        enabled = enabled,
        role = Role.Button,
        interactionSource = interactionSource,
        indication = rememberRipple()
    ) {
        CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
            ProvideTextStyle(
                value = ComposeAppTheme.typography.headline2
            ) {
                Row(
                    Modifier
                        .defaultMinSize(
                            minWidth = ButtonDefaults.MinWidth,
                            minHeight = ButtonDefaults.MinHeight
                        )
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

object ButtonDefaults {
    private val ButtonHorizontalPadding = 16.dp

    val ContentPadding = PaddingValues(
        start = ButtonHorizontalPadding,
        end = ButtonHorizontalPadding
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
    val MinHeight = 50.dp

    @Composable
    fun textButtonColors(
        backgroundColor: Color = Color.Transparent,
        contentColor: Color = ComposeAppTheme.colors.claude,
        disabledBackgroundColor: Color = Steel20,
        disabledContentColor: Color = Grey50,
    ): ButtonColors = DefaultButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor
    )
}

@Immutable
private class DefaultButtonColors(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val disabledBackgroundColor: Color,
    private val disabledContentColor: Color
) : ButtonColors {
    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) backgroundColor else disabledBackgroundColor)
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) contentColor else disabledContentColor)
    }

    override fun equals(other: Any?): Boolean {

        if (other !is DefaultButtonColors) return false

        return backgroundColor != other.backgroundColor
                && contentColor != other.contentColor
                && disabledBackgroundColor != other.disabledBackgroundColor
                && disabledContentColor != other.disabledContentColor
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledBackgroundColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        return result
    }
}