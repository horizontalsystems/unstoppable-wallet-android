package io.horizontalsystems.walletkit.uiv3.components.controls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.modules.multiswap.AmountInput
import io.horizontalsystems.walletkit.modules.multiswap.FiatAmountInput
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.BadgeText
import io.horizontalsystems.walletkit.uiv3.components.cell.CellLeftCoinIcon
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.headline1_leah
import io.horizontalsystems.walletkit.uiv3.components.BoxBordered
import java.math.BigDecimal

@Composable
fun TokenAmountInput(
    token: Token?,
    amount: BigDecimal?,
    fiatAmount: BigDecimal?,
    fiatAmountInputEnabled: Boolean,
    currency: Currency,
    focusRequester: FocusRequester,
    onValueChange: (BigDecimal?) -> Unit,
    onFiatValueChange: (BigDecimal?) -> Unit,
    amountExceedsBalance: Boolean,
    onTokenClick: (() -> Unit)?
) {
    BoxBordered(bottom = true) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TokenSelector(token = token, onClick = onTokenClick)
            HSpacer(8.dp)
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                AmountInput(
                    value = amount,
                    onValueChange = onValueChange,
                    focusRequester = focusRequester,
                    error = amountExceedsBalance
                )
                if (fiatAmountInputEnabled || fiatAmount != null) {
                    FiatAmountInput(
                        value = fiatAmount,
                        currency = currency,
                        onValueChange = onFiatValueChange,
                        enabled = fiatAmountInputEnabled,
                    )
                } else {
                    FiatLinePlaceholder(currency)
                }
            }
        }
    }
}

@Composable
fun AvailableBalanceRow(
    balanceToken: Token?,
    availableBalance: BigDecimal?,
    onAvailableBalanceClick: (() -> Unit)? = null,
    onPercentClick: ((Int) -> Unit)? = null,
    onClearClick: (() -> Unit)? = null,
    showClear: Boolean = false,
) {
    if (availableBalance != null && balanceToken != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = if (onAvailableBalanceClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAvailableBalanceClick,
                    )
                } else {
                    Modifier
                },
                text = stringResource(
                    R.string.Send_Available,
                    App.numberFormatter.formatCoinFull(
                        availableBalance,
                        balanceToken.coin.code,
                        balanceToken.decimals
                    )
                ),
                style = ComposeAppTheme.typography.caption,
                color = if (onAvailableBalanceClick != null) ComposeAppTheme.colors.ocean else ComposeAppTheme.colors.grey,
            )

            if (showClear) {
                Text(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onClearClick?.invoke() }
                    ),
                    text = stringResource(R.string.Action_Clear),
                    style = ComposeAppTheme.typography.caption,
                    color = ComposeAppTheme.colors.ocean,
                )
            } else if (onPercentClick != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    for (percent in listOf(25, 50, 75)) {
                        Text(
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onPercentClick(percent) }
                            ),
                            text = "$percent%",
                            style = ComposeAppTheme.typography.caption,
                            color = ComposeAppTheme.colors.ocean,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The token half of [TokenAmountInput]: icon, code, and chain badge. With an [onClick] it
 * is a selector with a drop-down arrow. An unchosen token shows a prompt in the code's place
 * and an invisible badge, so the block already has its final height.
 */
@Composable
fun TokenSelector(
    token: Token?,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = if (onClick != null) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
        } else {
            Modifier
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CellLeftCoinIcon(token = token)
        HSpacer(16.dp)
        Column {
            headline1_leah(text = token?.coin?.code ?: stringResource(R.string.CrossPay_ChooseCoin))
            VSpacer(5.dp)
            BadgeText(
                modifier = if (token == null) Modifier.alpha(0f) else Modifier,
                text = token?.badge ?: stringResource(R.string.CoinPlatforms_Native),
                background = ComposeAppTheme.colors.blade,
                textColor = ComposeAppTheme.colors.leah,
            )
        }
        if (onClick != null) {
            HSpacer(8.dp)
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.arrow_s_down_24),
                contentDescription = null,
                tint = ComposeAppTheme.colors.leah,
            )
        }
    }
}

/** Invisible fiat line reserving the space the fiat text or input takes once a rate is known. */
@Composable
fun FiatLinePlaceholder(currency: Currency) {
    Text(
        modifier = Modifier.alpha(0f),
        text = "${currency.symbol}0",
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.andy,
    )
}
