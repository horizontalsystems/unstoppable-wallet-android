package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

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
        buttonColors = ButtonPrimaryDefaults.textButtonColors(
            backgroundColor = ComposeAppTheme.colors.leah,
            contentColor = ComposeAppTheme.colors.claude,
            disabledBackgroundColor = ComposeAppTheme.colors.leah,
            disabledContentColor = ComposeAppTheme.colors.claude,
        ),
        content = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        enabled = enabled
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ButtonPrimaryTransparent(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val contentColor = when {
        !enabled -> ComposeAppTheme.colors.steel20
        isPressed -> ComposeAppTheme.colors.grey
        else -> ComposeAppTheme.colors.leah
    }

    Surface(
        modifier = modifier,
        color = ComposeAppTheme.colors.transparent,
        contentColor = contentColor,
        onClick = onClick,
        enabled = enabled,
        role = Role.Button,
        interactionSource = interactionSource,
        indication = null
    ) {
        ProvideTextStyle(
            value = ComposeAppTheme.typography.headline2
        ) {
            Row(
                Modifier
                    .defaultMinSize(
                        minWidth = ButtonPrimaryDefaults.MinWidth,
                        minHeight = ButtonPrimaryDefaults.MinHeight
                    )
                    .padding(ButtonPrimaryDefaults.ContentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
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
        buttonColors = ButtonPrimaryDefaults.textButtonColors(
            backgroundColor = ComposeAppTheme.colors.yellowD,
            contentColor = ComposeAppTheme.colors.dark,
            disabledBackgroundColor = ComposeAppTheme.colors.yellowD,
            disabledContentColor = ComposeAppTheme.colors.dark,
        ),
        content = {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
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
        buttonColors = ButtonPrimaryDefaults.textButtonColors(
            backgroundColor = ComposeAppTheme.colors.redD,
            contentColor = ComposeAppTheme.colors.claude,
            disabledBackgroundColor = ComposeAppTheme.colors.redD,
            disabledContentColor = ComposeAppTheme.colors.claude,
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
        buttonColors = ButtonPrimaryDefaults.textButtonColors(
            backgroundColor = ComposeAppTheme.colors.yellowD,
            contentColor = ComposeAppTheme.colors.dark,
            disabledBackgroundColor = ComposeAppTheme.colors.yellowD,
            disabledContentColor = ComposeAppTheme.colors.dark,
        ),
        content = {
            if (showSpinner) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = ComposeAppTheme.colors.grey,
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
    buttonColors: ButtonColors,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(25.dp),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonPrimaryDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {

    Surface(
        modifier = modifier,
        shape = shape,
        color = buttonColors.backgroundColor(enabled).value,
        contentColor = buttonColors.contentColor(enabled).value,
        border = border,
        onClick = onClick,
        enabled = enabled,
        role = Role.Button,
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple()
    ) {
        ProvideTextStyle(
            value = ComposeAppTheme.typography.headline2
        ) {
            Row(
                Modifier
                    .defaultMinSize(
                        minWidth = ButtonPrimaryDefaults.MinWidth,
                        minHeight = ButtonPrimaryDefaults.MinHeight
                    )
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

object ButtonPrimaryDefaults {
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
        backgroundColor: Color,
        contentColor: Color,
        disabledBackgroundColor: Color,
        disabledContentColor: Color,
    ): ButtonColors = HsButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor,
    )
}
