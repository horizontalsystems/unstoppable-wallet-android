package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class RateSyncerTest {

    private lateinit var rateSyncer: RateSyncer

    private val rateManager = mock(RateManager::class.java)
    private val adapterManager = mock(IAdapterManager::class.java)
    private val currencyManager = mock(ICurrencyManager::class.java)
    private val networkAvailabilityManager = mock(NetworkAvailabilityManager::class.java)

    private val coinCodes1 = listOf("BTC", "ETH", "BCH")
    private val coinCodes2 = listOf("BTC", "XRP")

    private val currencyCode1 = "EUR"
    private val currencyCode2 = "USD"
    private lateinit var adapters1: List<IAdapter>
    private lateinit var adapters2: List<IAdapter>
    private val baseCurrency1 = mock(Currency::class.java)
    private val baseCurrency2 = mock(Currency::class.java)
    private val adaptersUpdatedSignal = PublishSubject.create<Unit>()
    private val baseCurrencyUpdatedSignal = PublishSubject.create<Unit>()
    private val timerSignal = BehaviorSubject.createDefault(Unit)
    private val networkAvailabilitySignal = PublishSubject.create<Unit>()

    @Before
    fun setup() {
        RxBaseTest.setup()

        adapters1 = coinCodes1.map { coinCode ->
            val coin = mock(Coin::class.java)
            val adapter = mock(IAdapter::class.java)

            whenever(coin.code).thenReturn(coinCode)
            whenever(adapter.coin).thenReturn(coin)

            adapter
        }

        adapters2 = coinCodes2.map { coinCode ->
            val coin = mock(Coin::class.java)
            val adapter = mock(IAdapter::class.java)

            whenever(coin.code).thenReturn(coinCode)
            whenever(adapter.coin).thenReturn(coin)

            adapter
        }

        whenever(baseCurrency1.code).thenReturn(currencyCode1)
        whenever(baseCurrency2.code).thenReturn(currencyCode2)

        whenever(adapterManager.adaptersUpdatedSignal).thenReturn(adaptersUpdatedSignal)
        whenever(currencyManager.baseCurrencyUpdatedSignal).thenReturn(baseCurrencyUpdatedSignal)
        whenever(networkAvailabilityManager.networkAvailabilitySignal).thenReturn(networkAvailabilitySignal)

        whenever(networkAvailabilityManager.isConnected).thenReturn(true)
        whenever(adapterManager.adapters).thenReturn(adapters1)
        whenever(currencyManager.baseCurrency).thenReturn(baseCurrency1)
    }

    @Test
    fun init() {
        rateSyncer = RateSyncer(rateManager, adapterManager, currencyManager, networkAvailabilityManager, timerSignal)

        verify(rateManager).refreshLatestRates(coinCodes1, currencyCode1)
        verifyNoMoreInteractions(rateManager)
    }

    @Test
    fun init_noInternetConnection() {
        whenever(networkAvailabilityManager.isConnected).thenReturn(false)

        rateSyncer = RateSyncer(rateManager, adapterManager, currencyManager, networkAvailabilityManager, timerSignal)

        verifyNoMoreInteractions(rateManager)
    }

    @Test
    fun onConnectionEstablished() {
        whenever(networkAvailabilityManager.isConnected).thenReturn(false, true)

        rateSyncer = RateSyncer(rateManager, adapterManager, currencyManager, networkAvailabilityManager, timerSignal)

        networkAvailabilitySignal.onNext(Unit)

        verify(rateManager).refreshLatestRates(coinCodes1, currencyCode1)
    }

    @Test
    fun onInternetDisconnected() {
        whenever(networkAvailabilityManager.isConnected).thenReturn(true, false)

        rateSyncer = RateSyncer(rateManager, adapterManager, currencyManager, networkAvailabilityManager, timerSignal)

        verify(rateManager).refreshLatestRates(coinCodes1, currencyCode1)

        networkAvailabilitySignal.onNext(Unit)
        timerSignal.onNext(Unit)
        adaptersUpdatedSignal.onNext(Unit)
        baseCurrencyUpdatedSignal.onNext(Unit)

        verifyNoMoreInteractions(rateManager)
    }

    @Test
    fun onTimePassed() {
        rateSyncer = RateSyncer(rateManager, adapterManager, currencyManager, networkAvailabilityManager, timerSignal)

        timerSignal.onNext(Unit)

        verify(rateManager, times(2)).refreshLatestRates(coinCodes1, currencyCode1)
    }

    @Test
    fun onCoinsUpdate() {
        whenever(adapterManager.adapters).thenReturn(adapters1, adapters2)

        rateSyncer = RateSyncer(rateManager, adapterManager, currencyManager, networkAvailabilityManager, timerSignal)

        adaptersUpdatedSignal.onNext(Unit)

        val inOrder = inOrder(rateManager)
        inOrder.verify(rateManager).refreshLatestRates(coinCodes1, currencyCode1)
        inOrder.verify(rateManager).refreshLatestRates(coinCodes2, currencyCode1)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun onBaseCurrencyUpdate() {
        whenever(currencyManager.baseCurrency).thenReturn(baseCurrency1, baseCurrency2)

        rateSyncer = RateSyncer(rateManager, adapterManager, currencyManager, networkAvailabilityManager, timerSignal)

        baseCurrencyUpdatedSignal.onNext(Unit)

        val inOrder = inOrder(rateManager)
        inOrder.verify(rateManager).refreshLatestRates(coinCodes1, currencyCode1)
        inOrder.verify(rateManager).refreshLatestRates(coinCodes1, currencyCode2)
    }

}
