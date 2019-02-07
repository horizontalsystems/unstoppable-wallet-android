package io.horizontalsystems.bankwallet.modules.balance

import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class BalanceInteractorTest {

    private val delegate = mock(BalanceModule.IInteractorDelegate::class.java)

    private val adapterManager = mock(IAdapterManager::class.java)
    private val rateStorage = mock(IRateStorage::class.java)
    private val currencyManager = mock(ICurrencyManager::class.java)

    private lateinit var interactor: BalanceInteractor

    @Before
    fun setup() {
        RxBaseTest.setup()

        interactor = BalanceInteractor(adapterManager, rateStorage, currencyManager)
        interactor.delegate = delegate

        whenever(adapterManager.adaptersUpdatedSignal).thenReturn(Observable.empty())
        whenever(currencyManager.baseCurrencyUpdatedSignal).thenReturn(Observable.empty())
    }

    @Test
    fun initWallets_emptyAdapters() {
        val adapters = listOf<IAdapter>()
        val currency = mock(Currency::class.java)

        whenever(currencyManager.baseCurrency).thenReturn(currency)
        whenever(adapterManager.adapters).thenReturn(adapters)

        interactor.initAdapters()

        verify(delegate).didUpdateAdapters(adapters)
        verify(delegate).didUpdateCurrency(currency)
    }

    @Test
    fun initWallets_currencyUpdates() {
        val initialCurrency = mock(Currency::class.java)
        val updatedCurrency = mock(Currency::class.java)

        val currencyUpdatedSignal = PublishSubject.create<Unit>()

        whenever(currencyManager.baseCurrency).thenReturn(initialCurrency, updatedCurrency)
        whenever(currencyManager.baseCurrencyUpdatedSignal).thenReturn(currencyUpdatedSignal)

        interactor.initAdapters()

        currencyUpdatedSignal.onNext(Unit)

        inOrder(delegate).let { inOrder ->
            inOrder.verify(delegate).didUpdateCurrency(initialCurrency)
            inOrder.verify(delegate).didUpdateCurrency(updatedCurrency)
            inOrder.verifyNoMoreInteractions()
        }
    }

    @Test
    fun fetchRates() {
        val currencyCode = "USD"
        val coinCode1 = "BTC"
        val coinCode2 = "ETH"
        val coinCodes = listOf(coinCode1, coinCode2)

        val rate1 = mock(Rate::class.java)
        val rate1Update = mock(Rate::class.java)
        val rate2 = mock(Rate::class.java)

        whenever(rateStorage.latestRateObservable(coinCode1, currencyCode)).thenReturn(Flowable.just(rate1, rate1Update))
        whenever(rateStorage.latestRateObservable(coinCode2, currencyCode)).thenReturn(Flowable.just(rate2))

        interactor.fetchRates(currencyCode, coinCodes)

        verify(delegate).didUpdateRate(rate1)
        verify(delegate).didUpdateRate(rate2)
        verify(delegate).didUpdateRate(rate1Update)

    }
}
