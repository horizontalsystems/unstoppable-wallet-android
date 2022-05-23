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
import kotlin.math.log10
import kotlin.math.pow

class NumberFormatter(
        private val languageManager: ILanguageManager
        ) : IAppNumberFormatter {

    private var formatters: MutableMap<String, NumberFormat> = mutableMapOf()
    private val numberRounding = NumberRounding()

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

    override fun formatCoinFull(value: BigDecimal, code: String, coinDecimals: Int): String {
        val rounded = numberRounding.getRoundedCoinFull(value, coinDecimals)

        val formattedNumber = format(rounded.value, 0, Int.MAX_VALUE)
        val res = when (rounded) {
            is BigDecimalRounded.Large -> {
                val suffixResId = when (rounded.suffix) {
                    NumberSuffix.Blank -> null
                    NumberSuffix.Thousand -> R.string.CoinPage_MarketCap_Thousand
                    NumberSuffix.Million -> R.string.CoinPage_MarketCap_Million
                    NumberSuffix.Billion -> R.string.CoinPage_MarketCap_Billion
                    NumberSuffix.Trillion -> R.string.CoinPage_MarketCap_Trillion
                }

                formattedNumber + suffixResId?.let {
                    " " + Translator.getString(it)
                }
            }
            is BigDecimalRounded.LessThen -> {
                "<$formattedNumber"
            }
            is BigDecimalRounded.Regular -> {
                formattedNumber
            }
        }

        return "$res $code"
    }

    override fun formatCoinShort(value: BigDecimal, code: String, coinDecimals: Int): String {
        val rounded = numberRounding.getRoundedCoinShort(value, coinDecimals)

        val formattedNumber = format(rounded.value, 0, Int.MAX_VALUE)
        val res = when (rounded) {
            is BigDecimalRounded.Large -> {
                val suffixResId = when (rounded.suffix) {
                    NumberSuffix.Blank -> null
                    NumberSuffix.Thousand -> R.string.CoinPage_MarketCap_Thousand
                    NumberSuffix.Million -> R.string.CoinPage_MarketCap_Million
                    NumberSuffix.Billion -> R.string.CoinPage_MarketCap_Billion
                    NumberSuffix.Trillion -> R.string.CoinPage_MarketCap_Trillion
                }

                formattedNumber + suffixResId?.let {
                    " " + Translator.getString(it)
                }
            }
            is BigDecimalRounded.LessThen -> {
                "<$formattedNumber"
            }
            is BigDecimalRounded.Regular -> {
                formattedNumber
            }
        }

        return "$res $code"
    }

    override fun formatNumberShort(value: BigDecimal, maximumFractionDigits: Int): String {
        val rounded = numberRounding.getRoundedShort(value, maximumFractionDigits)
        val formattedNumber = format(rounded.value, 0, Int.MAX_VALUE)

        return when (rounded) {
            is BigDecimalRounded.Large -> {
                val suffixResId = when (rounded.suffix) {
                    NumberSuffix.Blank -> null
                    NumberSuffix.Thousand -> R.string.CoinPage_MarketCap_Thousand
                    NumberSuffix.Million -> R.string.CoinPage_MarketCap_Million
                    NumberSuffix.Billion -> R.string.CoinPage_MarketCap_Billion
                    NumberSuffix.Trillion -> R.string.CoinPage_MarketCap_Trillion
                }

                formattedNumber + suffixResId?.let {
                    " " + Translator.getString(it)
                }
            }
            is BigDecimalRounded.LessThen -> {
                "<$formattedNumber"
            }
            is BigDecimalRounded.Regular -> {
                formattedNumber
            }
        }
    }

    override fun getShortenedForTxs(value: BigDecimal): BigDecimalShortened {
        val integralPart = value.setScale(0, RoundingMode.HALF_UP)
        return when {
            integralPart >= BigDecimal(10_000) -> {
                val shortened = getShortened(integralPart)

                BigDecimalShortened(getRounded(shortened.value), shortened.suffix)
            }
            else -> {
                BigDecimalShortened(getRounded(value), BigDecimalShortened.Suffix.Blank)
            }
        }
    }

    private fun getShortened(value: BigDecimal): BigDecimalShortened {
        val base = log10(value.toDouble()).toInt() + 1
        val groupCount = (base - 1) / 3

        return shortByGroupCount(groupCount, value)
    }

    private fun shortByGroupCount(
        groupCount: Int,
        value: BigDecimal
    ): BigDecimalShortened {
        return when (val suffix = BigDecimalShortened.Suffix.getByGroupCount(groupCount)) {
            null -> BigDecimalShortened(value, BigDecimalShortened.Suffix.Blank)
            else -> {
                val t = groupCount * 3
                val shortened = value.divide(BigDecimal(10.0.pow(t.toDouble())))
                if (shortened >= BigDecimal("999")) {
                    shortByGroupCount(groupCount + 1, value)
                } else {
                    BigDecimalShortened(shortened, suffix)
                }
            }
        }
    }

    private fun getRounded(value: BigDecimal): BigDecimal {
        val converted = when {
            value.compareTo(BigDecimal.ZERO) == 0  -> value
            value < BigDecimal("0.00000001") -> BigDecimal("0.00000001")
            value < BigDecimal("0.0001") -> value.setScale(8, RoundingMode.HALF_EVEN)
            value < BigDecimal("1") -> value.setScale(4, RoundingMode.HALF_EVEN)
            value < BigDecimal("10") -> value.setScale(2, RoundingMode.HALF_EVEN)
            value < BigDecimal("100") -> value.setScale(1, RoundingMode.HALF_EVEN)
            else -> value.setScale(0, RoundingMode.HALF_EVEN)
        }

        return converted.stripTrailingZeros()
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

    override fun formatFiatShort(
        value: BigDecimal,
        symbol: String,
        currencyDecimals: Int
    ): String {
        val rounded = numberRounding.getRoundedCurrencyShort(value, currencyDecimals)

        val formattedNumber = format(rounded.value, 0, Int.MAX_VALUE, prefix = symbol)

        return when (rounded) {
            is BigDecimalRounded.Large -> {
                val suffixResId = when (rounded.suffix) {
                    NumberSuffix.Blank -> null
                    NumberSuffix.Thousand -> R.string.CoinPage_MarketCap_Thousand
                    NumberSuffix.Million -> R.string.CoinPage_MarketCap_Million
                    NumberSuffix.Billion -> R.string.CoinPage_MarketCap_Billion
                    NumberSuffix.Trillion -> R.string.CoinPage_MarketCap_Trillion
                }

                formattedNumber + suffixResId?.let {
                    " " + Translator.getString(it)
                }
            }
            is BigDecimalRounded.LessThen -> {
                "<$formattedNumber"
            }
            is BigDecimalRounded.Regular -> {
                formattedNumber
            }
        }
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

    override fun formatCurrencyValueAsShortened(currencyValue: CurrencyValue): String {
        return formatFiatShort(currencyValue.value, currencyValue.currency.symbol, 2)
    }

    override fun formatValueAsDiff(value: Value): String =
        when (value) {
            is Value.Currency -> {
                val currencyValue = value.currencyValue
                formatFiatShort(currencyValue.value, currencyValue.currency.symbol, currencyValue.currency.decimal)
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

data class BigDecimalShortened(
    val value: BigDecimal,
    val suffix: Suffix
) {
    enum class Suffix(val groupCount: Int, val titleResId: Int?) {
        Blank(0, null),
        Thousand(1, R.string.CoinPage_MarketCap_Thousand),
        Million(2, R.string.CoinPage_MarketCap_Million),
        Billion(3, R.string.CoinPage_MarketCap_Billion),
        Trillion(4, R.string.CoinPage_MarketCap_Trillion);

        companion object {
            private val valuesByGroupCount = values().associateBy(Suffix::groupCount)

            fun getByGroupCount(groupCount: Int): Suffix? {
                return valuesByGroupCount[groupCount]
            }
        }
    }
}
