package io.horizontalsystems.bankwallet.core.managers

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.ILanguageManager
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*


class NumberFormatter(private val languageManager: ILanguageManager) : IAppNumberFormatter {

    private val COIN_BIG_NUMBER_EDGE = "0.01".toBigDecimal()
    private val FIAT_BIG_NUMBER_EDGE = "1000".toBigDecimal()
    private val FIAT_SMALL_NUMBER_EDGE = "0.01".toBigDecimal()
    private val FIAT_TEN_CENT_EDGE = "0.1".toBigDecimal()

    private var formatters: MutableMap<String, NumberFormat> = mutableMapOf()

    override fun format(coinValue: CoinValue, explicitSign: Boolean, realNumber: Boolean): String? {
        var value = coinValue.value.abs()

        val customFormatter = getFormatter(languageManager.currentLanguage) ?: return null

        when {
            !realNumber && value > COIN_BIG_NUMBER_EDGE -> customFormatter.maximumFractionDigits = 4
            value.compareTo(BigDecimal.ZERO) == 0 -> customFormatter.maximumFractionDigits = 0
            else -> customFormatter.maximumFractionDigits = 8
        }
        value = value.stripTrailingZeros()
        val formatted = customFormatter.format(value)
        var result = "$formatted ${coinValue.coinCode}"

        if (explicitSign && coinValue.value.toLong() != 0L) {
            val sign = if (coinValue.value < BigDecimal.ZERO) "-" else "+"
            result = "$sign $result"
        }

        return result
    }

    override fun formatForTransactions(coinValue: CoinValue): String? {
        var formatted = format(coinValue)
        if (coinValue.value < BigDecimal.ZERO) {
            formatted = "- $formatted"
        }
        return formatted
    }

    override fun format(currencyValue: CurrencyValue, showNegativeSign: Boolean, trimmable: Boolean, canUseLessSymbol: Boolean): String? {

        val absValue = currencyValue.value.abs()
        var value = absValue

        val customFormatter = getFormatter(languageManager.currentLanguage) ?: return null

        when {
            value.compareTo(BigDecimal.ZERO) == 0 -> {
                value = BigDecimal.ZERO
                customFormatter.minimumFractionDigits = if (trimmable) 0 else 2
            }
            value < FIAT_TEN_CENT_EDGE && !canUseLessSymbol -> {
                customFormatter.maximumFractionDigits = 4
            }
            value < FIAT_SMALL_NUMBER_EDGE -> {
                value = BigDecimal("0.01")
                customFormatter.maximumFractionDigits = 2
            }
            else -> {
                when {
                    trimmable && (value >= FIAT_BIG_NUMBER_EDGE) -> {
                        customFormatter.maximumFractionDigits = 0
                    }
                    else -> {
                        customFormatter.maximumFractionDigits = 2
                    }
                }
            }
        }

        val formatted = customFormatter.format(value)

        var result = "${currencyValue.currency.symbol}$formatted"

        if (canUseLessSymbol && absValue <= FIAT_SMALL_NUMBER_EDGE && absValue > BigDecimal.ZERO) {
            result = "< $result"
        }

        if (showNegativeSign && currencyValue.value < BigDecimal.ZERO) {
            result = "- $result"
        }

        return result
    }

    override fun formatForTransactions(currencyValue: CurrencyValue, isIncoming: Boolean): SpannableString {
        val spannable = SpannableString(format(currencyValue, showNegativeSign = true, trimmable = true, canUseLessSymbol = true))

        //set color
        val amountTextColor = if (isIncoming) R.color.green_crypto else R.color.yellow_crypto
        val color = ContextCompat.getColor(App.instance, amountTextColor)

        spannable.setSpan(ForegroundColorSpan(color), 0, spannable.length, 0)
        return spannable
    }

    override fun format(value: Double): String {
        val customFormatter = getFormatter(languageManager.currentLanguage)
        customFormatter?.maximumFractionDigits = 8
        if (value == 0.0) {
            customFormatter?.maximumFractionDigits = 0
        }

        return customFormatter?.format(value) ?: "0"
    }

    private fun getFormatter(locale: Locale): NumberFormat? {
        return formatters[locale.language] ?: run {
            val newFormatter = NumberFormat.getInstance(locale)
            newFormatter.roundingMode = RoundingMode.HALF_EVEN
            formatters[locale.language] = newFormatter
            return newFormatter
        }
    }

}
