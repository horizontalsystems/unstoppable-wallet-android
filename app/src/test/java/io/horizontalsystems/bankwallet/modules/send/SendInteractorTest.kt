package io.horizontalsystems.bankwallet.modules.send

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.Error
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Maybe
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class SendInteractorTest {

    private val delegate = mock(SendModule.IInteractorDelegate::class.java)
    private val clipboardManager = mock(IClipboardManager::class.java)

    private val currencyManager = mock(ICurrencyManager::class.java)
    private val rateManager = mock(RateManager::class.java)
    private val wallet = mock(Wallet::class.java)
    private val adapter = mock(IAdapter::class.java)

    private val currency = Currency("USD", symbol = "\u0024")
    private val coin = CoinCode()
    private val rate = Rate(coin, currency.code, 0.1, timestamp = System.currentTimeMillis())
    private val userInput = mock(SendModule.UserInput::class.java)
    private val balance = 123.0

    private lateinit var interactor: SendInteractor

    @Before
    fun setup() {
        RxBaseTest.setup()

        whenever(wallet.coinCode).thenReturn(coin)
        whenever(wallet.adapter).thenReturn(adapter)
        whenever(currencyManager.baseCurrency).thenReturn(currency)
        whenever(rateManager.rate(coin, currency.code)).thenReturn(Maybe.just(rate))
        whenever(adapter.balance).thenReturn(balance)

        interactor = SendInteractor(currencyManager, rateManager, clipboardManager, wallet)
        interactor.delegate = delegate
    }

    @Test
    fun retrieveRate() {
        interactor.retrieveRate()

        verify(rateManager).rate(coin, currency.code)
    }

    @Test
    fun send() {
        interactor.retrieveRate() // set rate

        whenever(userInput.address).thenReturn("abc")
        whenever(userInput.amount).thenReturn(1.0)

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
        whenever(userInput.amount).thenReturn(0.0)

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
        whenever(userInput.amount).thenReturn(1.0)
        whenever(adapter.send(any(), any(), any())).then {
            val completion = it.arguments[2] as (Throwable?) -> Unit
            completion.invoke(exception)
        }

        interactor.send(userInput)

        verify(delegate).didFailToSend(exception)
    }

    @Test
    fun stateForUserInput_setCoinFee() {
        val fee = 0.123
        val input = SendModule.UserInput()
        input.address = "address"
        input.amount = 123.0
        input.inputType = SendModule.InputType.COIN

        whenever(adapter.fee(any(), any(), any())).thenReturn(fee)
        val state = interactor.stateForUserInput(input)

        Assert.assertEquals(state.feeCoinValue, CoinValue(coin, value= fee))
    }

    @Test
    fun stateForUserInput_setCoinFee_InsufficientAmountError() {
        val fee = 0.123
        val input = SendModule.UserInput()
        input.address = "address"
        input.amount = 123.0
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
        val fee = 0.123
        val input = SendModule.UserInput()
        input.address = "address"
        input.amount = 123.0
        input.inputType = SendModule.InputType.CURRENCY

        whenever(adapter.fee(any(), any(), any())).thenReturn(fee)

        interactor.retrieveRate()
        val state = interactor.stateForUserInput(input)

        Assert.assertEquals(state.feeCurrencyValue, CurrencyValue(currency, value= fee * rate.value))
    }

    @Test
    fun stateForUserInput_setCurrencyFee_InsufficientAmountError() {
        val fee = 0.123
        val input = SendModule.UserInput()
        input.address = "address"
        input.amount = 123.0
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

}
