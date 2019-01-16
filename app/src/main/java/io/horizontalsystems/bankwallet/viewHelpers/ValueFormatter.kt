package io.horizontalsystems.bankwallet.viewHelpers

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import java.math.RoundingMode
import java.text.NumberFormat


object ValueFormatter {

    private const val COIN_BIG_NUMBER_EDGE = 0.0001
    private const val FIAT_BIG_NUMBER_EDGE = 100.0

    private val coinFormatter: NumberFormat
        get() {
            val format: NumberFormat = NumberFormat.getInstance()
            format.maximumFractionDigits = 8
            return format
        }

    private val currencyFormatter: NumberFormat
        get() {
            val format: NumberFormat = NumberFormat.getInstance()
            format.maximumFractionDigits = 2
            format.roundingMode = RoundingMode.HALF_EVEN
            return format
        }

    fun format(value: Double): String {
        val customFormatter = coinFormatter
        if (value == 0.0) {
            customFormatter.maximumFractionDigits = 0
        }

        return customFormatter.format(value)
    }

    fun format(coinValue: CoinValue, explicitSign: Boolean = false): String? {
        val value = if (explicitSign) Math.abs(coinValue.value) else coinValue.value

        val customFormatter = coinFormatter

        if (value >= COIN_BIG_NUMBER_EDGE) {
            customFormatter.maximumFractionDigits = 4
        }

        val formattedValue = customFormatter.format(value) ?: run { return null }

        var result = "$formattedValue ${coinValue.coinCode}"

        if (explicitSign) {
            val sign = if (coinValue.value < 0) "-" else "+"
            result = "$sign $result"
        }

        return result
    }

    fun format(currencyValue: CurrencyValue, approximate: Boolean = false, showNegativeSign: Boolean = true): String? {
        var value = currencyValue.value

        value = Math.abs(value)

        val formatter = currencyFormatter

        if (value >= FIAT_BIG_NUMBER_EDGE || approximate) {
            formatter.maximumFractionDigits = 0
        }

        var result: String = formatter.format(value) ?: kotlin.run {
            return null
        }

        result = "${currencyValue.currency.symbol}$result"

        if (showNegativeSign && currencyValue.value < 0) {
            result = "- $result"
        }

        if (approximate) {
            result = "~ $result"
        }

        return result
    }

    fun formatSimple(currencyValue: CurrencyValue): String? {
        val result = currencyFormatter.format(currencyValue.value) ?: return null
        return "${currencyValue.currency.symbol}$result"
    }
}
