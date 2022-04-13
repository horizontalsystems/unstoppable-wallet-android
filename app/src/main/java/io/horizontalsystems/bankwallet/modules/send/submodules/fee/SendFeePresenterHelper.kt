package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModule
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.PlatformCoin
import java.math.BigDecimal

class SendFeePresenterHelper(
        private val numberFormatter: IAppNumberFormatter,
        private val coin: PlatformCoin,
        private val baseCurrency: Currency) {

    fun feeAmount(coinAmount: BigDecimal? = null, inputType: AmountInputModule.InputType, rate: BigDecimal?): String? {
        return when (inputType) {
            AmountInputModule.InputType.COIN -> coinAmount?.let {
                numberFormatter.formatCoin(it, coin.code, 0, 8)
            }
            AmountInputModule.InputType.CURRENCY -> {
                rate?.let { rateValue ->
                    coinAmount?.times(rateValue)?.let { amount ->
                        numberFormatter.formatFiat(amount, baseCurrency.symbol, 2, 2)
                    }
                }
            }
        }
    }

}
