package io.horizontalsystems.bankwallet.modules.walletconnect.session.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.Status
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

@Composable
fun BlockchainCell(
    title: String,
    value: String,
) {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        subhead2_grey(text = title)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        subhead1_leah(text = value)
    }
}

@Composable
fun TitleValueCell(title: String, value: String) {
    RowUniversal(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        subhead2_grey(text = title)
        Spacer(Modifier.weight(1f))
        subhead1_leah(text = value)
    }
}

@Composable
fun StatusCell(connectionStatus: Status?) {
    RowUniversal(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        subhead2_grey(text = stringResource(id = R.string.WalletConnect_Status))
        Spacer(Modifier.weight(1f))
        connectionStatus?.let { status ->
            val color = when (status) {
                Status.OFFLINE -> ComposeAppTheme.colors.lucian
                Status.CONNECTING -> ComposeAppTheme.colors.leah
                Status.ONLINE -> ComposeAppTheme.colors.remus
            }
            Text(
                text = stringResource(status.value),
                color = color,
                style = ComposeAppTheme.typography.subhead1
            )
        }
    }
}

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
        Row {
            body_leah(
                text = value,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            if (enabled) {
                Icon(
                    modifier = Modifier
                        .padding(start = 4.dp, top = 2.dp)
                        .align(Alignment.CenterVertically),
                    painter = painterResource(id = R.drawable.ic_down_arrow_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}
