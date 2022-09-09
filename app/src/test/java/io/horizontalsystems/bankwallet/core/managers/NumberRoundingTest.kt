package io.horizontalsystems.bankwallet.core.managers

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class NumberRoundingTest {
    private val numberRounding = NumberRounding()

    @Test
    fun getShort_zero() {
        assertShortRegular("0", "0")
    }

    @Test
    fun getShort_lessThenMinimum_lessThenSign() {
        assertLessThen("0.000000001", "0.00000001")
    }

    @Test
    fun getShort_lessThen1_show4SignificantDecimals() {
        assertShortRegular("0.123456789", "0.1235")
        assertShortRegular("0.012345678", "0.01235")
        assertShortRegular("0.001234567", "0.001235")
        assertShortRegular("0.000123456", "0.0001235")
        assertShortRegular("0.000012345", "0.00001235")
        assertShortRegular("0.000001234", "0.00000123")
        assertShortRegular("0.999999999", "1")

        // noTrailingZeros
        assertShortRegular("0.10001", "0.1")
    }

    @Test
    fun getShort_range_show4Decimals() {
        // 1 until 1.01 show 4 decimals

        assertShortRegular("1", "1")
        assertShortRegular("1.00123456789", "1.0012")
        assertShortRegular("1.00994", "1.0099")
        assertShortRegular("1.00995", "1.01")
    }

    @Test
    fun getShort_range_show3Decimals() {
        // 1.01 until 1.1 show 3 decimals

        assertShortRegular("1.01", "1.01")
        assertShortRegular("1.0123456789", "1.012")
        assertShortRegular("1.0994", "1.099")
        assertShortRegular("1.0995", "1.1")
    }

    @Test
    fun getShort_range_show2Decimals() {
        // 1.1 until 20 show 2 decimals

        assertShortRegular("1.1", "1.1")
        assertShortRegular("1.123456789", "1.12")
        assertShortRegular("19.994", "19.99")
        assertShortRegular("19.995", "20")
    }

    @Test
    fun getShort_range_show1Decimals() {
        // 20 until 200 show 1 decimals

        assertShortRegular("20", "20")
        assertShortRegular("20.123456789", "20.1")
        assertShortRegular("20.456789", "20.5")
        assertShortRegular("199.94", "199.9")
        assertShortRegular("199.95", "200")
    }

    @Test
    fun getShort_range_showNoDecimals() {
        // 200 until 20000 show 0 decimals

        assertShortRegular("200", "200")
        assertShortRegular("200.1", "200")
        assertShortRegular("200.5", "201")

        assertShortRegular("19999.1", "19999")
        assertShortLarge("19999.5", "20", LargeNumberName.Thousand)
    }

    @Test
    fun getShort_largeNumber_range_show1Decimals() {
        // 20k until 200k show 1 decimals

        assertShortLarge("20000", "20", LargeNumberName.Thousand)
        assertShortLarge("20123", "20.1", LargeNumberName.Thousand)
        assertShortLarge("20350", "20.4", LargeNumberName.Thousand)
        assertShortLarge("199555", "199.6", LargeNumberName.Thousand)
        assertShortLarge("199950", "200", LargeNumberName.Thousand)
    }

    @Test
    fun getShort_largeNumber_range_showNoDecimals() {
        // 200k until 1M show 0 decimals

        assertShortLarge("200000", "200", LargeNumberName.Thousand)
        assertShortLarge("998500", "999", LargeNumberName.Thousand)
        assertShortLarge("999499", "999", LargeNumberName.Thousand)
        assertShortLarge("999500", "1", LargeNumberName.Million)
    }

    @Test
    fun getShort_largeNumber_range_show2Decimals() {
        // 1M until 20M show 2 decimals

        assertShortLarge("1234567", "1.23", LargeNumberName.Million)
        assertShortLarge("19123456", "19.12", LargeNumberName.Million)
        assertShortLarge("19995000", "20", LargeNumberName.Million)
    }

    private fun assertLessThen(value: String, expectedValue: String, ) {
        val actual = numberRounding.getRoundedShort(BigDecimal(value), 8) as BigDecimalRounded.LessThen
        assertEquals(expectedValue, actual.value.toPlainString())
    }

    private fun assertShortLarge(
        value: String,
        expectedValue: String,
        expectedSuffix: LargeNumberName
    ) {
        val actual = numberRounding.getRoundedShort(BigDecimal(value), 8) as BigDecimalRounded.Large

        assertEquals(expectedValue, actual.value.toPlainString())
        assertEquals(expectedSuffix, actual.name)
    }

    private fun assertShortRegular(
        value: String,
        expectedValue: String,
    ) {
        val actual = numberRounding.getRoundedShort(BigDecimal(value), 8) as BigDecimalRounded.Regular

        assertEquals(expectedValue, actual.value.toPlainString())
    }
}