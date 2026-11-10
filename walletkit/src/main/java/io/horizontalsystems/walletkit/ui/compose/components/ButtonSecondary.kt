package io.horizontalsystems.walletkit.ui.compose.components

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
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.Select
import io.horizontalsystems.walletkit.ui.compose.WithTranslatableTitle
import io.horizontalsystems.walletkit.ui.compose.components.SecondaryButtonDefaults.buttonColors

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
                                .background(if (select.selected == it) ComposeAppTheme.colors.orange else ComposeAppTheme.colors.grey)
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
        backgroundColor: Color = ComposeAppTheme.colors.blade,
        contentColor: Color = ComposeAppTheme.colors.leah,
        disabledBackgroundColor: Color = ComposeAppTheme.colors.blade,
        disabledContentColor: Color = ComposeAppTheme.colors.andy,
    ): ButtonColors = HsButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor,
    )
}
