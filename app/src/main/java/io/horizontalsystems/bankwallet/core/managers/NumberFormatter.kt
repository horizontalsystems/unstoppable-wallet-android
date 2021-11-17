package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.core.ILanguageManager
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.log10

class NumberFormatter(
        private val languageManager: ILanguageManager
        ) : IAppNumberFormatter {

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
            valueBeforeDot < BigDecimal("1") -> 8
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

    override fun shortenValue(number: BigDecimal): Pair<BigDecimal, String> {
        if (number <= BigDecimal("100")) {
            val roundedNumber = if (number < BigDecimal.TEN) {
                number.setScale(2, RoundingMode.HALF_EVEN)
            } else {
                number.setScale(1, RoundingMode.HALF_EVEN)
            }

            return Pair(roundedNumber, "")
        }

        val suffix = arrayOf(
                " ",
                Translator.getString(R.string.CoinPage_MarketCap_Thousand),
                Translator.getString(R.string.CoinPage_MarketCap_Million),
                Translator.getString(R.string.CoinPage_MarketCap_Billion),
                Translator.getString(R.string.CoinPage_MarketCap_Trillion))

        val valueLong = number.toLong()
        val value = floor(log10(valueLong.toDouble())).toInt()
        val base = value / 3

        var returnSuffix = ""
        var valueDecimal = valueLong.toBigDecimal()
        if (value >= 3 && base < suffix.size) {
            valueDecimal = (valueLong / Math.pow(10.0, (base * 3).toDouble())).toBigDecimal()
            returnSuffix = suffix[base]
        }

        return Pair(valueDecimal.setScale(1, RoundingMode.HALF_EVEN), returnSuffix)
    }

    override fun formatCurrencyValueAsShortened(currencyValue: CurrencyValue): String {
        val (shortenValue, suffix) = shortenValue(currencyValue.value)
        return formatFiat(shortenValue, currencyValue.currency.symbol, 0, 2) + " $suffix"
    }

   override fun formatCoinValueAsShortened(number: BigDecimal, code: String): String {
        val (shortValue, suffix) = shortenValue(number)
        return format(shortValue, 0, 2) + " $suffix $code"
    }

    override fun formatValueAsDiff(value: Value): String =
        when (value) {
            is Value.Currency -> {
                val currencyValue = value.currencyValue
                val (shortValue, suffix) = shortenValue(currencyValue.value.abs())
                format(
                    shortValue.abs(),
                    0,
                    2,
                    "${sign(currencyValue.value)}${currencyValue.currency.symbol}",
                    " $suffix"
                )
            }
            is Value.Percent -> {
                format(value.percent.abs(), 0, 2, sign(value.percent), "%")
            }
        }

    private fun sign(value: BigDecimal): String {
        return when (value.signum()) {
            1 -> "+"
            -1 -> "-"
            else -> ""
        }
    }

}
