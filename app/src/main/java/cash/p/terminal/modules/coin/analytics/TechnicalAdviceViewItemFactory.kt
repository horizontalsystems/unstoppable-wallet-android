package cash.p.terminal.modules.coin.analytics

import cash.p.terminal.R
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.helpers.DateHelper
import cash.p.terminal.wallet.models.Analytics.TechnicalAdvice
import cash.p.terminal.wallet.models.Analytics.TechnicalAdvice.Advice
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
                cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_OverSold),
                cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_Down),
                "30%"
            )

            Advice.Overbought, Advice.StrongSell, Advice.Sell, Advice.Neutral -> Triple(
                cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_OverBought),
                cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_Up),
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
            cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_OverIndicators_SignalDate, date)
        }

        val adviceString = when (technicalAdvice.advice) {
            Advice.Oversold, Advice.Overbought -> {
                val advice = cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_OverMain)

                val rsi = rsiValue?.let { value ->
                    cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_OverRsi, value, overtype)
                } ?: ""

                val indicators = listOf(
                    cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_OverIndicators, overtype),
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

                val resultAdvice = cash.p.terminal.strings.helpers.Translator.getString(
                    R.string.TechnicalAdvice_OverAdvice,
                    direction
                )

                listOf(advice, combinedIndicators, resultAdvice).joinToString(" ")
            }

            Advice.StrongBuy, Advice.StrongSell -> {
                val rsi = rsiValue?.let {
                    cash.p.terminal.strings.helpers.Translator.getString(
                        R.string.TechnicalAdvice_StrongRsi,
                        it,
                        overtype
                    )
                } ?: ""

                val indicators = listOf(
                    cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_StrongIndicators, overtype),
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
                    cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_StrongAdvice, direction)
                ).joinToString(" ")
            }

            Advice.Buy, Advice.Sell -> {
                val rsi = rsiValue?.let {
                    cash.p.terminal.strings.helpers.Translator.getString(
                        R.string.TechnicalAdvice_StableRsi,
                        it,
                        rsiLine
                    )
                } ?: ""

                val indicators = listOf(
                    cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_StrongIndicators, overtype),
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
                    cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_StrongAdvice, direction)
                ).joinToString(" ")
            }

            Advice.Neutral, null -> {
                val rsi =
                    rsiValue?.let { cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_StableRsi, it) }
                        ?: ""

                val indicators = listOf(
                    cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_NeutralIndicators, overtype),
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
                    cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_NeutralAdvice)
                ).joinToString(" ")
            }
        }

        return adviceString
    }

    private fun trendAdvice(technicalAdvice: TechnicalAdvice): String? {
        val price = technicalAdvice.price ?: return null

        val emaAdvice = technicalAdvice.ema?.let { ema ->
            val direction = cash.p.terminal.strings.helpers.Translator.getString(
                if (price >= ema) R.string.TechnicalAdvice_EmaAbove else R.string.TechnicalAdvice_EmaBelow
            )
            val action = cash.p.terminal.strings.helpers.Translator.getString(
                if (price >= ema) R.string.TechnicalAdvice_EmaGrowth else R.string.TechnicalAdvice_EmaDecrease
            )
            val emaValue = numberFormatter.format(ema, 0, 4)
            cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_EmaAdvice, direction, emaValue, action)
        }

        val macdAdvice = technicalAdvice.macd?.let { macd ->
            val direction = cash.p.terminal.strings.helpers.Translator.getString(
                if (macd >= BigDecimal.ZERO) R.string.TechnicalAdvice_MacdPositive else R.string.TechnicalAdvice_MacdNegative
            )
            val action =
                cash.p.terminal.strings.helpers.Translator.getString(if (macd >= BigDecimal.ZERO) R.string.TechnicalAdvice_Up else R.string.TechnicalAdvice_Down)
            val sign = if (macd >= BigDecimal.ZERO) "" else "-"
            val macdValue = numberFormatter.format(macd, 0, 4, prefix = sign)
            cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_MacdAdvice, direction, macdValue, action)
        }

        val advices = listOf(emaAdvice, macdAdvice).filterNotNull()
        if (advices.isEmpty()) {
            return null
        }

        return buildString {
            append(cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_OtherTitle))
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
            Advice.Oversold -> cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_Indicators_RiskyToTrade)
            Advice.StrongSell -> cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_Indicators_StrongSell)
            Advice.Sell -> cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_Indicators_Sell)
            Advice.Neutral -> cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_Indicators_Neutral)
            Advice.Buy -> cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_Indicators_Buy)
            Advice.StrongBuy -> cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_Indicators_StrongBuy)
            Advice.Overbought -> cash.p.terminal.strings.helpers.Translator.getString(R.string.TechnicalAdvice_Indicators_RiskyToTrade)
        }
    }

val Advice.sliderIndex: Int
    get() {
        return when (this) {
            Advice.Oversold -> 0
            Advice.StrongSell -> 3
            Advice.Sell -> 2
            Advice.Neutral -> 1
            Advice.Buy -> 2
            Advice.StrongBuy -> 3
            Advice.Overbought -> 0
        }
    }