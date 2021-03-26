package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.core.ILanguageManager
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.log10

class NumberFormatter(private val languageManager: ILanguageManager) : IAppNumberFormatter {

    private var formatters: MutableMap<String, NumberFormat> = mutableMapOf()

    override fun format(value: Number, minimumFractionDigits: Int, maximumFractionDigits: Int, prefix: String, suffix: String): String {
        val bigDecimalValue = when (value) {
            is Double -> value.toBigDecimal()
            is Float -> value.toBigDecimal()
            is BigDecimal -> value
            else -> throw UnsupportedOperationException()
        }

        val formatter = getFormatter(languageManager.currentLocale, minimumFractionDigits, maximumFractionDigits)

        val mostLowValue = BigDecimal(BigInteger.ONE, maximumFractionDigits)

        return if (bigDecimalValue > BigDecimal.ZERO && bigDecimalValue < mostLowValue) {
            "< " + prefix + formatter.format(mostLowValue) + suffix
        } else {
            prefix + formatter.format(bigDecimalValue) + suffix
        }
    }

    override fun formatCoin(value: Number, code: String, minimumFractionDigits: Int, maximumFractionDigits: Int): String {
        return format(value, minimumFractionDigits, maximumFractionDigits, suffix = " $code")
    }

    override fun formatFiat(value: Number, symbol: String, minimumFractionDigits: Int, maximumFractionDigits: Int): String {
        val finalMinimumFractionDigits: Int
        val finalMaximimFractionDigits: Int

        if (value.toInt() >= 1000) {
            finalMinimumFractionDigits = 0
            finalMaximimFractionDigits = 0
        } else {
            finalMinimumFractionDigits = minimumFractionDigits
            finalMaximimFractionDigits = maximumFractionDigits
        }

        return format(value, finalMinimumFractionDigits, finalMaximimFractionDigits, prefix = symbol)
    }

    override fun getSignificantDecimalFiat(value: BigDecimal): Int {
        if (value == BigDecimal.ZERO || value >= BigDecimal(1)) {
            return 2
        }

        val numberOfZerosAfterDot = value.scale() - value.precision()

        return if (numberOfZerosAfterDot >= 4) {
            numberOfZerosAfterDot + 4
        } else {
            4
        }
    }

    override fun getSignificantDecimalCoin(value: BigDecimal): Int {
        val absValue = value.abs()
        val valueBeforeDot = absValue.setScale(0, RoundingMode.FLOOR)
        val valueAfterDot = absValue - valueBeforeDot

        return when {
            valueBeforeDot < BigDecimal("10") && valueAfterDot < BigDecimal("0.0001") -> 8
            valueBeforeDot < BigDecimal("100") -> 4
            else -> 2
        }
    }

    private fun getFormatter(locale: Locale, minimumFractionDigits: Int, maximumFractionDigits: Int): NumberFormat {
        val formatterId = "${locale.language}-$minimumFractionDigits-$maximumFractionDigits"

        if (formatters[formatterId] == null) {
            formatters[formatterId] = NumberFormat.getInstance(locale).apply {
                this.roundingMode = RoundingMode.FLOOR

                this.minimumFractionDigits = minimumFractionDigits
                this.maximumFractionDigits = maximumFractionDigits
            }
        }

        return formatters[formatterId] ?: throw Exception("No formatter")
    }

    override fun shortenValue(number: Number): Pair<BigDecimal, String> {
        val suffix = arrayOf(
                " ",
                App.instance.localizedContext().getString(R.string.CoinPage_MarketCap_Thousand),
                App.instance.localizedContext().getString(R.string.CoinPage_MarketCap_Million),
                App.instance.localizedContext().getString(R.string.CoinPage_MarketCap_Billion),
                App.instance.localizedContext().getString(R.string.CoinPage_MarketCap_Trillion))

        val valueLong = number.toLong()
        val value = floor(log10(valueLong.toDouble())).toInt()
        val base = value / 3

        var returnSuffix = ""
        var valueDecimal = valueLong.toBigDecimal()
        if (value >= 3 && base < suffix.size) {
            valueDecimal = (valueLong / Math.pow(10.0, (base * 3).toDouble())).toBigDecimal()
            returnSuffix = suffix[base]
        }

        val roundedDecimalValue = if (valueDecimal < BigDecimal.TEN) {
            valueDecimal.setScale(2, RoundingMode.HALF_EVEN)
        } else {
            valueDecimal.setScale(1, RoundingMode.HALF_EVEN)
        }

        return Pair(roundedDecimalValue, returnSuffix)
    }

}
