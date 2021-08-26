package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Steel20

@Composable
fun ButtonPrimaryCircle(
    @DrawableRes icon: Int = R.drawable.ic_arrow_down_left_24,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    IconButton(
        onClick = { onClick() },
        modifier = Modifier
            .size(50.dp)
            .clip(shape)
            .background(ComposeAppTheme.colors.leah)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = ComposeAppTheme.colors.claude
        )
    }
}

@Composable
fun ButtonSecondaryCircle(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.ic_arrow_down_20,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Box(modifier = modifier) {
        IconButton(
            onClick = { onClick() },
            modifier = Modifier
                .size(28.dp)
                .clip(shape)
                .background(Steel20)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = ComposeAppTheme.colors.leah
            )
        }
    }
}
