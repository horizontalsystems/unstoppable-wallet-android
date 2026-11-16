package io.horizontalsystems.walletkit.modules.manageaccount.showextendedkey

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.components.ButtonStyle
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSDropdownButton
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HsIconButton
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.body_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_grey

@Composable
fun MenuItem(
    title: String,
    value: String,
    testTag: String? = null,
    infoButtonClick: (() -> Unit)? = null,
    onClick: (() -> Unit)?
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        body_leah(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        infoButtonClick?.let { click ->
            HSpacer(width = 8.dp)
            HsIconButton(
                modifier = Modifier.size(20.dp),
                onClick = click
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info_20),
                    contentDescription = "info button",
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (onClick != null) {
            // A value that can be changed opens its list of options
            HSDropdownButton(
                modifier = testTag?.let { Modifier.testTag(it) } ?: Modifier,
                variant = ButtonVariant.Secondary,
                style = ButtonStyle.Transparent,
                title = value,
                onClick = onClick,
            )
        } else {
            subhead1_grey(
                text = value,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}
