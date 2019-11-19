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
    fun format_Currency_forLatestRate() {
        assertCurrencyFormatterForLatestRates(1203.903, "$1,204")
        assertCurrencyFormatterForLatestRates(0.0005, "$0.0005")
        assertCurrencyFormatterForLatestRates(0.5005, "$0.5")
        assertCurrencyFormatterForLatestRates(0.0805, "$0.0805")
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


    private fun assertCurrencyFormatterForLatestRates(input: Double, expected: String) {
        val value = CurrencyValue(usdCurrency, input.toBigDecimal())
        val formatted = formatter.format(value, trimmable = true, canUseLessSymbol = false)
        Assert.assertEquals(expected, formatted)
    }

    private fun assertCurrencyFormatter(input: Double, expected: String) {
        val value = CurrencyValue(usdCurrency, input.toBigDecimal())
        val formatted = formatter.format(value, showNegativeSign = true, trimmable = true)
        Assert.assertEquals(expected, formatted)
    }

    private fun assertCoinFormatter(input: Double, expected: String) {
        val value = CoinValue(btcCoin, input.toBigDecimal())
        val formatted = formatter.format(value)
        Assert.assertEquals(expected, formatted)
    }
}
