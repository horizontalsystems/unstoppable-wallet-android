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
    fun testFormatCoin() {
        assertFormattedCoin(BigDecimal("0.000012345678"), 0, 4, "< 0.0001 BTC")

        assertFormattedCoin(BigDecimal("0.0000012345678"), 0, 8, "0.00000123 BTC")
        assertFormattedCoin(BigDecimal("0.000012345678"), 0, 8, "0.00001234 BTC")
        assertFormattedCoin(BigDecimal("0.00012345678"), 0, 8, "0.00012345 BTC")
        assertFormattedCoin(BigDecimal("0.0012345678"), 0, 8, "0.00123456 BTC")
        assertFormattedCoin(BigDecimal("0.012345678"), 0, 8, "0.01234567 BTC")
        assertFormattedCoin(BigDecimal("0.12345678"), 0, 8, "0.12345678 BTC")

        assertFormattedCoin(BigDecimal("1.0000012345678"), 0, 8, "1.00000123 BTC")
        assertFormattedCoin(BigDecimal("1.000012345678"), 0, 8, "1.00001234 BTC")
        assertFormattedCoin(BigDecimal("1.00012345678"), 0, 8, "1.00012345 BTC")
        assertFormattedCoin(BigDecimal("1.0012345678"), 0, 8, "1.00123456 BTC")
        assertFormattedCoin(BigDecimal("1.012345678"), 0, 8, "1.01234567 BTC")
        assertFormattedCoin(BigDecimal("1.12345678"), 0, 8, "1.12345678 BTC")

        assertFormattedCoin(BigDecimal("123.0000012345678"), 0, 8, "123.00000123 BTC")
        assertFormattedCoin(BigDecimal("123.000012345678"), 0, 8, "123.00001234 BTC")
        assertFormattedCoin(BigDecimal("123.00012345678"), 0, 8, "123.00012345 BTC")
        assertFormattedCoin(BigDecimal("123.0012345678"), 0, 8, "123.00123456 BTC")
        assertFormattedCoin(BigDecimal("123.012345678"), 0, 8, "123.01234567 BTC")
        assertFormattedCoin(BigDecimal("123.12345678"), 0, 8, "123.12345678 BTC")

        assertFormattedCoin(BigDecimal("1234.0000012345678"), 0, 8, "1,234.00000123 BTC")
        assertFormattedCoin(BigDecimal("1234.000012345678"), 0, 8, "1,234.00001234 BTC")
        assertFormattedCoin(BigDecimal("1234.00012345678"), 0, 8, "1,234.00012345 BTC")
        assertFormattedCoin(BigDecimal("1234.0012345678"), 0, 8, "1,234.00123456 BTC")
        assertFormattedCoin(BigDecimal("1234.012345678"), 0, 8, "1,234.01234567 BTC")
        assertFormattedCoin(BigDecimal("1234.12345678"), 0, 8, "1,234.12345678 BTC")
    }

    private fun assertFormattedCoin(value: BigDecimal, minimumFractionDigits: Int, maximumFractionDigits: Int, expected: String) {
        val formatted = numberFormatter.formatCoin(value, "BTC", minimumFractionDigits, maximumFractionDigits)
        assertEquals(expected, formatted)
    }

    @Test
    fun testFormatCoinSignificant() {
        assertFormattedCoinSignificant(BigDecimal("0.0000000000001000"), "< 0.00000001 BTC")

        assertFormattedCoinSignificant(BigDecimal("0.00000123"), "0.00000123 BTC")
        assertFormattedCoinSignificant(BigDecimal("0.00001234"), "0.00001234 BTC")
        assertFormattedCoinSignificant(BigDecimal("0.00012345"), "0.0001 BTC")
        assertFormattedCoinSignificant(BigDecimal("0.00123456"), "0.0012 BTC")
        assertFormattedCoinSignificant(BigDecimal("0.01234567"), "0.0123 BTC")
        assertFormattedCoinSignificant(BigDecimal("0.12345678"), "0.1234 BTC")

        assertFormattedCoinSignificant(BigDecimal("1.00000123"), "1.00000123 BTC")
        assertFormattedCoinSignificant(BigDecimal("5.00000123"), "5.00000123 BTC")
        assertFormattedCoinSignificant(BigDecimal("1.00001234"), "1.00001234 BTC")
        assertFormattedCoinSignificant(BigDecimal("1.00012345"), "1.0001 BTC")
        assertFormattedCoinSignificant(BigDecimal("1.00123456"), "1.0012 BTC")
        assertFormattedCoinSignificant(BigDecimal("1.01234567"), "1.0123 BTC")
        assertFormattedCoinSignificant(BigDecimal("1.12345678"), "1.1234 BTC")

        assertFormattedCoinSignificant(BigDecimal("12.00000123"), "12 BTC")
        assertFormattedCoinSignificant(BigDecimal("12.00001234"), "12 BTC")
        assertFormattedCoinSignificant(BigDecimal("12.00012345"), "12.0001 BTC")
        assertFormattedCoinSignificant(BigDecimal("12.00123456"), "12.0012 BTC")
        assertFormattedCoinSignificant(BigDecimal("12.01234567"), "12.0123 BTC")
        assertFormattedCoinSignificant(BigDecimal("12.12345678"), "12.1234 BTC")

        assertFormattedCoinSignificant(BigDecimal("123.00000123"), "123 BTC")
        assertFormattedCoinSignificant(BigDecimal("123.00001234"), "123 BTC")
        assertFormattedCoinSignificant(BigDecimal("123.00012345"), "123 BTC")
        assertFormattedCoinSignificant(BigDecimal("123.00123456"), "123 BTC")
        assertFormattedCoinSignificant(BigDecimal("123.01234567"), "123.01 BTC")
        assertFormattedCoinSignificant(BigDecimal("123.12345678"), "123.12 BTC")

        assertFormattedCoinSignificant(BigDecimal("1234.00000123"), "1,234 BTC")
        assertFormattedCoinSignificant(BigDecimal("1234.00001234"), "1,234 BTC")
        assertFormattedCoinSignificant(BigDecimal("1234.00012345"), "1,234 BTC")
        assertFormattedCoinSignificant(BigDecimal("1234.00123456"), "1,234 BTC")
        assertFormattedCoinSignificant(BigDecimal("1234.01234567"), "1,234.01 BTC")
        assertFormattedCoinSignificant(BigDecimal("1234.12345678"), "1,234.12 BTC")

        assertFormattedCoinSignificant(BigDecimal("0.00000123"), "0.00000123 BTC")
        assertFormattedCoinSignificant(BigDecimal("1.00000123"), "1.00000123 BTC")
        assertFormattedCoinSignificant(BigDecimal("1.00012345"), "1.0001 BTC")

        assertFormattedCoinSignificant(BigDecimal("-5.00000123"), "-5.00000123 BTC")
    }

    private fun assertFormattedCoinSignificant(value: BigDecimal, expected: String) {
        val formatted = numberFormatter.formatCoin(value, "BTC", 0, numberFormatter.getSignificantDecimalCoin(value))

        assertEquals(expected, formatted)
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