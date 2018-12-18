package io.horizontalsystems.bankwallet.modules.send

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Maybe
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class SendInteractorTest {

    private val delegate = mock(SendModule.IInteractorDelegate::class.java)
    private val clipboardManager = mock(IClipboardManager::class.java)

    private val currencyManager = mock(CurrencyManager::class.java)
    private val rateManager = mock(RateManager::class.java)
    private val wallet = mock(Wallet::class.java)
    private val adapter = mock(IAdapter::class.java)

    private val currency = Currency("USD", symbol = "\u0024")
    private val coin = CoinCode()
    private val rate = Rate(coin, currency.code, 0.1, timestamp = System.currentTimeMillis())
    private val userInput = mock(SendModule.UserInput::class.java)

    private lateinit var interactor: SendInteractor

    @Before
    fun setup() {
        RxBaseTest.setup()

        whenever(wallet.coinCode).thenReturn(coin)
        whenever(wallet.adapter).thenReturn(adapter)
        whenever(currencyManager.baseCurrency).thenReturn(currency)
        whenever(rateManager.rate(coin, currency.code)).thenReturn(Maybe.just(rate))

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

}
