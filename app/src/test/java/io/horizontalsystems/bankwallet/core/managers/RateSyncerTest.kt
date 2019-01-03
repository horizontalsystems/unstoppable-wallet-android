package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.BehaviorSubject
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
    private val walletsSubject = BehaviorSubject.createDefault(wallets1)
    private val currencySubject = BehaviorSubject.createDefault(baseCurrency1)
    private val timerSubject = BehaviorSubject.createDefault(0L)
    private val networkStateObservable = BehaviorSubject.createDefault(true)

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

        whenever(walletManager.walletsObservable).thenReturn(walletsSubject.toFlowable(BackpressureStrategy.DROP))
        whenever(currencyManager.baseCurrencyObservable).thenReturn(currencySubject.toFlowable(BackpressureStrategy.DROP))
        whenever(networkAvailabilityManager.stateObservable).thenReturn(networkStateObservable.toFlowable(BackpressureStrategy.DROP))
    }

    @Test
    fun init() {
        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSubject.toFlowable(BackpressureStrategy.DROP))

        verify(syncer).refreshRates(coins1, currencyCode1)
        verifyNoMoreInteractions(syncer)
    }

    @Test
    fun init_noInternetConnection() {
        networkStateObservable.onNext(false)

        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSubject.toFlowable(BackpressureStrategy.DROP))

        verifyNoMoreInteractions(syncer)
    }

    @Test
    fun onConnectionEstablished() {
        networkStateObservable.onNext(false)

        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSubject.toFlowable(BackpressureStrategy.DROP))

        networkStateObservable.onNext(true)

        verify(syncer).refreshRates(coins1, currencyCode1)
    }

    @Test
    fun onInternetDisconnected() {
        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSubject.toFlowable(BackpressureStrategy.DROP))

        networkStateObservable.onNext(false)
        timerSubject.onNext(1)
        walletsSubject.onNext(wallets2)
        currencySubject.onNext(baseCurrency2)

        verify(syncer).refreshRates(coins1, currencyCode1)
        verifyNoMoreInteractions(syncer)
    }

    @Test
    fun onTimePassed() {
        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSubject.toFlowable(BackpressureStrategy.DROP))

        timerSubject.onNext(1)

        verify(syncer, times(2)).refreshRates(coins1, currencyCode1)
    }

    @Test
    fun onCoinsUpdate() {
        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSubject.toFlowable(BackpressureStrategy.DROP))

        walletsSubject.onNext(wallets2)

        val inOrder = inOrder(syncer)
        inOrder.verify(syncer).refreshRates(coins1, currencyCode1)
        inOrder.verify(syncer).refreshRates(coins2, currencyCode1)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun onBaseCurrencyUpdate() {
        rateSyncer = RateSyncer(syncer, walletManager, currencyManager, networkAvailabilityManager, timerSubject.toFlowable(BackpressureStrategy.DROP))

        currencySubject.onNext(baseCurrency2)

        val inOrder = inOrder(syncer)
        inOrder.verify(syncer).refreshRates(coins1, currencyCode1)
        inOrder.verify(syncer).refreshRates(coins1, currencyCode2)
        inOrder.verifyNoMoreInteractions()
    }

}
