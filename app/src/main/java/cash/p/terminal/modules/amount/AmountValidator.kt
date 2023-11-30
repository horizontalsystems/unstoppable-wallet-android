package cash.p.terminal.modules.amount

import cash.p.terminal.R
import cash.p.terminal.core.HSCaution
import cash.p.terminal.modules.send.SendErrorInsufficientBalance
import cash.p.terminal.modules.send.SendErrorMaximumSendAmount
import cash.p.terminal.modules.send.SendErrorMinimumSendAmount
import cash.p.terminal.ui.compose.TranslatableString
import java.math.BigDecimal

class AmountValidator {

    fun validate(
        coinAmount: BigDecimal?,
        coinCode: String,
        availableBalance: BigDecimal,
        minimumSendAmount: BigDecimal? = null,
        maximumSendAmount: BigDecimal? = null,
        leaveSomeBalanceForFee: Boolean = false
    ) = when {
        coinAmount == null -> null
        coinAmount == BigDecimal.ZERO -> null
        coinAmount > availableBalance -> {
            SendErrorInsufficientBalance(coinCode)
        }
        minimumSendAmount != null && coinAmount < minimumSendAmount -> {
            SendErrorMinimumSendAmount(minimumSendAmount)
        }
        maximumSendAmount != null && coinAmount > maximumSendAmount -> {
            SendErrorMaximumSendAmount(maximumSendAmount)
        }
        leaveSomeBalanceForFee && coinAmount == availableBalance -> {
            HSCaution(
                TranslatableString.ResString(R.string.EthereumTransaction_Warning_CoinNeededForFee, coinCode),
                HSCaution.Type.Warning
            )
        }
        else -> null
    }

}
