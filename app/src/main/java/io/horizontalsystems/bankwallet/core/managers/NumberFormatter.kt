package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.core.ILanguageManager
import java.lang.UnsupportedOperationException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

class NumberFormatter(private val languageManager: ILanguageManager) : IAppNumberFormatter {

    private var formatters: MutableMap<String, NumberFormat> = mutableMapOf()

    override fun formatFiat(value: BigDecimal, symbol: String, minimumFractionDigits: Int, maximumFractionDigits: Int): String {
        val formatter = getFormatter(languageManager.currentLocale) ?: throw Exception("No formatter")

        formatter.minimumFractionDigits = minimumFractionDigits
        formatter.maximumFractionDigits = maximumFractionDigits

        val mostLowValue = 10.0.pow(-maximumFractionDigits).toBigDecimal()

        return if (value > BigDecimal.ZERO && value < mostLowValue) {
            "< " + symbol + formatter.format(mostLowValue)
        } else {
            symbol + formatter.format(value)
        }
    }

    override fun getSignificantDecimal(value: BigDecimal): Int {
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

    override fun formatSimple(value: Number, minimumFractionDigits: Int, maximumFractionDigits: Int): String {
        val bigDecimalValue = when (value) {
            is Double -> BigDecimal(value)
            is BigDecimal -> value
            else -> throw UnsupportedOperationException()
        }

        val formatter = getFormatter(languageManager.currentLocale) ?: throw Exception("No formatter")

        formatter.minimumFractionDigits = minimumFractionDigits
        formatter.maximumFractionDigits = maximumFractionDigits

        val mostLowValue = 10.0.pow(-maximumFractionDigits).toBigDecimal()

        return if (bigDecimalValue > BigDecimal.ZERO && bigDecimalValue < mostLowValue) {
            "< " + formatter.format(mostLowValue)
        } else {
            formatter.format(bigDecimalValue)
        }
    }

    override fun formatCoin(value: Number, code: String, minimumFractionDigits: Int, maximumFractionDigits: Int): String {
        val bigDecimalValue = when (value) {
            is Double -> BigDecimal(value)
            is BigDecimal -> value
            else -> throw UnsupportedOperationException()
        }

        val formatter = getFormatter(languageManager.currentLocale) ?: throw Exception("No formatter")

        formatter.minimumFractionDigits = minimumFractionDigits
        formatter.maximumFractionDigits = maximumFractionDigits

        val mostLowValue = 10.0.pow(-maximumFractionDigits).toBigDecimal()

        return if (bigDecimalValue > BigDecimal.ZERO && bigDecimalValue < mostLowValue) {
            "< " + formatter.format(mostLowValue) + " " + code
        } else {
            formatter.format(bigDecimalValue) + " " + code
        }
    }

    private fun getFormatter(locale: Locale): NumberFormat? {
        if (formatters[locale.language] == null) {
            val newFormatter = NumberFormat.getInstance(locale).apply {
                roundingMode = RoundingMode.HALF_UP
            }

            formatters[locale.language] = newFormatter
        }

        return formatters[locale.language]?.apply {
            // reset
            this.minimumFractionDigits = 0
            this.maximumFractionDigits = 3
        }
    }
}
