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


    @Test
    fun format_Currency() {
        assertCurrencyFormatter(12.03903, "$ 12.04")
        assertCurrencyFormatter(12.03203, "$ 12.03")
        assertCurrencyFormatter(1203.903, "$ 1,204")
        assertCurrencyFormatter(0.0003903, "$ 0.01")
        assertCurrencyFormatter(-0.0003903, "- $ 0.01")
        assertCurrencyFormatter(0.0, "$ 0")
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


    private fun assertCurrencyFormatter(input: Double, expected: String) {
        val value = CurrencyValue(usdCurrency, input.toBigDecimal())
        val formatted = formatter.format(value, showNegativeSign = true)
        Assert.assertEquals(expected, formatted)
    }

    private fun assertCoinFormatter(input: Double, expected: String) {
        val value = CoinValue(btcCoinCode, input.toBigDecimal())
        val formatted = formatter.format(value)
        Assert.assertEquals(expected, formatted)
    }

}
