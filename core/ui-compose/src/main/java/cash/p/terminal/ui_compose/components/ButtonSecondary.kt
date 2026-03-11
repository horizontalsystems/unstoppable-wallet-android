package cash.p.terminal.ui_compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Icon
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.strings.helpers.WithTranslatableTitle
import cash.p.terminal.ui_compose.R
import cash.p.terminal.ui_compose.Select
import cash.p.terminal.ui_compose.components.SecondaryButtonDefaults.buttonColors
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun ButtonSecondaryDefault(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    ButtonSecondary(
        modifier = modifier,
        onClick = onClick,
        content = {
            if (enabled) {
                captionSB_leah(text = title, maxLines = 1)
            } else {
                captionSB_grey50(text = title, maxLines = 1)
            }
        },
        enabled = enabled
    )
}

@Composable
fun ButtonSecondaryCustom(
    modifier: Modifier = Modifier,
    title: String,
    textColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    ButtonSecondary(
        modifier = modifier,
        onClick = onClick,
        content = {
            if (enabled) {
                E2(
                    text = title,
                    maxLines = 1,
                    color = textColor
                )
            } else {
                captionSB_grey50(text = title, maxLines = 1)
            }
        },
        enabled = enabled
    )
}

@Composable
fun ButtonSecondaryYellow(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    ButtonSecondary(
        modifier = modifier,
        onClick = onClick,
        buttonColors = ButtonPrimaryDefaults.textButtonColors(
            backgroundColor = ComposeAppTheme.colors.yellowD,
            contentColor = ComposeAppTheme.colors.dark,
            disabledBackgroundColor = ComposeAppTheme.colors.steel20,
            disabledContentColor = ComposeAppTheme.colors.grey50,
        ),
        content = {
            Text(
                title,
                maxLines = 1,
                style = ComposeAppTheme.typography.captionSB,
                overflow = TextOverflow.Ellipsis,
            )
        },
        enabled = enabled
    )
}

@Composable
fun ButtonSecondaryWithIcon(
    modifier: Modifier = Modifier,
    title: String,
    iconRight: Painter,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    ButtonSecondary(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 8.dp,
        ),
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                captionSB_leah(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    modifier = Modifier.padding(start = 2.dp),
                    painter = iconRight,
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        },
        enabled = enabled
    )
}

@Composable
fun ButtonSecondaryTransparent(
    modifier: Modifier = Modifier,
    title: String,
    @DrawableRes iconRight: Int? = null,
    onClick: () -> Unit,
    enabled: Boolean = true
) {

    ButtonSecondary(
        modifier = modifier,
        onClick = onClick,
        buttonColors = buttonColors(
            backgroundColor = ComposeAppTheme.colors.transparent,
            contentColor = ComposeAppTheme.colors.leah,
            disabledBackgroundColor = ComposeAppTheme.colors.transparent,
            disabledContentColor = ComposeAppTheme.colors.grey50,
        ),
        content = {
            if (iconRight != null) {
                Row (
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = title,
                        maxLines = 1,
                        style = ComposeAppTheme.typography.captionSB,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        modifier = Modifier.padding(start = 4.dp),
                        painter = painterResource(id = iconRight),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                }
            } else {
                Text(
                    text = title,
                    maxLines = 1,
                    style = ComposeAppTheme.typography.captionSB,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        enabled = enabled
    )
}

@Composable
fun <T : WithTranslatableTitle> ButtonSecondaryToggle(
    modifier: Modifier = Modifier,
    select: Select<T>,
    onSelect: (T) -> Unit,
    enabled: Boolean = true
) {
    ButtonSecondary(
        modifier = modifier,
        onClick = {
            val options = select.options
            val selectedItemIndex = options.indexOf(select.selected)
            val nextSelectedItemIndex = if (selectedItemIndex == options.size - 1) 0 else selectedItemIndex + 1

            onSelect(options[nextSelectedItemIndex])
        },
        content = {
            Row(
                modifier = Modifier.height(IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = select.selected.title.getString(),
                    maxLines = 1,
                    style = ComposeAppTheme.typography.captionSB,
                    overflow = TextOverflow.Ellipsis
                )
                Column(
                    modifier = Modifier
                        .height(16.dp)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    select.options.forEach {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(if (select.selected == it) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey)
                        )
                    }
                }
            }
        },
        enabled = enabled
    )
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ButtonSecondary(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    border: BorderStroke? = null,
    buttonColors: ButtonColors = buttonColors(),
    contentPadding: PaddingValues = SecondaryButtonDefaults.ContentPadding,
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
    ) {
        ProvideTextStyle(
            value = ComposeAppTheme.typography.captionSB
        ) {
            Row(
                Modifier
                    .defaultMinSize(
                        minWidth = SecondaryButtonDefaults.MinWidth,
                        minHeight = SecondaryButtonDefaults.MinHeight
                    )
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

object SecondaryButtonDefaults {
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
    fun buttonColors(
        backgroundColor: Color = ComposeAppTheme.colors.steel20,
        contentColor: Color = ComposeAppTheme.colors.leah,
        disabledBackgroundColor: Color = ComposeAppTheme.colors.steel20,
        disabledContentColor: Color = ComposeAppTheme.colors.grey50,
    ): ButtonColors = HsButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor,
    )
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun ButtonSecondaryDefaultPreview() {
    ComposeAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ButtonSecondaryDefault(
                title = "Secondary Default",
                onClick = {}
            )
            ButtonSecondaryDefault(
                title = "Secondary Disabled",
                onClick = {},
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun ButtonSecondaryCustomPreview() {
    ComposeAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ButtonSecondaryCustom(
                title = "Custom Color",
                textColor = ComposeAppTheme.colors.jacob,
                onClick = {}
            )
            ButtonSecondaryCustom(
                title = "Custom Red",
                textColor = ComposeAppTheme.colors.lucian,
                onClick = {}
            )
            ButtonSecondaryCustom(
                title = "Custom Disabled",
                textColor = ComposeAppTheme.colors.jacob,
                onClick = {},
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun ButtonSecondaryYellowPreview() {
    ComposeAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ButtonSecondaryYellow(
                title = "Yellow Button",
                onClick = {}
            )
            ButtonSecondaryYellow(
                title = "Yellow Disabled",
                onClick = {},
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun ButtonSecondaryWithIconPreview() {
    ComposeAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ButtonSecondaryWithIcon(
                title = "With Icon",
                iconRight = painterResource(R.drawable.ic_back),
                onClick = {}
            )
            ButtonSecondaryWithIcon(
                title = "With Icon Disabled",
                iconRight = painterResource(R.drawable.ic_back),
                onClick = {},
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun ButtonSecondaryTransparentPreview() {
    ComposeAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ButtonSecondaryTransparent(
                title = "Transparent",
                onClick = {}
            )
            ButtonSecondaryTransparent(
                title = "With Icon",
                iconRight = R.drawable.ic_back,
                onClick = {}
            )
            ButtonSecondaryTransparent(
                title = "Transparent Disabled",
                onClick = {},
                enabled = false
            )
        }
    }
}

private enum class PreviewToggleOption : WithTranslatableTitle {
    Option1, Option2, Option3;

    override val title: TranslatableString
        get() = TranslatableString.PlainString(name)
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun ButtonSecondaryTogglePreview() {
    ComposeAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ButtonSecondaryToggle(
                select = Select(
                    selected = PreviewToggleOption.Option1,
                    options = PreviewToggleOption.entries
                ),
                onSelect = {}
            )
            ButtonSecondaryToggle(
                select = Select(
                    selected = PreviewToggleOption.Option2,
                    options = PreviewToggleOption.entries
                ),
                onSelect = {}
            )
            ButtonSecondaryToggle(
                select = Select(
                    selected = PreviewToggleOption.Option1,
                    options = PreviewToggleOption.entries
                ),
                onSelect = {},
                enabled = false
            )
        }
    }
}

// endregion
