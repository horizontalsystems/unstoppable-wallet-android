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
    fun testFormatFiat() {
        assertFormattedFiat(BigDecimal("0.004"), 0, 2, "< $0.01")
        assertFormattedFiat(BigDecimal("0.009"), 0, 2, "< $0.01")
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
        assertFormattedFiatSignificant(BigDecimal("1234.012345678"), "$1,234.01")
        assertFormattedFiatSignificant(BigDecimal("1234.12345678"), "$1,234.12")
    }

    private fun assertFormattedFiatSignificant(value: BigDecimal, expected: String) {
        val formatted = numberFormatter.formatFiat(value, "$", 0, numberFormatter.getSignificantDecimal(value))

        assertEquals(expected, formatted)
    }


//    @Test
//    fun testFormatBigDecimal() {
//
//        assertBigDecimal(BigDecimal("235.0000"), "235 BTC")
//        assertBigDecimal(BigDecimal("2350000"), "2,350,000 BTC")
//    }
//
//    private fun assertBigDecimal(value: BigDecimal, expected: String) {
//        val format = numberFormatter.format(value, "BTC")
//        assertEquals(expected, format)
//    }
//
//    @Test
//    fun testFormatDouble() {
//        assertFormatDouble(0.123456789, "0.12345679")
//        assertFormatDouble(0.0, "0")
//    }
//
//    private fun assertFormatDouble(value: Double, expected: String) {
//        assertEquals(expected, numberFormatter.format(value))
//    }
}