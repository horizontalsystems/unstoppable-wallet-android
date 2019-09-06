package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import java.math.BigDecimal

class SendFeePresenterHelper(
        private val numberFormatter: IAppNumberFormatter,
        private val coin: Coin,
        private val baseCurrency: Currency) {

    fun feeAmount(coinAmount: BigDecimal? = null, inputType: SendModule.InputType, rate: Rate?): String? {
        return when (inputType) {
            SendModule.InputType.COIN -> coinAmount?.let {
                numberFormatter.format(CoinValue(coin, it))
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

    fun duration(durationInSecs: Long): String {
        return DateHelper.getTxDurationString(App.instance, durationInSecs)
    }

    fun durationInterval(durationInSecs: Long): String {
        return DateHelper.getTxDurationIntervalString(App.instance, durationInSecs)
    }

    fun priority(priority: FeeRatePriority): String = when (priority) {
        FeeRatePriority.LOW -> App.instance.getString(R.string.Send_TxSpeed_Low)
        FeeRatePriority.MEDIUM -> App.instance.getString(R.string.Send_TxSpeed_Medium)
        FeeRatePriority.HIGH -> App.instance.getString(R.string.Send_TxSpeed_High)
    }

}
