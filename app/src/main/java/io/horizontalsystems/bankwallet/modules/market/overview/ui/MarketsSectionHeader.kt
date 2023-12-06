package io.horizontalsystems.bankwallet.modules.market.overview.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah

@Composable
fun MarketsSectionHeader(
    title: Int,
    icon: Painter,
    onClick: (() -> Unit) = {},
    endSlot: @Composable() (() -> Unit)? = null,
) {
    Box {
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )
        Row(
            modifier = Modifier
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clickable(
                        onClick = onClick,
                        interactionSource = MutableInteractionSource(),
                        indication = null
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = icon,
                    contentDescription = "Section Header Icon"
                )
                HSpacer(width = 16.dp)
                body_leah(
                    text = stringResource(title),
                    maxLines = 1,
                )
            }
            HFillSpacer(minWidth = 16.dp)
            endSlot?.invoke()
        }
    }
}