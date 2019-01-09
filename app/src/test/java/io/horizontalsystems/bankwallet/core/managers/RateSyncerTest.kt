package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class RateSyncerTest {

    private lateinit var rateSyncer: RateSyncer

    private val syncer = mock(RateManager::class.java)
    private val walletManager = mock(IWalletManager::class.java)
    private val currencyManager = mock(ICurrencyManager::class.java)
    private val networkAvailabilityManager = mock(NetworkAvailabilityManager::class.java)

    private val coins1 = listOf("BTC", "ETH", "BCH")
    private val coins2 = listOf("BTC", "XRP")
    private val currencyCode1 = "EUR"
    private val currencyCode2 = "USD"
    private val wallets1 = listOf(mock(Wallet::class.java), mock(Wallet::class.java), mock(Wallet::class.java))
    private val wallets2 = listOf(mock(Wallet::class.java), mock(Wallet::class.java))
    private val baseCurrency1 = mock(Currency::class.java)
    private val baseCurrency2 = mock(Currency::class.java)
    private val walletsUpdatedSignal = PublishSubject.create<Unit>()
    private val baseCurrencyUpdatedSignal = PublishSubject.create<Unit>()
    private val timerSignal = BehaviorSubject.createDefault(Unit)
    private val networkAvailabilitySignal = PublishSubject.create<Unit>()

    @Before
    fun setup() {
        RxBaseTest.setup()

        wallets1.forEachIndexed { index, wallet ->
            whenever(wallet.coinCode).thenReturn(coins1[index])
        }
        wallets2.forEachIndexed { index, wallet ->
            whenever(wallet.coinCode).thenReturn(coins2[index])
        }
        whenever(baseCurrency1.code).thenReturn(currencyCode1)
        whenever(baseCurrency2.code).thenReturn(currencyCode2)

        whenever(walletManager.walletsUpdatedSignal).thenReturn(walletsUpdatedSignal)
        whenever(currencyManager.baseCurrencyUpdatedSignal).thenReturn(baseCurrencyUpdatedSignal)
        whenever(networkAvailabilityManager.networkAvailabilitySignal).thenReturn(networkAvailabilitySignal)

        whenever(networkAvailabilityManager.isConnected).thenReturn(true)
        whenever(walletManager.wallets).thenReturn(wallets1)
        whenever(currencyManager.baseCurrency).thenReturn(baseCurrency1)
    }

    @Test
    fun init() {
        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSignal)

        verify(syncer).refreshRates(coins1, currencyCode1)
        verifyNoMoreInteractions(syncer)
    }

    @Test
    fun init_noInternetConnection() {
        whenever(networkAvailabilityManager.isConnected).thenReturn(false)

        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSignal)

        verifyNoMoreInteractions(syncer)
    }

    @Test
    fun onConnectionEstablished() {
        whenever(networkAvailabilityManager.isConnected).thenReturn(false, true)

        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSignal)

        networkAvailabilitySignal.onNext(Unit)

        verify(syncer).refreshRates(coins1, currencyCode1)
    }

    @Test
    fun onInternetDisconnected() {
        whenever(networkAvailabilityManager.isConnected).thenReturn(true, false)

        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSignal)

        verify(syncer).refreshRates(coins1, currencyCode1)

        networkAvailabilitySignal.onNext(Unit)
        timerSignal.onNext(Unit)
        walletsUpdatedSignal.onNext(Unit)
        baseCurrencyUpdatedSignal.onNext(Unit)

        verifyNoMoreInteractions(syncer)
    }

    @Test
    fun onTimePassed() {
        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSignal)

        timerSignal.onNext(Unit)

        verify(syncer, times(2)).refreshRates(coins1, currencyCode1)
    }

    @Test
    fun onCoinsUpdate() {
        whenever(walletManager.wallets).thenReturn(wallets1, wallets2)

        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSignal)

        walletsUpdatedSignal.onNext(Unit)

        val inOrder = inOrder(syncer)
        inOrder.verify(syncer).refreshRates(coins1, currencyCode1)
        inOrder.verify(syncer).refreshRates(coins2, currencyCode1)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun onBaseCurrencyUpdate() {
        whenever(currencyManager.baseCurrency).thenReturn(baseCurrency1, baseCurrency2)

        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSignal)

        baseCurrencyUpdatedSignal.onNext(Unit)

        val inOrder = inOrder(syncer)
        inOrder.verify(syncer).refreshRates(coins1, currencyCode1)
        inOrder.verify(syncer).refreshRates(coins1, currencyCode2)
        inOrder.verifyNoMoreInteractions()
    }

}
