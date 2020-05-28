package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.core.ILanguageManager
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

class NumberFormatter(private val languageManager: ILanguageManager) : IAppNumberFormatter {

    private val COIN_BIG_NUMBER_EDGE = "0.01".toBigDecimal()
    private val FIAT_BIG_NUMBER_EDGE = "1000".toBigDecimal()
    private val FIAT_SMALL_NUMBER_EDGE = "0.01".toBigDecimal()
    private val FIAT_TEN_CENT_EDGE = "0.1".toBigDecimal()

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

    override fun format(coinValue: CoinValue, realNumber: Boolean): String? {
        return format(coinValue.value, coinValue.coin.code, realNumber)
    }

    override fun format(value: BigDecimal, coinCode: String, realNumber: Boolean): String? {
        val customFormatter = getFormatter(languageManager.currentLocale) ?: return null

        when {
            !realNumber && value > COIN_BIG_NUMBER_EDGE -> customFormatter.maximumFractionDigits = 4
            else -> customFormatter.maximumFractionDigits = 8
        }
        val formatted = customFormatter.format(value)

        return "$formatted $coinCode"
    }


    override fun format(value: Double, precision: Int): String {
        val customFormatter = getFormatter(languageManager.currentLocale)?.also {
            it.maximumFractionDigits = precision
        }

        return customFormatter?.format(abs(value)) ?: "0"
    }

    override fun format(value: BigDecimal, precision: Int): String? {
        val numberFormat = getFormatter(languageManager.currentLocale)?.apply {
            maximumFractionDigits = precision
        }

        return numberFormat?.format(value)
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
