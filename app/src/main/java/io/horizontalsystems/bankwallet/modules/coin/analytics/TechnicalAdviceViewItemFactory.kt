package io.horizontalsystems.bankwallet.modules.coin.analytics

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.Analytics.TechnicalAdvice
import io.horizontalsystems.marketkit.models.Analytics.TechnicalAdvice.Advice
import java.math.BigDecimal
import java.util.Date

class TechnicalAdviceViewItemFactory(private val numberFormatter: IAppNumberFormatter) {

    fun advice(technicalAdvice: TechnicalAdvice): String {
        val mainAdvice = mainAdvice(technicalAdvice)
        val trendAdvice = trendAdvice(technicalAdvice)

        val combinedAdvice = buildString {
            append(mainAdvice)
            if (trendAdvice != null) {
                append("\n\n")
                append(trendAdvice)
            }
        }

        return combinedAdvice
    }

    private fun mainAdvice(technicalAdvice: TechnicalAdvice): String {
        val (overtype, direction, rsiLine) = when (technicalAdvice.advice ?: Advice.Neutral) {
            Advice.Oversold, Advice.StrongBuy, Advice.Buy -> Triple(
                Translator.getString(R.string.TechnicalAdvice_OverSold),
                Translator.getString(R.string.TechnicalAdvice_Down),
                "30%"
            )

            Advice.Overbought, Advice.StrongSell, Advice.Sell, Advice.Neutral -> Triple(
                Translator.getString(R.string.TechnicalAdvice_OverSold),
                Translator.getString(R.string.TechnicalAdvice_Down),
                "70%"
            )
        }

        val rsiValue = technicalAdvice.rsi?.let {
            numberFormatter.format(it, 0, 1, suffix = "%")
        }

        val signalTimeString = technicalAdvice.signalTimestamp?.let { timestamp ->
            val date = Date(timestamp * 1000)
            DateHelper.shortDate(date)
        }?.let { date ->
            Translator.getString(R.string.TechnicalAdvice_OverIndicators_SignalDate, date)
        }

        val adviceString = when (technicalAdvice.advice) {
            Advice.Oversold, Advice.Overbought -> {
                val advice = Translator.getString(R.string.TechnicalAdvice_OverMain)

                val rsi = rsiValue?.let { value ->
                    Translator.getString(R.string.TechnicalAdvice_OverRsi, value, overtype)
                } ?: ""

                val indicators = listOf(
                    Translator.getString(R.string.TechnicalAdvice_OverIndicators, overtype),
                    rsi
                ).joinToString(" ")

                val combinedIndicators = if (signalTimeString != null) {
                    listOf(
                        signalTimeString,
                        indicators,
                    ).joinToString(" ")
                } else {
                    indicators
                }

                val resultAdvice = Translator.getString(
                    R.string.TechnicalAdvice_OverAdvice,
                    direction
                )

                listOf(advice, combinedIndicators, resultAdvice).joinToString(" ")
            }

            Advice.StrongBuy, Advice.StrongSell -> {
                val rsi = rsiValue?.let {
                    Translator.getString(
                        R.string.TechnicalAdvice_StrongRsi,
                        it,
                        overtype
                    )
                } ?: ""

                val indicators = listOf(
                    Translator.getString(R.string.TechnicalAdvice_StrongIndicators, overtype),
                    rsi
                ).joinToString(" ")

                val combinedIndicators = if (signalTimeString != null) {
                    listOf(
                        signalTimeString,
                        indicators,
                    ).joinToString(" ")
                } else {
                    indicators
                }

                listOf(
                    combinedIndicators,
                    Translator.getString(R.string.TechnicalAdvice_StrongAdvice, direction)
                ).joinToString(" ")
            }

            Advice.Buy, Advice.Sell -> {
                val rsi = rsiValue?.let {
                    Translator.getString(
                        R.string.TechnicalAdvice_StableRsi,
                        it,
                        rsiLine
                    )
                } ?: ""

                val indicators = listOf(
                    Translator.getString(R.string.TechnicalAdvice_StrongIndicators, overtype),
                    rsi
                ).joinToString(" ")

                val combinedIndicators = if (signalTimeString != null) {
                    listOf(
                        signalTimeString,
                        indicators
                    ).joinToString(" ")
                } else {
                    indicators
                }

                listOf(
                    combinedIndicators,
                    Translator.getString(R.string.TechnicalAdvice_StrongAdvice, direction)
                ).joinToString(" ")
            }

            Advice.Neutral, null -> {
                val rsi =
                    rsiValue?.let { Translator.getString(R.string.TechnicalAdvice_StableRsi, it) }
                        ?: ""

                val indicators = listOf(
                    Translator.getString(R.string.TechnicalAdvice_NeutralIndicators, overtype),
                    rsi
                ).joinToString(" ")

                val combinedIndicators = if (signalTimeString != null) {
                    listOf(
                        signalTimeString,
                        indicators
                    ).joinToString(" ")
                } else {
                    indicators
                }

                listOf(
                    combinedIndicators,
                    Translator.getString(R.string.TechnicalAdvice_NeutralAdvice)
                ).joinToString(" ")
            }
        }

        return adviceString
    }

    private fun trendAdvice(technicalAdvice: TechnicalAdvice): String? {
        val price = technicalAdvice.price ?: return null

        val emaAdvice = technicalAdvice.ema?.let { ema ->
            val direction = Translator.getString(
                if (price >= ema) R.string.TechnicalAdvice_EmaAbove else R.string.TechnicalAdvice_EmaBelow
            )
            val action = Translator.getString(
                if (price >= ema) R.string.TechnicalAdvice_EmaGrowth else R.string.TechnicalAdvice_EmaDecrease
            )
            val emaValue = numberFormatter.format(ema, 0, 4)
            Translator.getString(R.string.TechnicalAdvice_EmaAdvice, direction, emaValue, action)
        }

        val macdAdvice = technicalAdvice.macd?.let { macd ->
            val direction = Translator.getString(
                if (macd >= BigDecimal.ZERO) R.string.TechnicalAdvice_MacdPositive else R.string.TechnicalAdvice_MacdNegative
            )
            val action =
                Translator.getString(if (macd >= BigDecimal.ZERO) R.string.TechnicalAdvice_Up else R.string.TechnicalAdvice_Down)
            val sign = if (macd >= BigDecimal.ZERO) "" else "-"
            val macdValue = numberFormatter.format(macd, 0, 4, prefix = sign)
            Translator.getString(R.string.TechnicalAdvice_MacdAdvice, direction, macdValue, action)
        }

        val advices = listOf(emaAdvice, macdAdvice).filterNotNull()
        if (advices.isEmpty()) {
            return null
        }

        return buildString {
            append(Translator.getString(R.string.TechnicalAdvice_OtherTitle))
            advices.forEach {
                append("\n\n")
                append(it)
            }
        }
    }
}

val Advice.title: String
    get() {
        return when (this) {
            Advice.Oversold -> Translator.getString(R.string.TechnicalAdvice_Indicators_Oversold)
            Advice.StrongBuy -> Translator.getString(R.string.TechnicalAdvice_Indicators_StrongBuy)
            Advice.Buy -> Translator.getString(R.string.TechnicalAdvice_Indicators_Buy)
            Advice.Neutral -> Translator.getString(R.string.TechnicalAdvice_Indicators_Neutral)
            Advice.Sell -> Translator.getString(R.string.TechnicalAdvice_Indicators_Sell)
            Advice.StrongSell -> Translator.getString(R.string.TechnicalAdvice_Indicators_StrongSell)
            Advice.Overbought -> Translator.getString(R.string.TechnicalAdvice_Indicators_Overbought)
        }
    }

val Advice.sliderIndex: Int
    get() {
        return when (this) {
            Advice.Oversold -> 0
            Advice.StrongBuy -> 3
            Advice.Buy -> 2
            Advice.Neutral -> 1
            Advice.Sell -> 2
            Advice.StrongSell -> 3
            Advice.Overbought -> 0
        }
    }