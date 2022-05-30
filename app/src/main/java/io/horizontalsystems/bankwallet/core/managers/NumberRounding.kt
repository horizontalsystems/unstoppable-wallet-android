package io.horizontalsystems.bankwallet.core.managers

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.log10
import kotlin.math.pow

class NumberRounding {

    fun getRoundedFull(value: BigDecimal, minimumFractionDigits: Int, maximumFractionDigits: Int): BigDecimalRounded {
        val mostLowValue = BigDecimal(BigInteger.ONE, maximumFractionDigits)

        return when {
            value < mostLowValue -> {
                BigDecimalRounded.Regular(BigDecimal.ZERO)
            }
            else -> {
                BigDecimalRounded.Regular(simpleRoundingStrategy(value, minimumFractionDigits, maximumFractionDigits))
            }
        }
    }

    fun getRoundedShort(value: BigDecimal, maximumFractionDigits: Int): BigDecimalRounded {
        val maximumFractionDigitsCoerced = maximumFractionDigits.coerceAtMost(8)
        val mostLowValue = BigDecimal(BigInteger.ONE, maximumFractionDigitsCoerced)

        return when {
            value.compareTo(BigDecimal.ZERO) == 0 -> {
                BigDecimalRounded.Regular(BigDecimal.ZERO)
            }
            value < mostLowValue -> {
                BigDecimalRounded.LessThen(mostLowValue)
            }
            value < BigDecimal("19999.5") -> {
                BigDecimalRounded.Regular(simpleRoundingStrategy(value, 0, maximumFractionDigitsCoerced))
            }
            else -> {
                largeNumberStrategy(value)
            }
        }
    }

    fun getRoundedShort(value: BigDecimal): BigDecimalRounded {
        return getRoundedShort(value, 8)
    }

    fun getRoundedCoinShort(value: BigDecimal, coinDecimals: Int): BigDecimalRounded {
        return getRoundedShort(value, coinDecimals)
    }

    fun getRoundedCoinFull(value: BigDecimal, coinDecimals: Int): BigDecimalRounded {
        return getRoundedFull(value, 4.coerceAtMost(coinDecimals), coinDecimals)
    }

    fun getRoundedCurrencyShort(value: BigDecimal, currencyDecimals: Int): BigDecimalRounded {
        return getRoundedShort(value, currencyDecimals)
    }

    fun getRoundedCurrencyFull(value: BigDecimal): BigDecimalRounded {
        return getRoundedFull(value, 0, 18)
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
    ): BigDecimal {
        val decimals = when {
            value < BigDecimal("1") -> getNumberOfZerosAfterDot(value) + 4
            value < BigDecimal("1.01") -> 4
            value < BigDecimal("1.1") -> 3
            value < BigDecimal("20") -> 2
            value < BigDecimal("200") -> 1
            else -> 0
        }

        val coerced = decimals.coerceIn(minimumFractionDigits, maximumFractionDigits)

        return value.setScale(coerced, RoundingMode.HALF_UP).stripTrailingZeros()
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
            1 -> LargeNumberName.Thousand
            2 -> LargeNumberName.Million
            3 -> LargeNumberName.Billion
            4 -> LargeNumberName.Trillion
            5 -> LargeNumberName.Quadrillion
            else -> null
        }

        return when (suffix) {
            null -> BigDecimalRounded.Large(value, LargeNumberName.None)
            else -> {
                val t = groupCount * 3
                val shortened = value.divide(BigDecimal(10.0.pow(t.toDouble())))
                if (shortened >= BigDecimal("999.5")) {
                    shortByGroupCount(groupCount + 1, value)
                } else {
                    BigDecimalRounded.Large(shortened, suffix)
                }
            }
        }
    }
}

sealed class BigDecimalRounded {
    abstract val value: BigDecimal

    data class LessThen(override val value: BigDecimal) : BigDecimalRounded()
    data class Regular(override val value: BigDecimal) : BigDecimalRounded()
    data class Large(override val value: BigDecimal, val name: LargeNumberName) : BigDecimalRounded()
}

enum class LargeNumberName {
    None,
    Thousand,
    Million,
    Billion,
    Trillion,
    Quadrillion;
}
