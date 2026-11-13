package io.horizontalsystems.designkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horizontalsystems.designkit.theme.ComposeAppTheme

/**
 * The amount shown at the top of a balance: a large primary amount over a
 * secondary line.
 *
 * The primary amount never wraps. When it is wider than the component, its
 * font shrinks from the Title size until it fits, and it is cut with an
 * ellipsis only when it is still too wide at half that size. The line keeps
 * the height of the Title size while the font shrinks, so the content below
 * does not move when the amount changes length.
 */
@Composable
fun HSAmountText(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    titleColor: Color = ComposeAppTheme.colors.leah,
    subtitleColor: Color = ComposeAppTheme.colors.grey,
    onClickTitle: () -> Unit,
    onClickSubtitle: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val titleStyle = ComposeAppTheme.typography.title
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val titleHeight = remember(titleStyle, density) {
        with(density) { textMeasurer.measure("0", titleStyle).size.height.toDp() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.tyler)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = titleHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClickTitle
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicText(
                text = title,
                style = titleStyle,
                color = { titleColor },
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = titleStyle.fontSize * MinTitleScale,
                    maxFontSize = titleStyle.fontSize,
                    stepSize = 1.sp,
                ),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClickSubtitle
                    ),
                    text = subtitle,
                    style = ComposeAppTheme.typography.body,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailingContent?.invoke()
        }
    }
}

private const val MinTitleScale = 0.5f

@Preview
@Composable
private fun Preview_HSAmountText() {
    ComponentPreview {
        HSAmountText(
            title = "$1,289.60",
            subtitle = "≈22.6057 BTC",
            onClickTitle = {},
            onClickSubtitle = {},
        )
        HSAmountText(
            title = "1,289,231,289,231.60458921 USDT",
            subtitle = "≈$1,289,231,289,231.60",
            onClickTitle = {},
            onClickSubtitle = {},
        )
        HSAmountText(
            modifier = Modifier.width(200.dp),
            title = "1,289,231,289,231.60458921 USDT",
            subtitle = "≈$1,289,231,289,231.60",
            onClickTitle = {},
            onClickSubtitle = {},
        )
    }
}
