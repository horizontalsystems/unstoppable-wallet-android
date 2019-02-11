package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal

class StateViewItemFactoryTest {

    private val factory = StateViewItemFactory()
    private val state = SendModule.State(8, SendModule.InputType.COIN)
    private val coinValue = CoinValue("BTC", BigDecimal("123.45"))
    private val usdCurrency = Currency(code = "USD", symbol = "$")
    private val currencyValue = CurrencyValue(usdCurrency, BigDecimal("0.116699446"))


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

    @Test
    fun testFeeInfo_CoinType() {
        state.inputType = SendModule.InputType.COIN
        state.feeCoinValue = coinValue
        state.feeCurrencyValue = currencyValue
        state.feeError = null

        val viewItem = factory.viewItemForState(state, false)

        val expectedFeeInfo = SendModule.FeeInfo()
        expectedFeeInfo.primaryFeeInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
        expectedFeeInfo.secondaryFeeInfo = SendModule.AmountInfo.CurrencyValueInfo(currencyValue)

        Assert.assertEquals(expectedFeeInfo.primaryFeeInfo, viewItem.feeInfo?.primaryFeeInfo)
        Assert.assertEquals(expectedFeeInfo.secondaryFeeInfo, viewItem.feeInfo?.secondaryFeeInfo)
        Assert.assertEquals(null, viewItem.feeInfo?.error)
    }

    @Test
    fun testFeeInfo_CurrencyType() {
        state.inputType = SendModule.InputType.CURRENCY
        state.feeCoinValue = coinValue
        state.feeCurrencyValue = currencyValue
        state.feeError = null

        val viewItem = factory.viewItemForState(state, false)

        val expectedFeeInfo = SendModule.FeeInfo()
        expectedFeeInfo.primaryFeeInfo = SendModule.AmountInfo.CurrencyValueInfo(currencyValue)
        expectedFeeInfo.secondaryFeeInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)

        Assert.assertEquals(expectedFeeInfo.primaryFeeInfo, viewItem.feeInfo?.primaryFeeInfo)
        Assert.assertEquals(expectedFeeInfo.secondaryFeeInfo, viewItem.feeInfo?.secondaryFeeInfo)
        Assert.assertEquals(null, viewItem.feeInfo?.error)
    }

    @Test
    fun testFeeInfo_feeError() {
        state.inputType = SendModule.InputType.CURRENCY
        state.feeCoinValue = null
        state.feeCurrencyValue = null
        val feeError = SendModule.AmountError.Erc20FeeError( "TNT", CoinValue("ETH", BigDecimal("0.04")))
        state.feeError = feeError

        val viewItem = factory.viewItemForState(state, false)

        val expectedFeeInfo = SendModule.FeeInfo()
        expectedFeeInfo.error = feeError

        Assert.assertEquals(expectedFeeInfo.error, viewItem.feeInfo?.error)
    }

    @Test
    fun testSendButtonEnabled_feeError() {
        state.coinValue = coinValue
        state.address = "address"
        state.feeError = SendModule.AmountError.Erc20FeeError("TNT", CoinValue("ETH", BigDecimal.ZERO))

        val viewItem = factory.viewItemForState(state, false)

        Assert.assertFalse(viewItem.sendButtonEnabled)
    }

}
