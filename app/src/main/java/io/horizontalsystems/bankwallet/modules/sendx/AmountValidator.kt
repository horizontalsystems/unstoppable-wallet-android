package io.horizontalsystems.bankwallet.modules.sendx

import java.math.BigDecimal

class AmountValidator {

    fun validate(
        coinAmount: BigDecimal?,
        coinCode: String,
        availableBalance: BigDecimal,
        minimumSendAmount: BigDecimal? = null,
        maximumSendAmount: BigDecimal? = null
    ) = when {
        coinAmount == null -> null
        coinAmount == BigDecimal.ZERO -> null
        coinAmount > availableBalance -> {
            SendErrorInsufficientBalance(coinCode)
        }
        minimumSendAmount != null && coinAmount <= minimumSendAmount -> {
            SendErrorMinimumSendAmount(minimumSendAmount)
        }
        maximumSendAmount != null && coinAmount > maximumSendAmount -> {
            SendErrorMaximumSendAmount(maximumSendAmount)
        }
        else -> null
    }

}
