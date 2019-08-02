//package io.horizontalsystems.bankwallet.modules.send
//
//import io.horizontalsystems.bankwallet.entities.*
//import org.junit.Assert
//import org.junit.Before
//import org.junit.Test
//import java.math.BigDecimal
//
//class StateViewItemFactoryTest {
//
//    private val factory = ConfirmationViewItemFactory()
//    private val state = SendModule.State(8, SendModule.InputType.COIN)
//    private val confirmation = SendModule.State(8, SendModule.InputType.COIN)
//    private val coinValue = CoinValue("BTC", BigDecimal("123.45"))
//    private val feeCoinValue = CoinValue("BTC", BigDecimal("0.05"))
//    private val usdCurrency = Currency(code = "USD", symbol = "$")
//    private val currencyValue = CurrencyValue(usdCurrency, BigDecimal("0.116699446"))
//    private val feeCurrencyValue = CurrencyValue(usdCurrency, BigDecimal("0.001699446"))
//    private val coin = Coin("Bitcoin", "BTC", CoinType.Bitcoin)
//
//    @Before
//    fun setup() {
//        confirmation.coinValue = coinValue
//        confirmation.address = "address"
//    }
//
//    @Test
//    fun testDecimal() {
//        val expectedDecimal = 8
//
//        state.decimal = expectedDecimal
//
//        val viewItem = factory.viewItemForState(state)
//
//        Assert.assertEquals(viewItem.decimal, expectedDecimal)
//    }
//
//    @Test
//    fun testAmountRounding_coin() {
//        val expectedValue = BigDecimal("0.11669944")
//
//        state.inputType = SendModule.InputType.COIN
//        state.decimal = 8
//        state.coinValue = CoinValue(coinCode = coinValue.coinCode, value = BigDecimal("0.116699446"))
//
//        val viewItem = factory.viewItemForState(state)
//
//        Assert.assertEquals(SendModule.AmountInfo.CoinValueInfo(coinValue = CoinValue(coinCode = coinValue.coinCode, value = expectedValue)), viewItem.amountInfo)
//    }
//
//    @Test
//    fun testAmountRounding_fiat() {
//        val expectedValue = BigDecimal("0.12")
//
//        state.inputType = SendModule.InputType.CURRENCY
//        state.decimal = 2
//        state.currencyValue = CurrencyValue(usdCurrency, BigDecimal("0.116699446"))
//
//        val viewItem = factory.viewItemForState(state)
//
//        Assert.assertEquals(SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(usdCurrency, expectedValue)), viewItem.amountInfo)
//    }
//
//    @Test
//    fun testFeeInfo_CoinType() {
//        state.inputType = SendModule.InputType.COIN
//        state.feeCoinValue = coinValue
//        state.feeCurrencyValue = currencyValue
//        state.feeError = null
//
//        val viewItem = factory.viewItemForState(state, false)
//
//        val expectedFeeInfo = SendModule.FeeInfo()
//        expectedFeeInfo.primaryFeeInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
//        expectedFeeInfo.secondaryFeeInfo = SendModule.AmountInfo.CurrencyValueInfo(currencyValue)
//
//        Assert.assertEquals(expectedFeeInfo.primaryFeeInfo, viewItem.feeInfo?.primaryFeeInfo)
//        Assert.assertEquals(expectedFeeInfo.secondaryFeeInfo, viewItem.feeInfo?.secondaryFeeInfo)
//        Assert.assertEquals(null, viewItem.feeInfo?.error)
//    }
//
//    @Test
//    fun testFeeInfo_CurrencyType() {
//        state.inputType = SendModule.InputType.CURRENCY
//        state.feeCoinValue = coinValue
//        state.feeCurrencyValue = currencyValue
//        state.feeError = null
//
//        val viewItem = factory.viewItemForState(state, false)
//
//        val expectedFeeInfo = SendModule.FeeInfo()
//        expectedFeeInfo.primaryFeeInfo = SendModule.AmountInfo.CurrencyValueInfo(currencyValue)
//        expectedFeeInfo.secondaryFeeInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
//
//        Assert.assertEquals(expectedFeeInfo.primaryFeeInfo, viewItem.feeInfo?.primaryFeeInfo)
//        Assert.assertEquals(expectedFeeInfo.secondaryFeeInfo, viewItem.feeInfo?.secondaryFeeInfo)
//        Assert.assertEquals(null, viewItem.feeInfo?.error)
//    }
//
//    @Test
//    fun testFeeInfo_feeError() {
//        state.inputType = SendModule.InputType.CURRENCY
//        state.feeCoinValue = null
//        state.feeCurrencyValue = null
//        val feeError = SendModule.AmountError.Erc20FeeError( "TNT", CoinValue("ETH", BigDecimal("0.04")))
//        state.feeError = feeError
//
//        val viewItem = factory.viewItemForState(state, false)
//
//        val expectedFeeInfo = SendModule.FeeInfo()
//        expectedFeeInfo.error = feeError
//
//        Assert.assertEquals(expectedFeeInfo.error, viewItem.feeInfo?.error)
//    }
//
//    @Test
//    fun testSendButtonEnabled_feeError() {
//        state.coinValue = coinValue
//        state.address = "address"
//        state.feeError = SendModule.AmountError.Erc20FeeError("TNT", CoinValue("ETH", BigDecimal.ZERO))
//
//        val viewItem = factory.viewItemForState(state, false)
//
//        Assert.assertFalse(viewItem.sendButtonEnabled)
//    }
//
//    @Test
//    fun testConfirmation_primaryAmount_coinInputType() {
//        confirmation.feeCoinValue = feeCoinValue
//        confirmation.inputType = SendModule.InputType.COIN
//
//        val viewItem = factory.confirmationViewItem(confirmation)
//
//        Assert.assertEquals(SendModule.AmountInfo.CoinValueInfo(coinValue), viewItem?.primaryAmountInfo)
//    }
//
//    @Test
//    fun testConfirmation_primaryAmount_currencyInputType() {
//        confirmation.feeCurrencyValue = feeCurrencyValue
//        confirmation.currencyValue = currencyValue
//        confirmation.inputType = SendModule.InputType.CURRENCY
//
//        val viewItem = factory.confirmationViewItem(confirmation)
//
//        Assert.assertEquals(SendModule.AmountInfo.CurrencyValueInfo(currencyValue), viewItem?.primaryAmountInfo)
//    }
//
//    @Test
//    fun testConfirmation_secondaryAmount_coinInputType() {
//        confirmation.feeCoinValue = feeCoinValue
//        confirmation.inputType = SendModule.InputType.COIN
//        confirmation.currencyValue = currencyValue
//
//        val viewItem = factory.confirmationViewItem(confirmation)
//
//        Assert.assertEquals(SendModule.AmountInfo.CurrencyValueInfo(currencyValue), viewItem?.secondaryAmountInfo)
//    }
//
//    @Test
//    fun testConfirmation_secondaryAmount_coinInputType_noCurrencyValue() {
//        confirmation.feeCoinValue = feeCoinValue
//        confirmation.inputType = SendModule.InputType.COIN
//        confirmation.currencyValue = null
//
//        val viewItem = factory.confirmationViewItem(confirmation)
//
//        Assert.assertEquals(null, viewItem?.secondaryAmountInfo)
//    }
//
//    @Test
//    fun testConfirmation_secondaryAmount_currencyInputType() {
//        confirmation.feeCoinValue = feeCoinValue
//        confirmation.inputType = SendModule.InputType.CURRENCY
//        confirmation.currencyValue = currencyValue
//
//        val viewItem = factory.confirmationViewItem(confirmation)
//
//        Assert.assertEquals(SendModule.AmountInfo.CoinValueInfo(coinValue), viewItem?.secondaryAmountInfo)
//    }
//
//    @Test
//    fun testConfirmation_TotalInfo_erc20_WithoutCurrencyValue() {
//        confirmation.feeCoinValue = CoinValue("ETH", BigDecimal("0.012"))
//
//        val viewItem = factory.confirmationViewItem(confirmation)
//
//        Assert.assertEquals(null, viewItem?.totalInfo)
//    }
//
//}
