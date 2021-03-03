package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal
import java.math.RoundingMode

class SendAmountPresenterHelper(
        private val numberFormatter: IAppNumberFormatter,
        private val coin: Coin,
        private val baseCurrency: Currency,
        private val coinDecimal: Int,
        private val currencyDecimal: Int) {

    fun getAmount(coinAmount: BigDecimal?, inputType: SendModule.InputType, rate: BigDecimal?): String {
        val amount = when (inputType) {
            SendModule.InputType.COIN -> {
                coinAmount?.setScale(coinDecimal, RoundingMode.DOWN)
            }
            SendModule.InputType.CURRENCY -> {
                rate?.let { coinAmount?.times(it) }?.let {
                    val scale = if (it >= BigDecimal(1000)) 0 else currencyDecimal

                    it.setScale(scale, RoundingMode.DOWN)
                }
            }
        } ?: BigDecimal.ZERO

        return if (amount > BigDecimal.ZERO) amount.stripTrailingZeros().toPlainString() else ""
    }


    fun getHint(coinAmount: BigDecimal? = null, inputType: SendModule.InputType, rate: BigDecimal?): String? {
        return when (inputType) {
            SendModule.InputType.CURRENCY -> {
                numberFormatter.formatCoin(coinAmount ?: BigDecimal.ZERO, coin.code, 0, 8)
            }
            SendModule.InputType.COIN -> {
                rate?.let {
                    numberFormatter.formatFiat(coinAmount?.times(it) ?: BigDecimal.ZERO, baseCurrency.symbol, 2, 2)
                }
            }
        }
    }

    fun getAvailableBalance(coinAmount: BigDecimal? = null, inputType: SendModule.InputType, rate: BigDecimal?): String? {
        return when (inputType) {
            SendModule.InputType.CURRENCY -> {
                rate?.let {
                    numberFormatter.formatFiat(coinAmount?.times(it) ?: BigDecimal.ZERO, baseCurrency.symbol, 2, 2)
                }
            }
            SendModule.InputType.COIN -> {
                numberFormatter.formatCoin(coinAmount ?: BigDecimal.ZERO, coin.code, 0, 8)
            }
        }
    }

    fun getAmountPrefix(inputType: SendModule.InputType, rate: BigDecimal?): String? {
        return when {
            inputType == SendModule.InputType.CURRENCY && rate != null -> baseCurrency.symbol
            else -> null
        }
    }

    fun getCoinAmount(amount: BigDecimal?, inputType: SendModule.InputType, rate: BigDecimal?): BigDecimal? {
        return when (inputType) {
            SendModule.InputType.CURRENCY -> rate?.let { amount?.divide(it, coinDecimal, RoundingMode.CEILING) }
            else -> amount
        }
    }

    fun decimal(inputType: SendModule.InputType) = if (inputType == SendModule.InputType.COIN) coinDecimal else currencyDecimal

}
