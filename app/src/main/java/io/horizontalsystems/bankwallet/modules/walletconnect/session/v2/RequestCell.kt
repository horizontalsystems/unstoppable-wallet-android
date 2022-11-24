package io.horizontalsystems.bankwallet.modules.walletconnect.requestlist.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2RequestViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun RequestCell(
    viewItem: WC2RequestViewItem,
    onRequestClick: (WC2RequestViewItem) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(
                onClick = {
                    onRequestClick.invoke(viewItem)
                },
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
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
