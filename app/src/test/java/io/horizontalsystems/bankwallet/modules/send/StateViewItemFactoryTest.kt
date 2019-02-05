package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal

class StateViewItemFactoryTest {

    private val factory = StateViewItemFactory()
    val state = SendModule.State(8, SendModule.InputType.COIN)
    private val coinValue = CoinValue("BTC", BigDecimal("123.45"))
    private val usdCurrency = Currency(code = "USD", symbol = "$")


    @Test
    fun testDecimal() {
        val expectedDecimal = 8

        state.decimal = expectedDecimal

        val viewItem = factory.viewItemForState(state)

        Assert.assertEquals(viewItem.decimal, expectedDecimal)
    }

    @Test
    fun testAmountRounding_coin() {
        val expectedValue = BigDecimal("0.11669944")

        state.inputType = SendModule.InputType.COIN
        state.decimal = 8
        state.coinValue = CoinValue(coinCode = coinValue.coinCode, value = BigDecimal("0.116699446"))

        val viewItem = factory.viewItemForState(state)

        Assert.assertEquals(SendModule.AmountInfo.CoinValueInfo(coinValue = CoinValue(coinCode = coinValue.coinCode, value = expectedValue)), viewItem.amountInfo)
    }

    @Test
    fun testAmountRounding_fiat() {
        val expectedValue = BigDecimal("0.12")

        state.inputType = SendModule.InputType.CURRENCY
        state.decimal = 2
        state.currencyValue = CurrencyValue(usdCurrency, BigDecimal("0.116699446"))

        val viewItem = factory.viewItemForState(state)

        Assert.assertEquals(SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(usdCurrency, expectedValue)), viewItem.amountInfo)
    }
}
