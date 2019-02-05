package io.horizontalsystems.bankwallet.modules.send

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal

class SendInteractorTest {

    private val delegate = mock(SendModule.IInteractorDelegate::class.java)
    private val localStorage = mock(ILocalStorage::class.java)
    private val clipboardManager = mock(IClipboardManager::class.java)
    private val appConfigProvider = mock(IAppConfigProvider::class.java)

    private val currencyManager = mock(ICurrencyManager::class.java)
    private val rateStorage = mock(IRateStorage::class.java)
    private val wallet = mock(Wallet::class.java)
    private val adapter = mock(IAdapter::class.java)

    private val currency = Currency("USD", symbol = "\u0024")
    private val coin = CoinCode()
    private val rate = mock(Rate::class.java)
    private val userInput = mock(SendModule.UserInput::class.java)
    private val balance = BigDecimal(123)
    private val zero = BigDecimal.ZERO
    private val one = BigDecimal.ONE
    private val fiatDecimal = 2
    private val maxDecimal = 8

    private lateinit var interactor: SendInteractor

    @Before
    fun setup() {
        RxBaseTest.setup()

        whenever(userInput.inputType).thenReturn(SendModule.InputType.COIN)
        whenever(rate.value).thenReturn(BigDecimal("0.1"))
        whenever(wallet.coinCode).thenReturn(coin)
        whenever(wallet.adapter).thenReturn(adapter)
        whenever(currencyManager.baseCurrency).thenReturn(currency)
        whenever(rateStorage.latestRateObservable(coin, currency.code)).thenReturn(Flowable.just(rate))
        whenever(adapter.balance).thenReturn(balance)
        whenever(adapter.decimal).thenReturn(fiatDecimal)
        whenever(appConfigProvider.fiatDecimal).thenReturn(fiatDecimal)
        whenever(appConfigProvider.maxDecimal).thenReturn(maxDecimal)

        interactor = SendInteractor(currencyManager, rateStorage, localStorage, clipboardManager, wallet, appConfigProvider)
        interactor.delegate = delegate
    }

    @Test
    fun defaultInputType_get() {
        val inputType = mock(SendModule.InputType::class.java)

        whenever(localStorage.sendInputType).thenReturn(inputType)

        Assert.assertEquals(inputType, interactor.defaultInputType)
    }

    @Test
    fun defaultInputType_getDefault() {
        whenever(localStorage.sendInputType).thenReturn(null)

        Assert.assertEquals(SendModule.InputType.COIN, interactor.defaultInputType)
    }

    @Test
    fun defaultInputType_set() {
        val inputType = mock(SendModule.InputType::class.java)

        interactor.defaultInputType = inputType

        verify(localStorage).sendInputType = inputType
    }

    @Test
    fun retrieveRate() {
        interactor.retrieveRate()

        verify(rateStorage).latestRateObservable(coin, currency.code)
    }

    @Test
    fun send_inCurrency() {
        interactor.retrieveRate() // set rate

        whenever(rate.value).thenReturn(BigDecimal(1024))
        whenever(userInput.inputType).thenReturn(SendModule.InputType.CURRENCY)
        whenever(userInput.address).thenReturn("abc")
        whenever(userInput.amount).thenReturn(one)
        whenever(adapter.decimal).thenReturn(8)

        interactor.send(userInput)

        val expectedAmountToSend = BigDecimal.valueOf(0.00097656) // 0.0009765625

        verify(adapter).send(eq("abc"), eq(expectedAmountToSend), any())
    }

    @Test
    fun send() {
        interactor.retrieveRate() // set rate

        whenever(userInput.address).thenReturn("abc")
        whenever(userInput.amount).thenReturn(one)

        whenever(adapter.send(any(), any(), any())).then {
            val completion = it.arguments[2] as (Throwable?) -> (Unit)
            completion.invoke(null)
        }

        interactor.send(userInput)

        verify(delegate).didSend()
    }

    @Test
    fun send_emptyAddress() {
        whenever(userInput.address).thenReturn(null)

        interactor.send(userInput)

        verify(delegate).didFailToSend(argThat {
            this is SendInteractor.SendError.NoAddress
        })
    }

    @Test
    fun send_emptyAmount() {
        whenever(userInput.address).thenReturn("abc")
        whenever(userInput.amount).thenReturn(zero)

        interactor.send(userInput)

        verify(delegate).didFailToSend(argThat {
            this is SendInteractor.SendError.NoAmount
        })
    }

    @Test
    fun send_error() {
        interactor.retrieveRate() // set rate

        val exception = Exception("InsufficientAmount")

        whenever(userInput.address).thenReturn("abc")
        whenever(userInput.amount).thenReturn(one)
        whenever(adapter.send(any(), any(), any())).then {
            val completion = it.arguments[2] as (Throwable?) -> Unit
            completion.invoke(exception)
        }

        interactor.send(userInput)

        verify(delegate).didFailToSend(exception)
    }

    @Test
    fun stateForUserInput_setCoinFee_asZero() {
        val expectedFee = BigDecimal.ZERO
        val input = SendModule.UserInput()
        input.address = "address"
        input.amount = BigDecimal.ZERO
        input.inputType = SendModule.InputType.COIN

        val state = interactor.stateForUserInput(input)

        Assert.assertEquals(state.feeCoinValue, CoinValue(coin, value= expectedFee))
    }

    @Test
    fun stateForUserInput_setCoinFee() {
        val fee = BigDecimal("0.123")
        val input = SendModule.UserInput()
        input.address = "address"
        input.amount = BigDecimal(123)
        input.inputType = SendModule.InputType.COIN

        whenever(adapter.fee(any(), any(), any())).thenReturn(fee)
        val state = interactor.stateForUserInput(input)

        Assert.assertEquals(state.feeCoinValue, CoinValue(coin, value= fee))
    }

    @Test
    fun stateForUserInput_setCoinFee_InsufficientAmountError() {
        val fee = BigDecimal("0.123")
        val input = SendModule.UserInput()
        input.address = "address"
        input.amount = BigDecimal(123)
        input.inputType = SendModule.InputType.COIN

        val insufficientAmountError = Error.InsufficientAmount(fee)
        whenever(adapter.fee(any(), any(), any())).thenThrow(insufficientAmountError)

        val state = interactor.stateForUserInput(input)

        Assert.assertEquals(state.feeCoinValue, CoinValue(coin, value= fee))

        val balanceMinusFee = balance - fee
        val error = SendModule.AmountError.InsufficientBalance(SendModule.AmountInfo.CoinValueInfo(CoinValue(wallet.coinCode, balanceMinusFee)))
        Assert.assertEquals(state.amountError, error)
    }

    @Test
    fun stateForUserInput_setCurrencyFee() {
        val fee = BigDecimal("0.123")
        val input = SendModule.UserInput()
        input.address = "address"
        input.amount = BigDecimal(123)
        input.inputType = SendModule.InputType.CURRENCY

        whenever(adapter.fee(any(), any(), any())).thenReturn(fee)

        interactor.retrieveRate()
        val state = interactor.stateForUserInput(input)

        Assert.assertEquals(state.feeCurrencyValue, CurrencyValue(currency, value= fee * rate.value))
    }

    @Test
    fun stateForUserInput_setCurrencyFee_InsufficientAmountError() {
        val fee = BigDecimal("0.123")
        val input = SendModule.UserInput()
        input.address = "address"
        input.amount = BigDecimal(123)
        input.inputType = SendModule.InputType.CURRENCY

        val insufficientAmountError = Error.InsufficientAmount(fee)
        whenever(adapter.fee(any(), any(), any())).thenThrow(insufficientAmountError)

        interactor.retrieveRate()
        val state = interactor.stateForUserInput(input)

        Assert.assertEquals(state.feeCurrencyValue, CurrencyValue(currency, value= fee * rate.value))

        val balanceMinusFee = (balance - fee) * rate.value
        val error = SendModule.AmountError.InsufficientBalance(SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(currency, balanceMinusFee)))
        Assert.assertEquals(state.amountError, error)
    }

    @Test
    fun getTotalBalanceMinusFee_coin() {
        val fee = BigDecimal("0.123")
        val input = SendModule.UserInput()
        input.address = "address"
        input.inputType = SendModule.InputType.COIN

        whenever(wallet.adapter.fee(any(), any(), any())).thenReturn(fee)

        val expectedBalanceMinusFee =  BigDecimal("122.877")
        val balanceMinusFee = interactor.getTotalBalanceMinusFee(input.inputType, input.address)

        Assert.assertEquals(expectedBalanceMinusFee, balanceMinusFee)
    }

    @Test
    fun getTotalBalanceMinusFee_currencyWithSmallFee() {
        val fee = BigDecimal("0.0000044")
        val input = SendModule.UserInput()
        val balanceAmount = BigDecimal("123")
        input.address = "address"
        input.inputType = SendModule.InputType.CURRENCY

        whenever(adapter.balance).thenReturn(balanceAmount)
        whenever(wallet.adapter.fee(any(), any(), any())).thenReturn(fee)

        interactor.retrieveRate()

        val expectedBalanceMinusFee = BigDecimal("12.29")
        val balanceMinusFee = interactor.getTotalBalanceMinusFee(input.inputType, input.address)

        Assert.assertEquals(expectedBalanceMinusFee, balanceMinusFee)
    }

    @Test
    fun convertedAmountForInputType_toCoinType() {
        interactor.retrieveRate()
        assertCurrencyConvertToCoin("12.5", "1.25")
        assertCurrencyConvertToCoin("1223.5", "122.35")
        assertCurrencyConvertToCoin("0.852", "0.0852")
    }

    @Test
    fun convertedAmountForInputType_toCurrencyType() {
        interactor.retrieveRate()
        assertCurrencyConvertToCurrency("12.5", "125")
        assertCurrencyConvertToCurrency("0.00778011", "0.0778011")
    }

    @Test
    fun testState_numberOfDecimals_coin() {
        val decimal = 8
        whenever(adapter.decimal).thenReturn(decimal)
        val input = SendModule.UserInput()
        val state = interactor.stateForUserInput(input)

        Assert.assertEquals(state.decimal, decimal)
    }

    @Test
    fun testState_numberOfDecimals_fiat() {
        val input = SendModule.UserInput()
        input.inputType = SendModule.InputType.CURRENCY

        val state = interactor.stateForUserInput(input)

        Assert.assertEquals(state.decimal, fiatDecimal)
    }

    @Test
    fun testState_numberOfDecimals_maxDecimal() {
        val expectedDecimal = 8

        whenever(adapter.decimal).thenReturn(18)

        val input = SendModule.UserInput()
        input.inputType = SendModule.InputType.COIN
        val state = interactor.stateForUserInput(input)

        Assert.assertEquals(expectedDecimal, state.decimal)
    }


    private fun assertCurrencyConvertToCoin(input: String, expected: String) {
        val inputDecimal = BigDecimal(input)
        val expectedDecimal = BigDecimal(expected)
        val converted = interactor.convertedAmountForInputType(SendModule.InputType.COIN, inputDecimal)
        Assert.assertEquals(expectedDecimal, converted)
    }

    private fun assertCurrencyConvertToCurrency(input: String, expected: String) {
        val inputDecimal = BigDecimal(input)
        val expectedDecimal = BigDecimal(expected)
        val converted = interactor.convertedAmountForInputType(SendModule.InputType.CURRENCY, inputDecimal)
        Assert.assertEquals(expectedDecimal, converted)
    }


}
