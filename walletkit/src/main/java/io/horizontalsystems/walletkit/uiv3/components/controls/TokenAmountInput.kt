package io.horizontalsystems.walletkit.uiv3.components.controls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import io.horizontalsystems.walletkit.ui.compose.components.body_grey
import io.horizontalsystems.walletkit.ui.compose.components.headline1_leah
import java.math.BigDecimal

/**
 * The amount block: the available balance, the token with its badge, and the amount with
 * its fiat value. [token] is what the amount is in and may be unchosen; [balanceToken] is
 * what funds it and formats [availableBalance], and the balance line is shown only when both
 * are known. An [onTokenClick] makes the token a selector with a drop-down arrow. An
 * [onAvailableBalanceClick] makes the balance line a shortcut, shown in blue; without one it
 * is a plain grey note.
 *
 * The block keeps its height while the token or the fiat rate is still unknown, so a card
 * built around it does not jump once they resolve.
 */
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
    onFocusChanged: (Boolean) -> Unit,
    amountExceedsBalance: Boolean = false,
    balanceToken: Token? = null,
    availableBalance: BigDecimal? = null,
    onTokenClick: (() -> Unit)? = null,
    onAvailableBalanceClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .onFocusChanged { onFocusChanged(it.hasFocus) }
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        if (availableBalance != null && balanceToken != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
                        App.numberFormatter.formatCoinFull(availableBalance, balanceToken.coin.code, balanceToken.decimals)
                    ),
                    style = ComposeAppTheme.typography.caption,
                    color = if (onAvailableBalanceClick != null) ComposeAppTheme.colors.ocean else ComposeAppTheme.colors.andy,
                )
            }
            VSpacer(8.dp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TokenSelector(token = token, onClick = onTokenClick)
            HSpacer(8.dp)
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                AmountInput(
                    value = amount,
                    onValueChange = onValueChange,
                    focusRequester = focusRequester,
                    textColor = if (amountExceedsBalance) ComposeAppTheme.colors.lucian else ComposeAppTheme.colors.leah,
                    placeholderColor = ComposeAppTheme.colors.leah,
                )
                VSpacer(3.dp)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                headline1_leah(text = token?.coin?.code ?: stringResource(R.string.CrossPay_ChooseCoin))
                if (onClick != null) {
                    HSpacer(4.dp)
                    Icon(
                        painter = painterResource(R.drawable.arrow_s_down_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.leah,
                    )
                }
            }
            VSpacer(5.dp)
            BadgeText(
                modifier = if (token == null) Modifier.alpha(0f) else Modifier,
                text = token?.badge ?: stringResource(R.string.CoinPlatforms_Native),
                background = ComposeAppTheme.colors.blade,
                textColor = ComposeAppTheme.colors.leah,
            )
        }
    }
}

/** Invisible fiat line reserving the space the fiat text or input takes once a rate is known. */
@Composable
fun FiatLinePlaceholder(currency: Currency) {
    body_grey(
        modifier = Modifier.alpha(0f),
        text = "${currency.symbol}0",
    )
}
