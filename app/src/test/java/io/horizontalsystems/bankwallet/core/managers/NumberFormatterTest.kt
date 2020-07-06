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
        assertFormattedCoin(BigDecimal("0.000012345678"), 0, 8, "0.00001235 BTC")
        assertFormattedCoin(BigDecimal("0.00012345678"), 0, 8, "0.00012346 BTC")
        assertFormattedCoin(BigDecimal("0.0012345678"), 0, 8, "0.00123457 BTC")
        assertFormattedCoin(BigDecimal("0.012345678"), 0, 8, "0.01234568 BTC")
        assertFormattedCoin(BigDecimal("0.12345678"), 0, 8, "0.12345678 BTC")

        assertFormattedCoin(BigDecimal("1.0000012345678"), 0, 8, "1.00000123 BTC")
        assertFormattedCoin(BigDecimal("1.000012345678"), 0, 8, "1.00001235 BTC")
        assertFormattedCoin(BigDecimal("1.00012345678"), 0, 8, "1.00012346 BTC")
        assertFormattedCoin(BigDecimal("1.0012345678"), 0, 8, "1.00123457 BTC")
        assertFormattedCoin(BigDecimal("1.012345678"), 0, 8, "1.01234568 BTC")
        assertFormattedCoin(BigDecimal("1.12345678"), 0, 8, "1.12345678 BTC")

        assertFormattedCoin(BigDecimal("123.0000012345678"), 0, 8, "123.00000123 BTC")
        assertFormattedCoin(BigDecimal("123.000012345678"), 0, 8, "123.00001235 BTC")
        assertFormattedCoin(BigDecimal("123.00012345678"), 0, 8, "123.00012346 BTC")
        assertFormattedCoin(BigDecimal("123.0012345678"), 0, 8, "123.00123457 BTC")
        assertFormattedCoin(BigDecimal("123.012345678"), 0, 8, "123.01234568 BTC")
        assertFormattedCoin(BigDecimal("123.12345678"), 0, 8, "123.12345678 BTC")

        assertFormattedCoin(BigDecimal("1234.0000012345678"), 0, 8, "1,234.00000123 BTC")
        assertFormattedCoin(BigDecimal("1234.000012345678"), 0, 8, "1,234.00001235 BTC")
        assertFormattedCoin(BigDecimal("1234.00012345678"), 0, 8, "1,234.00012346 BTC")
        assertFormattedCoin(BigDecimal("1234.0012345678"), 0, 8, "1,234.00123457 BTC")
        assertFormattedCoin(BigDecimal("1234.012345678"), 0, 8, "1,234.01234568 BTC")
        assertFormattedCoin(BigDecimal("1234.12345678"), 0, 8, "1,234.12345678 BTC")
    }

    private fun assertFormattedCoin(value: BigDecimal, minimumFractionDigits: Int, maximumFractionDigits: Int, expected: String) {
        val formatted = numberFormatter.formatCoin(value, "BTC", minimumFractionDigits, maximumFractionDigits)
        assertEquals(expected, formatted)
    }

    @Test
    fun testFormatFiat() {
        assertFormattedFiat(BigDecimal("1000.51"), 0, 2, "$1,001")
        assertFormattedFiat(BigDecimal("1000.1234"), 0, 2, "$1,000")
        assertFormattedFiat(BigDecimal("999.994"), 0, 2, "$999.99")
        assertFormattedFiat(BigDecimal("999.995"), 0, 2, "$1,000")
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

        assertFormattedFiatSignificant(BigDecimal("0.0000012345678"), "$0.000001235")
        assertFormattedFiatSignificant(BigDecimal("0.000012345678"), "$0.00001235")
        assertFormattedFiatSignificant(BigDecimal("0.00012345678"), "$0.0001")
        assertFormattedFiatSignificant(BigDecimal("0.0012345678"), "$0.0012")
        assertFormattedFiatSignificant(BigDecimal("0.012345678"), "$0.0123")
        assertFormattedFiatSignificant(BigDecimal("0.12345678"), "$0.1235")

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