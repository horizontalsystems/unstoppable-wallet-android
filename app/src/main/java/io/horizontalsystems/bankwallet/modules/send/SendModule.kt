package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import java.math.BigDecimal

object SendModule {

    data class AmountData(val primary: AmountInfo, val secondary: AmountInfo?) {
        fun getFormatted(): String {
            var formatted = primary.getFormattedPlain()

            secondary?.let {
                formatted += "  |  " + it.getFormattedPlain()
            }

            return formatted
        }
    }

    sealed class AmountInfo {
        data class CoinValueInfo(val coinValue: CoinValue) : AmountInfo()
        data class CurrencyValueInfo(val currencyValue: CurrencyValue) : AmountInfo()

        val value: BigDecimal
            get() = when (this) {
                is CoinValueInfo -> coinValue.value
                is CurrencyValueInfo -> currencyValue.value
            }

        val decimal: Int
            get() = when (this) {
                is CoinValueInfo -> coinValue.decimal
                is CurrencyValueInfo -> currencyValue.currency.decimal
            }

        fun getAmountName(): String = when (this) {
            is CoinValueInfo -> coinValue.coin.name
            is CurrencyValueInfo -> currencyValue.currency.code
        }

        fun getFormatted(): String = when (this) {
            is CoinValueInfo -> {
                coinValue.getFormatted()
            }
            is CurrencyValueInfo -> {
                App.numberFormatter.formatFiat(currencyValue.value, currencyValue.currency.symbol, 2, 2)
            }
        }

        fun getFormattedPlain(): String = when (this) {
            is CoinValueInfo -> {
                App.numberFormatter.format(value, 0, 8)
            }
            is CurrencyValueInfo -> {
                App.numberFormatter.formatFiat(currencyValue.value, currencyValue.currency.symbol, 2, 2)
            }
        }

        fun getFormattedForTxInfo(): String = when (this) {
            is CoinValueInfo -> {
                coinValue.getFormatted()
            }
            is CurrencyValueInfo -> {
                val significantDecimal = App.numberFormatter.getSignificantDecimalFiat(currencyValue.value)

                App.numberFormatter.formatFiat(currencyValue.value, currencyValue.currency.symbol, 0, significantDecimal)
            }
        }


    }

}
