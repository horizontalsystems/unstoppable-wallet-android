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
    private val suffixes = TreeMap<Long, String>().apply {
        put(1_000L, "k")
        put(1_000_000L, "M")
        put(1_000_000_000L, "B")
        put(1_000_000_000_000L, "T")
    }

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

    override fun format(currencyValue: CurrencyValue, showNegativeSign: Boolean, trimmable: Boolean, canUseLessSymbol: Boolean, shorten: Boolean): String? {

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
            value >= FIAT_BIG_NUMBER_EDGE && trimmable -> {
                customFormatter.maximumFractionDigits = 0
            }
            else -> {
                customFormatter.maximumFractionDigits = 2
            }
        }

        //  shorten will convert 100_000 to 100 K
        val formatted = if (shorten && value > BigDecimal("100000"))
            shortenNumber(value.toLong()) else
            customFormatter.format(value)

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

        //  set color
        val amountTextColor = if (isIncoming) R.color.green_crypto else R.color.yellow_crypto
        val color = ContextCompat.getColor(App.instance, amountTextColor)

        spannable.setSpan(ForegroundColorSpan(color), 0, spannable.length, 0)
        return spannable
    }

    override fun format(value: Double): String {
        val customFormatter = getFormatter(languageManager.currentLanguage)?.also {
            it.maximumFractionDigits = 8
        }

        if (value == 0.0) {
            customFormatter?.maximumFractionDigits = 0
        }

        return customFormatter?.format(value) ?: "0"
    }

    override fun shortenNumber(value: Long): String {
        if (value == Long.MIN_VALUE) return shortenNumber(Long.MIN_VALUE + 1)
        if (value < 0) return "-" + shortenNumber(-value)
        if (value < 1000) return value.toString() //deal with easy case

        val entry = suffixes.floorEntry(value)
        val divideBy: Long = entry.key
        val suffix: String = entry.value

        val truncated = value / (divideBy / 10) //the number part of the output times 10
        val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()

        return if (hasDecimal) "${truncated / 10.0} $suffix" else "${truncated / 10} $suffix"
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
