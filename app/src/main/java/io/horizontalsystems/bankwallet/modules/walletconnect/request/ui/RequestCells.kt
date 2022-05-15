package io.horizontalsystems.bankwallet.modules.walletconnect.request.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ValueType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.Ellipsis
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper

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
fun AmountCell(fiatAmount: String?, coinAmount: String, type: ValueType) {
    val coinAmountColor = when (type) {
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
            text = fiatAmount ?: "",
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = coinAmount,
            color = coinAmountColor,
            style = ComposeAppTheme.typography.subhead1
        )
    }
}

@Composable
fun SubheadCell(title: String, value: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            text = title,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            color = valueColor,
            style = ComposeAppTheme.typography.subhead1
        )
    }
}
