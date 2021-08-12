package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.UnstoppableComponentsAppTheme
import io.horizontalsystems.bankwallet.R

@Composable
fun UwCircleButton(
    @DrawableRes icon: Int = R.drawable.ic_arrow_down_left_24,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    IconButton(
        onClick = { onClick() },
        modifier = Modifier
            .size(50.dp)
            .clip(shape)
            .background(UnstoppableComponentsAppTheme.colors.leah)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = UnstoppableComponentsAppTheme.colors.claude
        )
    }
}
