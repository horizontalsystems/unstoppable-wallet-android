package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal
import java.math.RoundingMode

class SendAmountPresenterHelper(
        private val numberFormatter: IAppNumberFormatter,
        private val coin: Coin,
        private val baseCurrency: Currency,
        private val coinDecimal: Int,
        private val currencyDecimal: Int) {

    fun getAmount(coinAmount: BigDecimal?, inputType: SendModule.InputType, rate: Rate?): String {
        val amount = when (inputType) {
            SendModule.InputType.COIN -> {
                coinAmount?.setScale(coinDecimal, RoundingMode.DOWN)
            }
            SendModule.InputType.CURRENCY -> {
                val currencyAmount = rate?.let { coinAmount?.times(it.value) }
                currencyAmount?.setScale(currencyDecimal, RoundingMode.DOWN)
            }
        } ?: BigDecimal.ZERO

        return if (amount > BigDecimal.ZERO) amount.stripTrailingZeros().toPlainString() else ""
    }


    fun getHint(coinAmount: BigDecimal? = null, inputType: SendModule.InputType, rate: Rate?): String? {
        return when (inputType) {
            SendModule.InputType.CURRENCY -> coinAmount?.let {
                numberFormatter.format(CoinValue(coin, it), realNumber = true)
            }
            SendModule.InputType.COIN -> {
                rate?.value?.let { rateValue ->
                    coinAmount?.times(rateValue)?.let { amount ->
                        numberFormatter.format(CurrencyValue(baseCurrency, amount))
                    }
                }
            }
        }
    }

    fun getAmountPrefix(inputType: SendModule.InputType, rate: Rate?): String? {
        return when {
            inputType == SendModule.InputType.COIN -> coin.code
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
