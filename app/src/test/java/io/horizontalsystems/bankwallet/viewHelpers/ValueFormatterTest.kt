package io.horizontalsystems.bankwallet.viewHelpers

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import org.junit.Assert
import org.junit.Test

class ValueFormatterTest {

    private val formatter = ValueFormatter
    private val usdCurrency = Currency(code = "USD", symbol = "$")
    private val btcCoinCode: CoinCode = "BTC"


    //Currency formatting
    @Test
    fun format_Currency_12_03903() {
        val value = CurrencyValue(usdCurrency,12.03903)
        val formatted = formatter.format(value)
        val expected = "$ 12.04"
        Assert.assertEquals(expected, formatted)
    }

    @Test
    fun format_Currency_12_03203() {
        val value = CurrencyValue(usdCurrency,12.03203)
        val formatted = formatter.format(value)
        val expected = "$ 12.03"
        Assert.assertEquals(expected, formatted)
    }

    @Test
    fun format_Currency_1203_903() {
        val value = CurrencyValue(usdCurrency,1203.903)
        val formatted = formatter.format(value)
        val expected = "$ 1,204"
        Assert.assertEquals(expected, formatted)
    }

    @Test
    fun format_Currency_0_000903() {
        val value = CurrencyValue(usdCurrency,0.0003903)
        val formatted = formatter.format(value)
        val expected = "$ 0.01"
        Assert.assertEquals(expected, formatted)
    }

    @Test
    fun format_Currency_negative_0_000903() {
        val value = CurrencyValue(usdCurrency,-0.0003903)
        val formatted = formatter.format(value, showNegativeSign = true)
        val expected = "- $ 0.01"
        Assert.assertEquals(expected, formatted)
    }

    //Coin formatting
    @Test
    fun format_Coin_12_039030012345() {
        val value = CoinValue(btcCoinCode,12.039030012345)
        val formatted = formatter.format(value)
        val expected = "12.039 BTC"
        Assert.assertEquals(expected, formatted)
    }

    @Test
    fun format_Coin_12_0000000012345() {
        val value = CoinValue(btcCoinCode,12.0000000012345)
        val formatted = formatter.format(value)
        val expected = "12 BTC"
        Assert.assertEquals(expected, formatted)
    }

    @Test
    fun format_Coin_12_00000000() {
        val value = CoinValue(btcCoinCode,12.00000000)
        val formatted = formatter.format(value)
        val expected = "12 BTC"
        Assert.assertEquals(expected, formatted)
    }

    @Test
    fun format_Coin_0_000030012345() {
        val value = CoinValue(btcCoinCode,0.000030012345)
        val formatted = formatter.format(value)
        val expected = "0.00003001 BTC"
        Assert.assertEquals(expected, formatted)
    }

    @Test
    fun format_Coin_0_0000000012345() {
        val value = CoinValue(btcCoinCode,0.0000000012345)
        val formatted = formatter.format(value)
        val expected = "0 BTC"
        Assert.assertEquals(expected, formatted)
    }

    @Test
    fun format_Coin_0_123456789() {
        val value = CoinValue(btcCoinCode,0.123456789)
        val formatted = formatter.format(value)
        val expected = "0.1235 BTC"
        Assert.assertEquals(expected, formatted)
    }
}
