package io.horizontalsystems.bankwallet.viewHelpers

import android.support.v4.content.ContextCompat
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*


object ValueFormatter {

    private val COIN_BIG_NUMBER_EDGE = "0.0001".toBigDecimal()
    private val FIAT_BIG_NUMBER_EDGE = "100".toBigDecimal()
    private val FIAT_SMALL_NUMBER_EDGE = "0.01".toBigDecimal()

    private var formatters: MutableMap<String, NumberFormat> = mutableMapOf()

    fun format(coinValue: CoinValue, explicitSign: Boolean = false, realNumber: Boolean = false): String? {
        var value = if (explicitSign) coinValue.value.abs() else coinValue.value

        value = when {
            !realNumber && value >= COIN_BIG_NUMBER_EDGE -> value.setScale(4, RoundingMode.HALF_EVEN)
            value.compareTo(BigDecimal.ZERO) == 0 -> value.setScale(0, RoundingMode.HALF_EVEN)
            else -> value.setScale(8, RoundingMode.HALF_EVEN)
        }
        value = value.stripTrailingZeros()

        val formatted = getFormatter(App.languageManager.currentLanguage)?.format(value)
                ?: return null

        var result = "$formatted ${coinValue.coinCode}"

        if (explicitSign) {
            val sign = if (coinValue.value < BigDecimal.ZERO) "-" else "+"
            result = "$sign $result"
        }

        return result
    }

    fun format(currencyValue: CurrencyValue, approximate: Boolean = false, showNegativeSign: Boolean = true, realNumber: Boolean = false): String? {
        var value = currencyValue.value

        value = value.abs()

        value = when {
            value.compareTo(BigDecimal.ZERO) == 0 -> BigDecimal.ZERO
            value < FIAT_SMALL_NUMBER_EDGE -> BigDecimal("0.01")
            else -> {
                value = when {
                    !realNumber && (value >= FIAT_BIG_NUMBER_EDGE || approximate) -> value.setScale(0, RoundingMode.HALF_EVEN)
                    else -> value.setScale(2, RoundingMode.HALF_EVEN)
                }
                value
            }
        }

        val formatted = getFormatter(App.languageManager.currentLanguage)?.format(value)
                ?: return null

        var result = "${currencyValue.currency.symbol} $formatted"

        if (showNegativeSign && currencyValue.value < BigDecimal.ZERO) {
            result = "- $result"
        }

        if (approximate) {
            result = "~ $result"
        }

        return result
    }

    fun formatForTransactions(currencyValue: CurrencyValue, isIncoming: Boolean): SpannableString {
        val spannable = SpannableString(format(currencyValue))

        //set currency sign size
        val endOffset = if (currencyValue.value < BigDecimal.ZERO) 3 else 1
        spannable.setSpan(RelativeSizeSpan(0.75f), 0, endOffset, 0)

        //set color
        val amountTextColor = if (isIncoming) R.color.green_crypto else R.color.yellow_crypto
        val color = ContextCompat.getColor(App.instance, amountTextColor)

        spannable.setSpan(ForegroundColorSpan(color), 0, spannable.length, 0)
        return spannable
    }

    fun format(value: Double): String {
        val customFormatter = getFormatter(App.languageManager.currentLanguage)
        customFormatter?.maximumFractionDigits = 8
        if (value == 0.0) {
            customFormatter?.maximumFractionDigits = 0
        }

        return customFormatter?.format(value) ?: "0"
    }

    private fun getFormatter(locale: Locale): NumberFormat? {
        return formatters[locale.language] ?: run {
            val newFormatter = NumberFormat.getInstance(locale)
            formatters[locale.language] = newFormatter
            return newFormatter
        }
    }

}
