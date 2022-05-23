package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.core.ILanguageManager
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.*

class NumberFormatterTest {
    private val languageManager = mock<ILanguageManager>()
    private val numberFormatter = NumberFormatter(languageManager)

    @Before
    fun setup() {
        whenever(languageManager.currentLocale).thenReturn(Locale.US)
    }

    @Test
    fun testShortenedForTxs() {
        assertShortenedForTxs("0", "0", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.00000000", "0", BigDecimalShortened.Suffix.Blank)

        assertShortenedForTxs("0.0000000000000001", "0.00000001", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0000000033333333", "0.00000001", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0000000055555555", "0.00000001", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0000000099999999", "0.00000001", BigDecimalShortened.Suffix.Blank)

        assertShortenedForTxs("0.00000001", "0.00000001", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0000000100000001", "0.00000001", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0000000133333333", "0.00000001", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0000000155555555", "0.00000002", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0000000199999999", "0.00000002", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0000333333333333", "0.00003333", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0000555555555555", "0.00005556", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0000999999999999", "0.0001", BigDecimalShortened.Suffix.Blank)

        assertShortenedForTxs("0.0001", "0.0001", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0001000000000001", "0.0001", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0001333333333333", "0.0001", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0001555555555555", "0.0002", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.0001999999999999", "0.0002", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.3333333333333333", "0.3333", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.5555555555555555", "0.5556", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("0.9999999999999999", "1", BigDecimalShortened.Suffix.Blank)

        assertShortenedForTxs("1", "1", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("1.0000000000000001", "1", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("3.3333333333333333", "3.33", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("5.5555555555555555", "5.56", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("9.9999999999999999", "10", BigDecimalShortened.Suffix.Blank)

        assertShortenedForTxs("10", "10", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("10.0000000000000001", "10", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("33.3333333333333333", "33.3", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("55.5555555555555555", "55.6", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("99.9999999999999999", "100", BigDecimalShortened.Suffix.Blank)

        assertShortenedForTxs("100", "100", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("100.0000000000000001", "100", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("333.3333333333333333", "333", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("555.5555555555555555", "556", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("999.9999999999999999", "1000", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("3333.3333333333333333", "3333", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("5555.5555555555555555", "5556", BigDecimalShortened.Suffix.Blank)
        assertShortenedForTxs("9999.9999999999999999", "10", BigDecimalShortened.Suffix.Thousand)

        assertShortenedForTxs("10000", "10", BigDecimalShortened.Suffix.Thousand)
        assertShortenedForTxs("10000.0000000000000001", "10", BigDecimalShortened.Suffix.Thousand)
        assertShortenedForTxs("33333.3333333333333333", "33.3", BigDecimalShortened.Suffix.Thousand)
        assertShortenedForTxs("55555.5555555555555555", "55.6", BigDecimalShortened.Suffix.Thousand)
        assertShortenedForTxs("99999.9999999999999999", "100", BigDecimalShortened.Suffix.Thousand)
        assertShortenedForTxs("333333.3333333333333333", "333", BigDecimalShortened.Suffix.Thousand)
        assertShortenedForTxs("555555.5555555555555555", "556", BigDecimalShortened.Suffix.Thousand)
        assertShortenedForTxs("999999.9999999999999999", "1", BigDecimalShortened.Suffix.Million)
        assertShortenedForTxs("999966", "1", BigDecimalShortened.Suffix.Million)

        assertShortenedForTxs("1000000", "1", BigDecimalShortened.Suffix.Million)
        assertShortenedForTxs("1000000.0000000000000001", "1", BigDecimalShortened.Suffix.Million)
        assertShortenedForTxs("3333333.3333333333333333", "3.33", BigDecimalShortened.Suffix.Million)
        assertShortenedForTxs("5555555.5555555555555555", "5.56", BigDecimalShortened.Suffix.Million)
        assertShortenedForTxs("9999999.9999999999999999", "10", BigDecimalShortened.Suffix.Million)
        assertShortenedForTxs("33333333.333333333333333", "33.3", BigDecimalShortened.Suffix.Million)
        assertShortenedForTxs("55555555.555555555555555", "55.6", BigDecimalShortened.Suffix.Million)
        assertShortenedForTxs("99999999.999999999999999", "100", BigDecimalShortened.Suffix.Million)
        assertShortenedForTxs("333333333.3333333333333333", "333", BigDecimalShortened.Suffix.Million)
        assertShortenedForTxs("555555555.5555555555555555", "556", BigDecimalShortened.Suffix.Million)
        assertShortenedForTxs("999999999.9999999999999999", "1", BigDecimalShortened.Suffix.Billion)

        assertShortenedForTxs("1000000000", "1", BigDecimalShortened.Suffix.Billion)
        assertShortenedForTxs("1000000000.0000000000000001", "1", BigDecimalShortened.Suffix.Billion)
        assertShortenedForTxs("3333333333.3333333333333333", "3.33", BigDecimalShortened.Suffix.Billion)
        assertShortenedForTxs("5555555555.5555555555555555", "5.56", BigDecimalShortened.Suffix.Billion)
        assertShortenedForTxs("9999999999.9999999999999999", "10", BigDecimalShortened.Suffix.Billion)
        assertShortenedForTxs("33333333333.333333333333333", "33.3", BigDecimalShortened.Suffix.Billion)
        assertShortenedForTxs("55555555555.555555555555555", "55.6", BigDecimalShortened.Suffix.Billion)
        assertShortenedForTxs("99999999999.999999999999999", "100", BigDecimalShortened.Suffix.Billion)
        assertShortenedForTxs("333333333333.3333333333333333", "333", BigDecimalShortened.Suffix.Billion)
        assertShortenedForTxs("555555555555.5555555555555555", "556", BigDecimalShortened.Suffix.Billion)
        assertShortenedForTxs("999999999999.9999999999999999", "1", BigDecimalShortened.Suffix.Trillion)

        assertShortenedForTxs("1000000000000", "1", BigDecimalShortened.Suffix.Trillion)
        assertShortenedForTxs("1000000000000.0000000000000001", "1", BigDecimalShortened.Suffix.Trillion)
        assertShortenedForTxs("3333333333333.3333333333333333", "3.33", BigDecimalShortened.Suffix.Trillion)
        assertShortenedForTxs("5555555555555.5555555555555555", "5.56", BigDecimalShortened.Suffix.Trillion)
        assertShortenedForTxs("9999999999999.9999999999999999", "10", BigDecimalShortened.Suffix.Trillion)
    }

    private fun assertShortenedForTxs(value: String, expectedValue: String, expectedSuffix: BigDecimalShortened.Suffix) {
        val actual = numberFormatter.getShortenedForTxs(BigDecimal(value))

        assertEquals(expectedValue, actual.value.toPlainString())
        assertEquals(expectedSuffix, actual.suffix)
    }

    @Test
    fun testFormatFiat() {
        assertFormattedFiat(BigDecimal("1000.51"), 0, 2, "$1,000")
        assertFormattedFiat(BigDecimal("1000.1234"), 0, 2, "$1,000")
        assertFormattedFiat(BigDecimal("999.994"), 0, 2, "$999.99")
        assertFormattedFiat(BigDecimal("999.995"), 0, 2, "$999.99")
        assertFormattedFiat(BigDecimal("0.004"), 0, 2, "< $0.01")
        assertFormattedFiat(BigDecimal("0.009"), 0, 2, "< $0.01")
        assertFormattedFiat(BigDecimal("0.0004"), 0, 3, "< $0.001")
        assertFormattedFiat(BigDecimal.ZERO, 2, 2, "$0.00")
        assertFormattedFiat(BigDecimal.ZERO, 0, 2, "$0")
    }

    private fun assertFormattedFiat(value: BigDecimal, minimumFractionDigits: Int, maximumFractionDigits: Int, expected: String) {
        val formatted = numberFormatter.formatFiat(value, "$", minimumFractionDigits, maximumFractionDigits)

        assertEquals(expected, formatted)
    }

    @Test
    fun testFormatFiatSignificant() {
        assertFormattedFiatSignificant(BigDecimal("0"), "$0")
        assertFormattedFiatSignificant(BigDecimal("0.0"), "$0")
        assertFormattedFiatSignificant(BigDecimal("0.00"), "$0")

        assertFormattedFiatSignificant(BigDecimal("0.0000012345678"), "$0.000001234")
        assertFormattedFiatSignificant(BigDecimal("0.000012345678"), "$0.00001234")
        assertFormattedFiatSignificant(BigDecimal("0.00012345678"), "$0.0001")
        assertFormattedFiatSignificant(BigDecimal("0.0012345678"), "$0.0012")
        assertFormattedFiatSignificant(BigDecimal("0.012345678"), "$0.0123")
        assertFormattedFiatSignificant(BigDecimal("0.12345678"), "$0.1234")

        assertFormattedFiatSignificant(BigDecimal("1.0000012345678"), "$1")
        assertFormattedFiatSignificant(BigDecimal("1.000012345678"), "$1")
        assertFormattedFiatSignificant(BigDecimal("1.00012345678"), "$1")
        assertFormattedFiatSignificant(BigDecimal("1.0012345678"), "$1")
        assertFormattedFiatSignificant(BigDecimal("1.012345678"), "$1.01")
        assertFormattedFiatSignificant(BigDecimal("1.12345678"), "$1.12")

        assertFormattedFiatSignificant(BigDecimal("123.0000012345678"), "$123")
        assertFormattedFiatSignificant(BigDecimal("123.000012345678"), "$123")
        assertFormattedFiatSignificant(BigDecimal("123.00012345678"), "$123")
        assertFormattedFiatSignificant(BigDecimal("123.0012345678"), "$123")
        assertFormattedFiatSignificant(BigDecimal("123.012345678"), "$123.01")
        assertFormattedFiatSignificant(BigDecimal("123.12345678"), "$123.12")

        assertFormattedFiatSignificant(BigDecimal("1234.0000012345678"), "$1,234")
        assertFormattedFiatSignificant(BigDecimal("1234.000012345678"), "$1,234")
        assertFormattedFiatSignificant(BigDecimal("1234.00012345678"), "$1,234")
        assertFormattedFiatSignificant(BigDecimal("1234.0012345678"), "$1,234")
        assertFormattedFiatSignificant(BigDecimal("1234.012345678"), "$1,234")
        assertFormattedFiatSignificant(BigDecimal("1234.12345678"), "$1,234")
    }

    private fun assertFormattedFiatSignificant(value: BigDecimal, expected: String) {
        val formatted = numberFormatter.formatFiat(value, "$", 0, numberFormatter.getSignificantDecimalFiat(value))

        assertEquals(expected, formatted)
    }
}