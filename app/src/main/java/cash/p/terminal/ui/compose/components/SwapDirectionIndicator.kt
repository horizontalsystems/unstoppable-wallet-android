package cash.p.terminal.ui.compose.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Token

@Composable
fun SwapDirectionIndicator(
    modifier: Modifier = Modifier,
    intermediateToken: Token? = null,
    onClick: () -> Unit,
) {
    SwapDirectionIndicatorContent(
        modifier = modifier,
        expanded = intermediateToken != null,
        intermediateToken = intermediateToken,
        onClick = onClick,
    )
}

@Composable
private fun SwapDirectionIndicatorContent(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    intermediateToken: Token?,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .defaultMinSize(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ComposeAppTheme.colors.steel20)
            .clickable(
                onClick = onClick,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = false,
                    radius = 24.dp,
                    color = ComposeAppTheme.colors.leah
                )
            )
            .animateContentSize(animationSpec = tween(300)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (expanded) {
            HSpacer(width = 8.dp)
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_down_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.leah,
            modifier = Modifier.size(20.dp)
        )
        if (expanded) {
            HSpacer(width = 10.dp)
            subhead2_grey(text = stringResource(R.string.swap_through))
            HSpacer(width = 10.dp)
            CoinImage(
                token = intermediateToken,
                modifier = Modifier.size(24.dp)
            )
            HSpacer(width = 8.dp)
        }
    }
}

@Preview(name = "Circle (collapsed)")
@Composable
private fun SwapDirectionIndicatorCirclePreview() {
    ComposeAppTheme {
        SwapDirectionIndicatorContent(
            expanded = false,
            intermediateToken = null,
            onClick = {}
        )
    }
}

@Preview(name = "Pill (expanded)")
@Composable
private fun SwapDirectionIndicatorPillPreview() {
    ComposeAppTheme {
        SwapDirectionIndicatorContent(
            expanded = true,
            intermediateToken = null,
            onClick = {}
        )
    }
}

@Preview(name = "Interactive – tap to toggle")
@Composable
private fun SwapDirectionIndicatorInteractivePreview() {
    ComposeAppTheme {
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.width(200.dp), contentAlignment = Alignment.Center) {
            SwapDirectionIndicatorContent(
                expanded = expanded,
                intermediateToken = null,
                onClick = { expanded = !expanded }
            )
        }
    }
}
