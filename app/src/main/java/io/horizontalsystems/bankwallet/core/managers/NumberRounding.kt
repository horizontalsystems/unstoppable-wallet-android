package io.horizontalsystems.bankwallet.core.managers

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.log10
import kotlin.math.pow

class NumberRounding {

    fun getRoundedFull(value: BigDecimal, minimumFractionDigits: Int, maximumFractionDigits: Int): BigDecimalRounded {
        val mostLowValue = BigDecimal(BigInteger.ONE, maximumFractionDigits)

        return if (value < mostLowValue) {
            BigDecimalRounded.LessThen(mostLowValue)
        } else {
            simpleRoundingStrategy(value, minimumFractionDigits, maximumFractionDigits)
        }
    }

    fun getRoundedShort(value: BigDecimal, maximumFractionDigits: Int): BigDecimalRounded {
        val maximumFractionDigitsCoerced = maximumFractionDigits.coerceAtMost(8)
        val mostLowValue = BigDecimal(BigInteger.ONE, maximumFractionDigitsCoerced)

        return when {
            value < mostLowValue -> {
                BigDecimalRounded.LessThen(mostLowValue)
            }
            value < BigDecimal("19999.5") -> {
                simpleRoundingStrategy(value, 0, maximumFractionDigitsCoerced)
            }
            else -> {
                largeNumberStrategy(value)
            }
        }
    }

    private fun largeNumberStrategy(value: BigDecimal): BigDecimalRounded.Large {
        val shortened = getShortened(value)
        val shortenedValue = shortened.value
        return when {
            shortenedValue < BigDecimal("20") -> {
                val rounded = shortenedValue.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
                shortened.copy(value = rounded)
            }
            shortenedValue < BigDecimal("200") -> {
                val rounded = shortenedValue.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros()
                shortened.copy(value = rounded)
            }
            else -> {
                val rounded = shortenedValue.setScale(0, RoundingMode.HALF_UP).stripTrailingZeros()
                shortened.copy(value = rounded)
            }
        }
    }

    private fun simpleRoundingStrategy(
        value: BigDecimal,
        minimumFractionDigits: Int,
        maximumFractionDigits: Int
    ): BigDecimalRounded.Regular {
        val decimals = when {
            value < BigDecimal("1") -> getNumberOfZerosAfterDot(value) + 4
            value < BigDecimal("1.01") -> 4
            value < BigDecimal("1.1") -> 3
            value < BigDecimal("20") -> 2
            value < BigDecimal("200") -> 1
            else -> 0
        }

        val coerced = decimals.coerceIn(minimumFractionDigits, maximumFractionDigits)

        val rounded = value.setScale(coerced, RoundingMode.HALF_UP).stripTrailingZeros()
        return BigDecimalRounded.Regular(rounded)
    }

    private fun getShortened(value: BigDecimal): BigDecimalRounded.Large {
        val base = log10(value.toDouble()).toInt() + 1
        val groupCount = (base - 1) / 3

        return shortByGroupCount(groupCount, value)
    }

    private fun getNumberOfZerosAfterDot(value: BigDecimal): Int {
        return value.scale() - value.precision()
    }

    private fun shortByGroupCount(
        groupCount: Int,
        value: BigDecimal
    ): BigDecimalRounded.Large {
        val suffix = when (groupCount) {
            1 -> NumberSuffix.Thousand
            2 -> NumberSuffix.Million
            3 -> NumberSuffix.Billion
            4 -> NumberSuffix.Trillion
            else -> null
        }

        return when (suffix) {
            null -> BigDecimalRounded.Large(value, NumberSuffix.Blank)
            else -> {
                val t = groupCount * 3
                val shortened = value.divide(BigDecimal(10.0.pow(t.toDouble())))
                if (shortened >= BigDecimal("999")) {
                    shortByGroupCount(groupCount + 1, value)
                } else {
                    BigDecimalRounded.Large(shortened, suffix)
                }
            }
        }
    }
}

sealed class BigDecimalRounded {
    data class LessThen(val value: BigDecimal) : BigDecimalRounded()
    data class Regular(val value: BigDecimal) : BigDecimalRounded()
    data class Large(val value: BigDecimal, val suffix: NumberSuffix) : BigDecimalRounded()
}

enum class NumberSuffix {
    Blank,
    Thousand,
    Million,
    Billion,
    Trillion;
}
