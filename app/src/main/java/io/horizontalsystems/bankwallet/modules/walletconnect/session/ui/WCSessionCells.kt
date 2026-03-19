package io.horizontalsystems.bankwallet.modules.walletconnect.session.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.paidAction
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.modules.nav3.NavController
import io.horizontalsystems.bankwallet.modules.walletconnect.session.Status
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCWhiteListState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

@Composable
fun DropDownCell(
    title: String,
    value: String,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    RowUniversal(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clickable(enabled = enabled, onClick = onSelect),
        onClick = if (enabled) onSelect else null
    ) {
        subhead2_grey(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            subhead1_leah(
                text = value,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            if (enabled) {
                HSpacer(8.dp)
                Icon(
                    painter = painterResource(id = R.drawable.ic_down_arrow_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}
