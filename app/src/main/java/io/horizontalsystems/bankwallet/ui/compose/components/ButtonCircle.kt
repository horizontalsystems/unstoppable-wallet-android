package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun ButtonPrimaryCircle(
    @DrawableRes icon: Int = R.drawable.ic_arrow_down_left_24,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val shape = CircleShape
    HsIconButton(
        onClick = { onClick() },
        modifier = Modifier
            .size(50.dp)
            .clip(shape)
            .background(if (enabled) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.steel20),
        enabled = enabled,
        rippleColor = ComposeAppTheme.colors.claude
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = if (enabled) ComposeAppTheme.colors.claude else ComposeAppTheme.colors.grey50
        )
    }
}

@Composable
fun ButtonSecondaryCircle(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.ic_arrow_down_20,
    onClick: () -> Unit,
    tint: Color = ComposeAppTheme.colors.leah,
) {
    HsIconButton(
        onClick = onClick,
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(ComposeAppTheme.colors.steel20),
        rippleColor = tint
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = tint
        )
    }
}
