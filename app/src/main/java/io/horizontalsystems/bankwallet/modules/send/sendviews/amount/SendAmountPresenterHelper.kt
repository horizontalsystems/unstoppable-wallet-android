package io.horizontalsystems.bankwallet.modules.send.sendviews.amount

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal
import java.math.RoundingMode

class SendAmountPresenterHelper(
        private val coinCode: String,
        private val baseCurrency: Currency,
        private val coinDecimal: Int,
        private val currencyDecimal: Int) {

    fun getAmountInfo(coinAmount: BigDecimal? = null, inputType: SendModule.InputType, rate: Rate?): SendModule.AmountInfo? {
        return when (inputType) {
            SendModule.InputType.COIN -> {
                coinAmount?.let {
                    val rounded = it.setScale(coinDecimal, RoundingMode.DOWN)
                    val coinValue = CoinValue(coinCode, rounded)
                    SendModule.AmountInfo.CoinValueInfo(coinValue)
                }
            }
            SendModule.InputType.CURRENCY -> {
                val currencyAmount = rate?.let { coinAmount?.times(it.value) }
                currencyAmount?.let {
                    val rounded = it.setScale(currencyDecimal, RoundingMode.DOWN)
                    val currencyValue = CurrencyValue(baseCurrency, rounded)
                    SendModule.AmountInfo.CurrencyValueInfo(currencyValue)
                }
            }
        }
    }

     fun getHintInfo(coinAmount: BigDecimal? = null, inputType: SendModule.InputType, rate: Rate?): SendModule.AmountInfo? {
        return when (inputType) {
            SendModule.InputType.CURRENCY -> coinAmount?.let {
                SendModule.AmountInfo.CoinValueInfo(CoinValue(coinCode, it))
            }
            SendModule.InputType.COIN -> {
                rate?.value?.let { rateValue ->
                    coinAmount?.times(rateValue)?.let { amount ->
                        SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(baseCurrency, amount))
                    }
                }
            }
        }
    }

     fun getAmountPrefix(inputType: SendModule.InputType, rate: Rate?): String? {
        return when {
            inputType == SendModule.InputType.COIN -> coinCode
            rate == null -> null
            else -> baseCurrency.symbol
        }
    }

    fun getCoinAmount(amount: BigDecimal?, inputType: SendModule.InputType, rate: Rate?): BigDecimal? {
        return when (inputType) {
            SendModule.InputType.CURRENCY -> rate?.let { amount?.divide(it.value, 8, RoundingMode.CEILING) }
            else -> amount
        }
    }

    fun decimal(inputType: SendModule.InputType) = if (inputType == SendModule.InputType.COIN) coinDecimal else currencyDecimal
}