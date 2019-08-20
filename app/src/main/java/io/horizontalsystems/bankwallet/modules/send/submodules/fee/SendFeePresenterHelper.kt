package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal

class SendFeePresenterHelper(
        private val numberFormatter: IAppNumberFormatter,
        private val coinCode: String,
        private val baseCurrency: Currency) {

    fun feeAmount(coinAmount: BigDecimal? = null, inputType: SendModule.InputType, rate: Rate?): String? {
        return when (inputType) {
            SendModule.InputType.COIN -> coinAmount?.let {
                numberFormatter.format(CoinValue(coinCode, it))
            }
            SendModule.InputType.CURRENCY -> {
                rate?.value?.let { rateValue ->
                    coinAmount?.times(rateValue)?.let { amount ->
                        numberFormatter.format(CurrencyValue(baseCurrency, amount))
                    }
                }
            }
        }
    }

}
