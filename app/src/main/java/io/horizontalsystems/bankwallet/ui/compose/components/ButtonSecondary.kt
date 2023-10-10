package io.horizontalsystems.bankwallet.ui.compose.components

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
import androidx.compose.material.Icon
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
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.bankwallet.ui.compose.components.SecondaryButtonDefaults.buttonColors

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
                subhead1_leah(text = title, maxLines = 1)
            } else {
                subhead1_grey50(text = title, maxLines = 1)
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
            end = 12.dp,
        ),
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                subhead1_leah(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    modifier = Modifier.padding(start = 4.dp),
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
                Row {
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        modifier = Modifier.padding(start = 4.dp),
                        painter = painterResource(id = iconRight),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                }
            } else {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                Text(select.selected.title.getString(), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            value = ComposeAppTheme.typography.subhead1
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
