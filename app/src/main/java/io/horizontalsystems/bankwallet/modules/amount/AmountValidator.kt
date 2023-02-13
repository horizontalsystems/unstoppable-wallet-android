package io.horizontalsystems.bankwallet.modules.amount

import io.horizontalsystems.bankwallet.modules.send.SendErrorInsufficientBalance
import io.horizontalsystems.bankwallet.modules.send.SendErrorMinimumSendAmount
import java.math.BigDecimal

class AmountValidator {

    fun validate(
        coinAmount: BigDecimal?,
        coinCode: String,
        availableBalance: BigDecimal,
        minimumSendAmount: BigDecimal? = null,
    ) = when {
        coinAmount == null -> null
        coinAmount == BigDecimal.ZERO -> null
        coinAmount > availableBalance -> {
            SendErrorInsufficientBalance(coinCode)
        }
        minimumSendAmount != null && coinAmount <= minimumSendAmount -> {
            SendErrorMinimumSendAmount(minimumSendAmount)
        }
        else -> null
    }

}
