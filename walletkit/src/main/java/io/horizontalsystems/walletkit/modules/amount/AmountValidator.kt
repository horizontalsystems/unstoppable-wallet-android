package io.horizontalsystems.walletkit.modules.amount

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.HSCaution
import io.horizontalsystems.walletkit.modules.send.SendErrorInsufficientBalance
import io.horizontalsystems.walletkit.modules.send.SendErrorMaximumSendAmount
import io.horizontalsystems.walletkit.modules.send.SendErrorMinimumSendAmount
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
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
