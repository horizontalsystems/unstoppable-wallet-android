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
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token

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
        subhead2_grey(text = title)
        Spacer(Modifier.weight(1f))
        ButtonSecondaryDefault(
            modifier = Modifier.padding(start = 8.dp),
            title = valueVisible,
            onClick = {
                TextHelper.copyText(value)
                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
            }
        )
    }
}

@Composable
fun AmountCell(
    fiatAmount: String?,
    coinAmount: String,
    type: ValueType,
    token: Token
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
            iconUrl = token.coin.iconUrl,
            placeholder = token.iconPlaceholder,
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
        subhead2_grey(text = fiatAmount ?: "")
    }
}

@Composable
fun TokenCell(token: Token) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinImage(
            iconUrl = token.coin.iconUrl,
            placeholder = token.iconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
        )
        subhead1_leah(token.coin.code)
    }
}

@Composable
fun AmountMultiCell(amounts: List<AmountValues>, type: ValueType, token: Token) {
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
            iconUrl = token.coin.iconUrl,
            placeholder = token.iconPlaceholder,
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
                subhead1_grey(text = amounts[0].fiatAmount ?: "")
            }
            if (amounts.size > 1) {
                Spacer(Modifier.height(3.dp))
                Row {
                    caption_grey(text = amounts[1].coinAmount)
                    Spacer(Modifier.weight(1f))
                    caption_grey(text = amounts[1].fiatAmount ?: "")
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
        body_leah(text = title)
        Spacer(Modifier.weight(1f))
        subhead1_grey(text = value)
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
        subhead2_grey(
            modifier = Modifier.padding(end = 36.dp),
            text = title,
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
