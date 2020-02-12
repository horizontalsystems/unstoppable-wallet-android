//package io.horizontalsystems.bankwallet.modules.send
//
//import com.nhaarman.mockito_kotlin.*
//import io.horizontalsystems.bankwallet.core.*
//import io.horizontalsystems.bankwallet.entities.*
//import io.horizontalsystems.bankwallet.modules.RxBaseTest
//import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
//import io.reactivex.Flowable
//import io.reactivex.Single
//import io.reactivex.plugins.RxJavaPlugins
//import io.reactivex.schedulers.TestScheduler
//import org.junit.Assert
//import org.junit.Before
//import org.junit.Test
//import org.mockito.Mockito.mock
//import java.math.BigDecimal
//import java.math.RoundingMode
//import java.util.concurrent.TimeUnit
//
//class SendInteractorTest {
//
//    private val delegate = mock(SendModule.IInteractorDelegate::class.java)
//    private val localStorage = mock(ILocalStorage::class.java)
//    private val clipboardManager = mock(IClipboardManager::class.java)
//    private val appConfigProvider = mock(IAppConfigProvider::class.java)
//
//    private val currencyManager = mock(ICurrencyManager::class.java)
//    private val rateStorage = mock(IRateStorage::class.java)
//    private val coin = mock(Coin::class.java)
//    private val wallet = mock(Wallet::class.java)
//    private val adapter = mock(IAdapter::class.java)
//
//    private val currency = Currency("USD", symbol = "\u0024")
//    private val coinCode = CoinCode()
//    private val feeCoinCode = "ETH"
//    private val rate = mock(Rate::class.java)
//    private val fiatFeeRate = mock(Rate::class.java)
//    private val userInput = mock(SendModule.UserInput::class.java)
//    private val balance = BigDecimal(123)
//    private val zero = BigDecimal.ZERO
//    private val one = BigDecimal.ONE
//    private val fiatDecimal = 2
//    private val maxDecimal = 8
//    private val feePriority = FeeRatePriority.MEDIUM
//
//    private val testScheduler = TestScheduler()
//
//    private lateinit var interactor: SendInteractor
//
//    @Before
//    fun setup() {
//        RxBaseTest.setup()
//
//        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
//
//        whenever(userInput.inputType).thenReturn(SendModule.InputType.COIN)
//        whenever(rate.value).thenReturn(BigDecimal("0.1"))
//        whenever(rate.expired).thenReturn(false)
//        whenever(fiatFeeRate.value).thenReturn(BigDecimal("0.4"))
//        whenever(fiatFeeRate.expired).thenReturn(false)
//        whenever(coin.code).thenReturn(coinCode)
//        whenever(wallet.coin).thenReturn(coin)
//        whenever(currencyManager.baseCurrency).thenReturn(currency)
//        whenever(rateStorage.latestRateObservable(coinCode, currency.code)).thenReturn(Flowable.just(rate))
//        whenever(rateStorage.latestRateObservable(feeCoinCode, currency.code)).thenReturn(Flowable.just(fiatFeeRate))
//        whenever(adapter.wallet).thenReturn(wallet)
//        whenever(adapter.balance).thenReturn(balance)
//        whenever(adapter.decimal).thenReturn(fiatDecimal)
//        whenever(appConfigProvider.fiatDecimal).thenReturn(fiatDecimal)
//        whenever(appConfigProvider.maxDecimal).thenReturn(maxDecimal)
//
//        interactor = SendInteractor(currencyManager, rateStorage, localStorage, clipboardManager, adapter, appConfigProvider)
//        interactor.delegate = delegate
//    }
//
//    @Test
//    fun defaultInputType_get() {
//        val inputType = mock(SendModule.InputType::class.java)
//
//        whenever(localStorage.sendInputType).thenReturn(inputType)
//
//        Assert.assertEquals(inputType, interactor.defaultInputType)
//    }
//
//    @Test
//    fun defaultInputType_getDefault() {
//        whenever(localStorage.sendInputType).thenReturn(null)
//
//        Assert.assertEquals(SendModule.InputType.COIN, interactor.defaultInputType)
//    }
//
//    @Test
//    fun defaultInputType_set() {
//        val inputType = mock(SendModule.InputType::class.java)
//
//        interactor.defaultInputType = inputType
//
//        verify(localStorage).sendInputType = inputType
//    }
//
//    @Test
//    fun retrieveRate() {
//        interactor.retrieveRate()
//
//        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
//
//        verify(rateStorage).latestRateObservable(coinCode, currency.code)
//    }
//
//    @Test
//    fun retrieveRate_expiredRate() {
//        whenever(rate.expired).thenReturn(true)
//        whenever(rateStorage.latestRateObservable(coinCode, currency.code)).thenReturn(Flowable.just(rate))
//
//        interactor.retrieveRate()
//
//        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
//
//        verify(delegate).didRateRetrieve(null)
//    }
//
//    @Test
//    fun send_inCurrency() {
//        val feeRatePriority = FeeRatePriority.MEDIUM
//        interactor.retrieveRate() // set rate
//
//        whenever(rate.value).thenReturn(BigDecimal(1024))
//        whenever(userInput.inputType).thenReturn(SendModule.InputType.CURRENCY)
//        whenever(userInput.address).thenReturn("abc")
//        whenever(userInput.amount).thenReturn(one)
//        whenever(userInput.feePriority).thenReturn(feeRatePriority)
//        whenever(adapter.decimal).thenReturn(8)
//
//        val expectedAmountToSend = BigDecimal.valueOf(0.00097656) // 0.0009765625
//        whenever(adapter.send("abc", expectedAmountToSend, feeRatePriority)).thenReturn(Single.just(Unit))
//
//        interactor.send(userInput)
//
//        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
//
//        verify(delegate).didSend()
//    }
//
//    @Test
//    fun send() {
//        interactor.retrieveRate() // set rate
//
//        whenever(userInput.address).thenReturn("abc")
//        whenever(userInput.amount).thenReturn(one)
//        whenever(userInput.feePriority).thenReturn(feePriority)
//
//        whenever(adapter.send(any(), any(), any())).thenReturn(Single.just(Unit))
//
//        interactor.send(userInput)
//
//        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
//
//        verify(delegate).didSend()
//    }
//
//    @Test
//    fun send_emptyAddress() {
//        whenever(userInput.address).thenReturn(null)
//
//        interactor.send(userInput)
//
//        verify(delegate).showError(argThat {
//            this is SendInteractor.SendError.NoAddress
//        })
//    }
//
//    @Test
//    fun send_emptyAmount() {
//        whenever(userInput.address).thenReturn("abc")
//        whenever(userInput.amount).thenReturn(zero)
//
//        interactor.send(userInput)
//
//        verify(delegate).showError(argThat {
//            this is SendInteractor.SendError.NoAmount
//        })
//    }
//
//    @Test
//    fun send_error() {
//        interactor.retrieveRate() // set rate
//
//        val exception = Exception("InsufficientAmount")
//
//        whenever(userInput.address).thenReturn("abc")
//        whenever(userInput.amount).thenReturn(one)
//        whenever(userInput.feePriority).thenReturn(feePriority)
//        whenever(adapter.send(any(), any(), any())).thenReturn(Single.error(exception))
//
//        interactor.send(userInput)
//
//        verify(delegate).showError(exception)
//    }
//
//    @Test
//    fun stateForUserInput_setFees_asZero() {
//        val fee = BigDecimal("0.123")
//        val expectedFee = BigDecimal.ZERO
//        val rateValue = BigDecimal("0.1")
//        val expectedCurrencyFee = expectedFee * rateValue
//        val amount = BigDecimal.ZERO
//        val address = "address"
//
//        val input = SendModule.UserInput()
//        input.address = address
//        input.amount = amount
//        input.inputType = SendModule.InputType.COIN
//
//        whenever(rate.value).thenReturn(rateValue)
//        whenever(adapter.fee(amount, address, feePriority)).thenReturn(fee)
//        whenever(userInput.feePriority).thenReturn(feePriority)
//
//        interactor.retrieveRate()
//
//        val state = interactor.stateForUserInput(input)
//
//        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
//
//        Assert.assertEquals(CoinValue(coinCode, value = expectedFee), state.feeCoinValue)
//        Assert.assertEquals(CurrencyValue(currency, value = expectedCurrencyFee), state.feeCurrencyValue)
//    }
//
//    @Test
//    fun stateForUserInput_setCoinFee() {
//        val fee = BigDecimal("0.123")
//        val amount = BigDecimal(123)
//        val address = "address"
//
//        val input = SendModule.UserInput()
//        input.feePriority = feePriority
//        input.address = address
//        input.amount = amount
//        input.inputType = SendModule.InputType.COIN
//
//        whenever(adapter.fee(amount, address, feePriority)).thenReturn(fee)
//        val state = interactor.stateForUserInput(input)
//
//        Assert.assertEquals(state.feeCoinValue, CoinValue(coinCode, value = fee))
//    }
//
//    @Test
//    fun stateForUserInput_setCoinFee_InsufficientAmountError() {
//        val fee = BigDecimal("0.123")
//        val balance = BigDecimal(123)
//        val amount = BigDecimal(123)
//        val address = "address"
//
//        val input = SendModule.UserInput()
//        input.feePriority = feePriority
//        input.address = address
//        input.amount = amount
//        input.inputType = SendModule.InputType.COIN
//
//        val balanceMinusFee = balance - fee
//        val errors = mutableListOf(SendStateError.InsufficientAmount)
//        whenever(adapter.availableBalance(address, feePriority)).thenReturn(balanceMinusFee)
//        whenever(adapter.validate(amount, address, feePriority)).thenReturn(errors)
//        whenever(adapter.fee(amount, address, feePriority)).thenReturn(fee)
//
//        val state = interactor.stateForUserInput(input)
//
//        Assert.assertEquals(state.feeCoinValue, CoinValue(coinCode, value = fee))
//
//        val error = SendModule.AmountError.InsufficientBalance(SendModule.AmountInfo.CoinValueInfo(CoinValue(coinCode, balanceMinusFee)))
//        Assert.assertEquals(error, state.amountError)
//    }
//
//    @Test
//    fun stateForUserInput_setCurrencyFee() {
//        val fee = BigDecimal("0.123")
//        val amount = BigDecimal(123)
//        val address = "address"
//        val coinValue = amount.divide(rate.value, 8, RoundingMode.HALF_EVEN)
//
//        val input = SendModule.UserInput()
//        input.feePriority = feePriority
//        input.address = address
//        input.amount = amount
//        input.inputType = SendModule.InputType.CURRENCY
//
//        whenever(userInput.feePriority).thenReturn(feePriority)
//        whenever(adapter.fee(coinValue, address, feePriority)).thenReturn(fee)
//
//        interactor.retrieveRate()
//        val state = interactor.stateForUserInput(input)
//
//        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
//
//        Assert.assertEquals(state.feeCurrencyValue, CurrencyValue(currency, value = fee * rate.value))
//    }
//
//    @Test
//    fun stateForUserInput_setCurrencyFee_forErc20() {
//        val feeCoinCode = "ETH"
//        val fee = BigDecimal("0.000547")
//        val amount = BigDecimal(654)
//        val address = "address"
//        val coinValue = amount.divide(rate.value, 8, RoundingMode.HALF_EVEN)
//
//        val input = SendModule.UserInput()
//        input.feePriority = feePriority
//        input.address = address
//        input.amount = amount
//        input.inputType = SendModule.InputType.CURRENCY
//
//        whenever(adapter.feeCoinCode).thenReturn(feeCoinCode)
//        whenever(adapter.fee(coinValue, address, feePriority)).thenReturn(fee)
//
//        interactor.retrieveRate()
//        val state = interactor.stateForUserInput(input)
//        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
//
//        Assert.assertEquals(CurrencyValue(currency, value = fee * fiatFeeRate.value), state.feeCurrencyValue)
//    }
//
//    @Test
//    fun stateForUserInput_setCurrencyFee_InsufficientAmountError() {
//        val fee = BigDecimal("0.123")
//        val amount = BigDecimal(123)
//        val address = "address"
//        val coinValue = amount.divide(rate.value, 8, RoundingMode.HALF_EVEN)
//
//        val input = SendModule.UserInput()
//        input.feePriority = feePriority
//        input.address = address
//        input.amount = amount
//        input.inputType = SendModule.InputType.CURRENCY
//
//        val balanceMinusFee = balance - fee
//        val errors = mutableListOf(SendStateError.InsufficientAmount)
//        whenever(adapter.availableBalance(address, feePriority)).thenReturn(balanceMinusFee)
//        whenever(adapter.validate(coinValue, address, feePriority)).thenReturn(errors)
//        whenever(adapter.fee(coinValue, address, feePriority)).thenReturn(fee)
//
//        interactor.retrieveRate()
//        val state = interactor.stateForUserInput(input)
//
//        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
//
//        Assert.assertEquals(state.feeCurrencyValue, CurrencyValue(currency, value = fee * rate.value))
//
//        val balanceMinusFeeInCurrency = balanceMinusFee * rate.value
//        val error = SendModule.AmountError.InsufficientBalance(SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(currency, balanceMinusFeeInCurrency)))
//        Assert.assertEquals(error, state.amountError)
//    }
//
//    @Test
//    fun getTotalBalanceMinusFee_coin() {
//        val fee = BigDecimal("0.123")
//        val balanceAmount = BigDecimal("123")
//        val availableBalance = balanceAmount - fee
//        val address = "address"
//
//        val input = SendModule.UserInput()
//        input.feePriority = feePriority
//        input.address = address
//        input.inputType = SendModule.InputType.COIN
//
//        whenever(adapter.availableBalance(address, feePriority)).thenReturn(availableBalance)
//
//        val expectedBalanceMinusFee = BigDecimal("122.877")
//        val balanceMinusFee = interactor.getTotalBalanceMinusFee(input.inputType, input.address, feePriority)
//
//        Assert.assertEquals(expectedBalanceMinusFee, balanceMinusFee)
//    }
//
//    @Test
//    fun getTotalBalanceMinusFee_currencyWithSmallFee() {
//        val fee = BigDecimal("0.0000044")
//        val balanceAmount = BigDecimal("123")
//        val availableBalance = balanceAmount - fee
//        val address = "address"
//
//        val input = SendModule.UserInput()
//        input.address = address
//        input.inputType = SendModule.InputType.CURRENCY
//
//        whenever(adapter.balance).thenReturn(balanceAmount)
//        whenever(adapter.availableBalance(address, feePriority)).thenReturn(availableBalance)
//
//        interactor.retrieveRate()
//
//        val expectedBalanceMinusFee = BigDecimal("12.29999956")
//        val balanceMinusFee = interactor.getTotalBalanceMinusFee(input.inputType, input.address, feePriority)
//
//        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
//
//        Assert.assertEquals(expectedBalanceMinusFee, balanceMinusFee)
//    }
//
//    @Test
//    fun testState_FeeError_CoinType_InsufficientFeeBalance() {
//        val feeCoinCode = "ETH"
//        val erc20CoinCode = "TNT"
//        val amount = BigDecimal(123)
//        val address = "address"
//        val erc20Coin = Coin("trinitrotoluene", erc20CoinCode, type = CoinType.Erc20("some_address", 3))
//        val fee = BigDecimal("0.00004")
//        val expectedFeeError = SendModule.AmountError.Erc20FeeError(erc20CoinCode, CoinValue(feeCoinCode, fee))
//
//
//        val input = SendModule.UserInput()
//        input.amount = amount
//        input.feePriority = feePriority
//        input.address = address
//        input.inputType = SendModule.InputType.COIN
//
//        whenever(adapter.fee(amount, address, feePriority)).thenReturn(fee)
//        whenever(adapter.validate(amount, address, feePriority)).thenReturn(mutableListOf(SendStateError.InsufficientFeeBalance))
//        whenever(adapter.feeCoinCode).thenReturn(feeCoinCode)
//        val erc20Wallet = mock(Wallet::class.java)
//        whenever(erc20Wallet.coin).thenReturn(erc20Coin)
//        whenever(adapter.wallet).thenReturn(erc20Wallet)
//
//        val state = interactor.stateForUserInput(input)
//
//        Assert.assertEquals(expectedFeeError, state.feeError)
//    }
//
//    @Test
//    fun testState_numberOfDecimals_coin() {
//        val decimal = 8
//        val fee = BigDecimal("0.0000044")
//        val amount = BigDecimal(123)
//        val address = "address"
//        whenever(adapter.decimal).thenReturn(decimal)
//        whenever(adapter.fee(amount, address, feePriority)).thenReturn(fee)
//
//        val input = SendModule.UserInput()
//        input.amount = amount
//        input.feePriority = feePriority
//        input.address = address
//        input.inputType = SendModule.InputType.COIN
//
//        val state = interactor.stateForUserInput(input)
//
//        Assert.assertEquals(state.decimal, decimal)
//    }
//
//    @Test
//    fun testState_numberOfDecimals_fiat() {
//        val input = SendModule.UserInput()
//        input.address = "address"
//        input.inputType = SendModule.InputType.CURRENCY
//
//        val state = interactor.stateForUserInput(input)
//
//        Assert.assertEquals(state.decimal, fiatDecimal)
//    }
//
//    @Test
//    fun testState_numberOfDecimals_maxDecimal() {
//        val expectedDecimal = 8
//        val fee = BigDecimal("0.0000044")
//        val amount = BigDecimal(123)
//        val address = "address"
//
//        whenever(adapter.decimal).thenReturn(18)
//        whenever(adapter.fee(amount, address, feePriority)).thenReturn(fee)
//
//        val input = SendModule.UserInput()
//        input.amount = amount
//        input.feePriority = feePriority
//        input.address = address
//        input.inputType = SendModule.InputType.COIN
//        val state = interactor.stateForUserInput(input)
//
//        Assert.assertEquals(expectedDecimal, state.decimal)
//    }
//
//    @Test
//    fun testState_erc20_feeCoinCode() {
//        val decimal = 8
//        val fee = BigDecimal("0.0000044")
//        val amount = BigDecimal("0.044")
//        val address = "address"
//
//        val erc20CoinCode = "TNT"
//        val erc20Coin = Coin("trinitrotoluene", erc20CoinCode, type = CoinType.Erc20("some_address", 3))
//
//        val coinValue = CoinValue(feeCoinCode, fee)
//
//        whenever(adapter.decimal).thenReturn(decimal)
//        whenever(adapter.feeCoinCode).thenReturn(feeCoinCode)
//        whenever(adapter.fee(amount, address, feePriority)).thenReturn(fee)
//
//        val erc20Wallet = mock(Wallet::class.java)
//        whenever(erc20Wallet.coin).thenReturn(erc20Coin)
//        whenever(adapter.wallet).thenReturn(erc20Wallet)
//
//        val input = SendModule.UserInput()
//        input.address = address
//        input.amount = amount
//        input.feePriority = feePriority
//        input.inputType = SendModule.InputType.COIN
//
//        val state = interactor.stateForUserInput(input)
//
//        Assert.assertEquals(coinValue, state.feeCoinValue)
//    }
//
//    @Test
//    fun testFetchFeeRate() {
//        whenever(adapter.feeCoinCode).thenReturn(feeCoinCode)
//
//        interactor.retrieveRate()
//
//        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
//
//        verify(delegate).didFeeRateRetrieve()
//    }
//
//    @Test
//    fun testFetchFeeRate_noFeeCoinCode() {
//        val feeCoinCode = null
//
//        whenever(adapter.feeCoinCode).thenReturn(feeCoinCode)
//
//        interactor.retrieveRate()
//
//        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
//
//        verify(delegate, atLeastOnce()).didRateRetrieve(rate)
//    }
//
//}
