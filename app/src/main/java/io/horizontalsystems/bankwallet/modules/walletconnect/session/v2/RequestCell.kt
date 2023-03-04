package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2RequestViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal

@Composable
fun RequestCell(
    viewItem: WC2RequestViewItem,
    onRequestClick: (WC2RequestViewItem) -> Unit,
) {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = { onRequestClick.invoke(viewItem) }
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = viewItem.title,
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.leah,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = viewItem.subtitle,
                style = ComposeAppTheme.typography.subhead2,
                color = ComposeAppTheme.colors.grey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Image(
            modifier = Modifier.padding(start = 5.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}
