package cash.p.terminal.modules.fee

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.modules.send.fee.NetworkFeeWarningData
import cash.p.terminal.ui.compose.components.CardsSwapInfo
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.caption_jacob
import cash.p.terminal.ui_compose.components.caption_lucian
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.components.subhead2_lucian
import cash.p.terminal.wallet.Token
import java.math.BigDecimal

@Composable
fun FeeInfoSection(
    tokenIn: Token?,
    displayBalance: BigDecimal?,
    balanceHidden: Boolean,
    feeToken: Token?,
    feeCoinBalance: BigDecimal?,
    feePrimary: String,
    feeSecondary: String,
    insufficientFeeBalance: Boolean,
    onBalanceClicked: () -> Unit,
    feeTitle: String? = null,
    feeLoading: Boolean = false,
    feeWarningData: NetworkFeeWarningData? = null,
) {
    val isNativeCoinSwap = feeCoinBalance == null
    val feeWarningText = feeWarningData?.let {
        stringResource(R.string.fee_warning_low_balance, it.balanceThreshold)
    }
    val showWarning = feeWarningText != null && !insufficientFeeBalance

    CardsSwapInfo {
        AvailableBalanceField(
            tokenIn = tokenIn,
            availableBalance = displayBalance,
            balanceHidden = balanceHidden,
            isError = insufficientFeeBalance && isNativeCoinSwap,
            toggleHideBalance = onBalanceClicked
        )
    }

    VSpacer(height = 12.dp)

    subhead2_grey(
        text = stringResource(R.string.FeeSettings_NetworkFee),
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
    )

    CardsSwapInfo(isError = insufficientFeeBalance, isWarning = showWarning) {
        FeeCoinBalanceField(
            feeToken = feeToken,
            feeCoinBalance = feeCoinBalance,
            balanceHidden = balanceHidden,
            isError = insufficientFeeBalance,
            toggleHideBalance = onBalanceClicked,
        )
        DataFieldFee(
            primary = feePrimary,
            secondary = feeSecondary,
            borderTop = feeCoinBalance != null,
            loading = feeLoading,
            title = feeTitle ?: stringResource(R.string.fee),
        )
    }

    if (insufficientFeeBalance) {
        val feeTokenCode = feeToken?.coin?.code ?: tokenIn?.coin?.code ?: ""
        VSpacer(height = 8.dp)
        caption_lucian(
            text = stringResource(R.string.swap_insufficient_fee_balance, feeTokenCode),
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    } else if (feeWarningText != null) {
        VSpacer(height = 8.dp)
        caption_jacob(
            text = feeWarningText,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun BalanceText(
    text: String,
    balanceHidden: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
) {
    val displayText = if (!balanceHidden) text else "*****"
    val clickModifier = Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
    if (isError) {
        subhead2_lucian(text = displayText, modifier = clickModifier)
    } else {
        subhead2_leah(text = displayText, modifier = clickModifier)
    }
}

@Composable
private fun AvailableBalanceField(
    tokenIn: Token?,
    availableBalance: BigDecimal?,
    balanceHidden: Boolean,
    isError: Boolean,
    toggleHideBalance: () -> Unit
) {
    QuoteInfoRow(
        title = {
            subhead2_grey(text = stringResource(R.string.Swap_AvailableBalance))
        },
        value = {
            val text = if (tokenIn != null && availableBalance != null) {
                CoinValue(tokenIn, availableBalance).getFormattedFull()
            } else {
                "-"
            }
            BalanceText(text, balanceHidden, isError, toggleHideBalance)
        }
    )
}

@Composable
private fun FeeCoinBalanceField(
    feeToken: Token?,
    feeCoinBalance: BigDecimal?,
    balanceHidden: Boolean,
    isError: Boolean,
    toggleHideBalance: () -> Unit,
) {
    if (feeToken == null || feeCoinBalance == null) return

    QuoteInfoRow(
        title = {
            subhead2_grey(
                text = stringResource(R.string.swap_balance_for_fees)
            )
        },
        value = {
            val text = CoinValue(feeToken, feeCoinBalance).getFormattedFull()
            BalanceText(text, balanceHidden, isError, toggleHideBalance)
        }
    )
}
