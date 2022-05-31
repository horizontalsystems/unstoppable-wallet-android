package io.horizontalsystems.bankwallet.modules.walletconnect.request.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.AmountValues
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ValueType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.Ellipsis
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.PlatformCoin

@Composable
fun TitleHexValueCell(title: String, valueVisible: String, value: String) {
    val localView = LocalView.current
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2
        )
        Spacer(Modifier.weight(1f))
        ButtonSecondaryDefault(
            modifier = Modifier.padding(start = 8.dp),
            title = valueVisible,
            onClick = {
                TextHelper.copyText(value)
                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
            },
            ellipsis = Ellipsis.Middle(10)
        )
    }
}

@Composable
fun AmountCell(
    fiatAmount: String?,
    coinAmount: String,
    type: ValueType,
    platformCoin: PlatformCoin
) {
    val coinAmountColor = when (type) {
        ValueType.Regular -> ComposeAppTheme.colors.bran
        ValueType.Disabled -> ComposeAppTheme.colors.grey
        ValueType.Outgoing -> ComposeAppTheme.colors.leah
        ValueType.Incoming -> ComposeAppTheme.colors.remus
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinImage(
            iconUrl = platformCoin.coin.iconUrl,
            placeholder = platformCoin.coinType.iconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
        )
        Text(
            text = coinAmount,
            color = coinAmountColor,
            style = ComposeAppTheme.typography.subhead1
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = fiatAmount ?: "",
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2
        )
    }
}

@Composable
fun AmountMultiCell(amounts: List<AmountValues>, type: ValueType, platformCoin: PlatformCoin) {
    val coinAmountColor = when (type) {
        ValueType.Regular -> ComposeAppTheme.colors.bran
        ValueType.Disabled -> ComposeAppTheme.colors.grey
        ValueType.Outgoing -> ComposeAppTheme.colors.leah
        ValueType.Incoming -> ComposeAppTheme.colors.remus
    }
    val height = if (amounts.size == 2) 60.dp else 48.dp
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinImage(
            iconUrl = platformCoin.coin.iconUrl,
            placeholder = platformCoin.coinType.iconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
        )
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                Text(
                    text = amounts[0].coinAmount,
                    color = coinAmountColor,
                    style = ComposeAppTheme.typography.subhead2
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = amounts[0].fiatAmount ?: "",
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead1
                )
            }
            if (amounts.size > 1) {
                Spacer(Modifier.height(3.dp))
                Row {
                    Text(
                        text = amounts[1].coinAmount,
                        color = ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.caption
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = amounts[1].fiatAmount ?: "",
                        color = ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.caption
                    )
                }
            }
        }
    }
}

@Composable
fun SubheadCell(title: String, value: String, iconRes: Int?) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        iconRes?.let { icon ->
            Icon(
                modifier = Modifier.padding(end = 16.dp),
                painter = painterResource(id = icon),
                tint = ComposeAppTheme.colors.grey,
                contentDescription = null,
            )
        }
        Text(
            text = title,
            color = ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.body
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead1
        )
    }
}

@Composable
fun TitleTypedValueCell(title: String, value: String, type: ValueType = ValueType.Regular) {
    val valueColor = when (type) {
        ValueType.Regular -> ComposeAppTheme.colors.bran
        ValueType.Disabled -> ComposeAppTheme.colors.grey
        ValueType.Outgoing -> ComposeAppTheme.colors.jacob
        ValueType.Incoming -> ComposeAppTheme.colors.remus
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(end = 36.dp),
            text = title,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            color = valueColor,
            style = ComposeAppTheme.typography.subhead1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
