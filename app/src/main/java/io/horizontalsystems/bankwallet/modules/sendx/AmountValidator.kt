package io.horizontalsystems.bankwallet.modules.sendx

import java.math.BigDecimal

class AmountValidator {

    fun validate(
        coinAmount: BigDecimal?,
        coinCode: String,
        availableBalance: BigDecimal,
        tmpMinimumSendAmount: BigDecimal? = null,
        tmpMaximumSendAmount: BigDecimal? = null
    ) = when {
        coinAmount == null -> null
        coinAmount == BigDecimal.ZERO -> null
        coinAmount > availableBalance -> {
            SendErrorInsufficientBalance(coinCode)
        }
        tmpMinimumSendAmount != null && coinAmount <= tmpMinimumSendAmount -> {
            SendErrorMinimumSendAmount(tmpMinimumSendAmount)
        }
        tmpMaximumSendAmount != null && coinAmount > tmpMaximumSendAmount -> {
            SendErrorMaximumSendAmount(tmpMaximumSendAmount)
        }
        else -> null
    }

}
