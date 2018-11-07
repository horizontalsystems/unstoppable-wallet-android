package bitcoin.wallet.viewHelpers

import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.CurrencyValue
import java.math.RoundingMode
import java.text.NumberFormat


object ValueFormatter {

    private val coinFormatter: NumberFormat
        get() {
            val format: NumberFormat = NumberFormat.getInstance()
            format.minimumFractionDigits = 2
            format.maximumFractionDigits = 8
            format.roundingMode = RoundingMode.CEILING
            return format
        }

    private val currencyFormatter: NumberFormat
        get() {
            val format: NumberFormat = NumberFormat.getInstance()
            format.minimumFractionDigits = 2
            format.roundingMode = RoundingMode.CEILING
            return format
        }

    fun format(coinValue: CoinValue, explicitSign: Boolean = false): String? {
        val value = if (explicitSign)  Math.abs (coinValue.value) else coinValue.value

        val formattedValue = coinFormatter.format(value) ?:kotlin.run{
            return null
        }

        var result = "$formattedValue ${coinValue.coin.code}"

        if (explicitSign) {
            val sign = if (coinValue.value < 0) "-" else "+"
            result = "$sign $result"
        }

        return result
    }

    fun format(currencyValue: CurrencyValue, approximate: Boolean = false): String? {
        val formatter = currencyFormatter
        formatter.maximumFractionDigits = if (approximate) 0 else 2

        var value = currencyValue.value

        if (approximate) {
            value = Math.abs(value)
        }

        var result: String = formatter.format(value) ?: kotlin.run {
            return null
        }

        result = "${currencyValue.currency.symbol}$result"

        if (approximate) {
            result = "~ $result"
        }

        return result
    }
}
