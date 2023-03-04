package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.market.Value
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NumberFormatter(
        private val languageManager: LanguageManager
        ) : IAppNumberFormatter {

    private var formatters = ConcurrentHashMap<String, NumberFormat>()
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

    override fun formatCoinFull(value: BigDecimal, code: String?, coinDecimals: Int): String {
        val rounded = numberRounding.getRoundedCoinFull(value, coinDecimals)
        return formatRounded(rounded = rounded, prefix = null, suffix = code?.let { " $it" })
    }

    override fun formatCoinShort(value: BigDecimal, code: String?, coinDecimals: Int): String {
        val rounded = numberRounding.getRoundedCoinShort(value, coinDecimals)
        return formatRounded(rounded = rounded, prefix = null, suffix = code?.let { " $it" })
    }

    override fun formatNumberShort(value: BigDecimal, maximumFractionDigits: Int): String {
        val rounded = numberRounding.getRoundedShort(value, maximumFractionDigits)
        return formatRounded(rounded = rounded, prefix = null, suffix = null)
    }

    override fun formatFiatFull(value: BigDecimal, symbol: String): String {
        val rounded = numberRounding.getRoundedCurrencyFull(value)
        return formatRounded(rounded = rounded, prefix = symbol, suffix = null)
    }

    override fun formatFiatShort(
        value: BigDecimal,
        symbol: String,
        currencyDecimals: Int
    ): String {
        val rounded = numberRounding.getRoundedCurrencyShort(value, currencyDecimals)
        return formatRounded(rounded = rounded, prefix = symbol, suffix = null)
    }

    private fun formatRounded(rounded: BigDecimalRounded, prefix: String?, suffix: String?): String {
        val formatter = getFormatter(languageManager.currentLocale, 0, Int.MAX_VALUE)
        var formattedNumber = formatter.format(rounded.value)

        prefix?.let {
            formattedNumber = "$prefix$formattedNumber"
        }

        if (rounded is BigDecimalRounded.LessThen) {
            formattedNumber = "< $formattedNumber"
        }

        when ((rounded as? BigDecimalRounded.Large)?.name) {
            LargeNumberName.Thousand -> R.string.CoinPage_MarketCap_Thousand
            LargeNumberName.Million -> R.string.CoinPage_MarketCap_Million
            LargeNumberName.Billion -> R.string.CoinPage_MarketCap_Billion
            LargeNumberName.Trillion -> R.string.CoinPage_MarketCap_Trillion
            LargeNumberName.Quadrillion -> R.string.CoinPage_MarketCap_Quadrillion
            else -> null
        }?.let {
            formattedNumber = Translator.getString(R.string.LargeNumberFormat, formattedNumber, Translator.getString(it))
        }

        suffix?.let {
            formattedNumber = "$formattedNumber$suffix"
        }

        return formattedNumber
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
