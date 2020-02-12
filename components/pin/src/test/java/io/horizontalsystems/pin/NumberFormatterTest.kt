package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.ILanguageManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.Currency
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class NumberFormatterTest {

    private val languageManager = Mockito.mock(ILanguageManager::class.java)

    private lateinit var formatter: NumberFormatter
    private val usdCurrency = Currency(code = "USD", symbol = "$")
    private val btcCoin: Coin = Coin("BTC", "Bitcoin","BTC",8, CoinType.Bitcoin)
    private val defaultLocale = Locale("en")

    @Before
    fun setup() {
        whenever(languageManager.currentLocale).thenReturn(defaultLocale)
        formatter = NumberFormatter(languageManager)
    }

    @Test
    fun format_Currency() {
        assertCurrencyFormatter(12.03903, "$12.04")
        assertCurrencyFormatter(12.03203, "$12.03")
        assertCurrencyFormatter(1203.903, "$1,204")
        assertCurrencyFormatter(0.0003903, "< $0.01")
        assertCurrencyFormatter(0.0100, "< $0.01")
        assertCurrencyFormatter(-0.0003903, "- < $0.01")
        assertCurrencyFormatter(0.0, "$0")
    }

    @Test
    fun format_Currency_ForTransactionInfo() {
        assertTransactionCurrencyFormatter(12.03903, "$12.04")
        assertTransactionCurrencyFormatter(12.03203, "$12.03")
        assertTransactionCurrencyFormatter(1203.903, "$1,203.9")
        assertTransactionCurrencyFormatter(0.0003903, "$0.01")
        assertTransactionCurrencyFormatter(0.0100, "$0.01")
        assertTransactionCurrencyFormatter(0.0, "$0.00")
    }

    @Test
    fun format_Currency_ForRates() {
        assertRatesCurrencyFormatter(12.03903, false, null, "$12.04")
        assertRatesCurrencyFormatter(12.03203,false, null, "$12.03")
        assertRatesCurrencyFormatter(21203.903,false, null, "$21,204")
        assertRatesCurrencyFormatter(1203.903,true, null, "$1,204")
        assertRatesCurrencyFormatter(0.00039,false, null, "$0.00039")
        assertRatesCurrencyFormatter(0.0003900,false, null, "$0.00039")
        assertRatesCurrencyFormatter(0.0003903,false, null, "$0.0003903")
        assertRatesCurrencyFormatter(0.0100,false, null, "$0.01")
        assertRatesCurrencyFormatter(0.0,false, null, "$0.00")
        assertRatesCurrencyFormatter(0.0,true, null, "$0")
        assertRatesCurrencyFormatter(0.00000041234,false, null, "$0.00000041")
        assertRatesCurrencyFormatter(0.00039,false, 4, "$0.0004")
        assertRatesCurrencyFormatter(0.00039124,false, 8, "$0.00039124")
        assertRatesCurrencyFormatter(0.000391241234,false, 18, "$0.000391241234")
    }

    @Test
    fun format_Coin() {
        assertCoinFormatter(12.03903001, "12.039 BTC")
        assertCoinFormatter(12.0000000012345, "12 BTC")
        assertCoinFormatter(12.00000000, "12 BTC")
        assertCoinFormatter(0.000030012345, "0.00003001 BTC")
        assertCoinFormatter(0.0000000012345, "0 BTC")
        assertCoinFormatter(0.123456789, "0.1235 BTC")
    }

    @Test
    fun format_Currency_by_russian_locale() {
        whenever(languageManager.currentLocale).thenReturn(Locale("ru"))
        formatter = NumberFormatter(languageManager)
        assertCurrencyFormatter(0.0003903, "< $0,01")
        assertCurrencyFormatter(12.03903, "$12,04")
    }

    private fun assertCurrencyFormatter(input: Double, expected: String) {
        val value = CurrencyValue(usdCurrency, input.toBigDecimal())
        val formatted = formatter.format(value, showNegativeSign = true, trimmable = true)
        Assert.assertEquals(expected, formatted)
    }

    private fun assertTransactionCurrencyFormatter(input: Double, expected: String) {
        val value = CurrencyValue(usdCurrency, input.toBigDecimal())
        val formatted = formatter.format(value, showNegativeSign = false, canUseLessSymbol = false)
        Assert.assertEquals(expected, formatted)
    }

    private fun assertRatesCurrencyFormatter(input: Double, trimmable: Boolean = false, maxFraction: Int? = null, expected: String) {
        val value = CurrencyValue(usdCurrency, input.toBigDecimal())
        val formatted = formatter.formatForRates(value, trimmable = trimmable, maxFraction = maxFraction)
        Assert.assertEquals(expected, formatted)
    }

    private fun assertCoinFormatter(input: Double, expected: String) {
        val value = CoinValue(btcCoin, input.toBigDecimal())
        val formatted = formatter.format(value)
        Assert.assertEquals(expected, formatted)
    }
}
