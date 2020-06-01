package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.core.ILanguageManager
import java.lang.UnsupportedOperationException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*
import kotlin.math.pow

class NumberFormatter(private val languageManager: ILanguageManager) : IAppNumberFormatter {

    private var formatters: MutableMap<String, NumberFormat> = mutableMapOf()

    override fun format(value: Number, minimumFractionDigits: Int, maximumFractionDigits: Int, prefix: String, suffix: String): String {
        val bigDecimalValue = when (value) {
            is Double -> BigDecimal(value)
            is Float -> value.toBigDecimal()
            is BigDecimal -> value
            else -> throw UnsupportedOperationException()
        }

        val formatter = getFormatter(languageManager.currentLocale) ?: throw Exception("No formatter")

        formatter.minimumFractionDigits = minimumFractionDigits
        formatter.maximumFractionDigits = maximumFractionDigits

        val mostLowValue = 10.0.pow(-maximumFractionDigits).toBigDecimal()

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
        return format(value, minimumFractionDigits, maximumFractionDigits, prefix = symbol)
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
