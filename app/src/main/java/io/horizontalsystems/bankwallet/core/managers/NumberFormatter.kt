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

class NumberFormatter(private val languageManager: ILanguageManager) : IAppNumberFormatter {

    private val COIN_BIG_NUMBER_EDGE = "0.01".toBigDecimal()
    private val FIAT_BIG_NUMBER_EDGE = "1000".toBigDecimal()
    private val FIAT_SMALL_NUMBER_EDGE = "0.01".toBigDecimal()
    private val FIAT_TEN_CENT_EDGE = "0.1".toBigDecimal()

    private var formatters: MutableMap<String, NumberFormat> = mutableMapOf()

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

    override fun format(currencyValue: CurrencyValue, trimmable: Boolean, canUseLessSymbol: Boolean): String? {
        var value = currencyValue.value

        val customFormatter = getFormatter(languageManager.currentLocale) ?: return null

        when {
            value.compareTo(BigDecimal.ZERO) == 0 -> {
                value = BigDecimal.ZERO
                customFormatter.minimumFractionDigits = if (trimmable) 0 else 2
            }
            value < FIAT_SMALL_NUMBER_EDGE -> {
                value = BigDecimal("0.01")
                customFormatter.maximumFractionDigits = 2
            }
            value >= FIAT_BIG_NUMBER_EDGE && trimmable -> {
                customFormatter.maximumFractionDigits = 0
            }
            else -> {
                customFormatter.maximumFractionDigits = 2
            }
        }

        val formatted = customFormatter.format(value)

        var result = "${currencyValue.currency.symbol}$formatted"

        if (canUseLessSymbol && currencyValue.value <= FIAT_SMALL_NUMBER_EDGE && currencyValue.value > BigDecimal.ZERO) {
            result = "< $result"
        }

        return result
    }

    override fun formatForRates(currencyValue: CurrencyValue, trimmable: Boolean, maxFraction: Int?): String? {
        val value = currencyValue.value.abs()

        val customFormatter = getFormatter(languageManager.currentLocale) ?: return null

        when {
            maxFraction != null -> customFormatter.maximumFractionDigits = maxFraction
            value.compareTo(BigDecimal.ZERO) == 0 -> customFormatter.minimumFractionDigits = if (trimmable) 0 else 2
            else -> {
                val significantDecimalCount: Int = getSignificantDecimal(value, maxDecimal = 8)
                customFormatter.maximumFractionDigits = significantDecimalCount
            }
        }

        val formatted = customFormatter.format(value)

        return "${currencyValue.currency.symbol}$formatted"
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

    private fun getSignificantDecimal(value: BigDecimal, maxDecimal: Int): Int {
        //Here 4 numbers is significant value
        val ten = 10.toBigDecimal()
        val threshold = 1000.toBigDecimal()

        for (decimalCount in 0 until maxDecimal) {
            if (value * ten.pow(decimalCount) >= threshold) {
                return decimalCount
            }
        }
        return maxDecimal
    }

    private fun getFormatter(locale: Locale): NumberFormat? {
        return formatters[locale.language] ?: run {
            val newFormatter = NumberFormat.getInstance(locale).apply {
                roundingMode = RoundingMode.HALF_EVEN
            }
            formatters[locale.language] = newFormatter
            return newFormatter
        }
    }
}
