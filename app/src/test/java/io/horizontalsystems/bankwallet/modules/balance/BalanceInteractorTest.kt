package io.horizontalsystems.bankwallet.modules.balance

import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class BalanceInteractorTest {

    private val delegate = mock(BalanceModule.IInteractorDelegate::class.java)

    private val walletManager = mock(IWalletManager::class.java)
    private val rateStorage = mock(IRateStorage::class.java)
    private val currencyManager = mock(ICurrencyManager::class.java)
    private val wallet = mock(Wallet::class.java)
    private val adapter = mock(IAdapter::class.java)

    //
    // PublishSubjects
    //
    private val walletsSubject = PublishSubject.create<List<Wallet>>()
    private val balanceSubject = PublishSubject.create<Double>()
    private val stateSubject = PublishSubject.create<AdapterState>()
    private val rateSubject = PublishSubject.create<Boolean>()
    private val currencySubject = PublishSubject.create<Currency>()

    private lateinit var interactor: BalanceInteractor

    @Before
    fun setup() {
        RxBaseTest.setup()

        interactor = BalanceInteractor(walletManager, rateStorage, currencyManager)
        interactor.delegate = delegate

        whenever(walletManager.walletsUpdatedSignal).thenReturn(Observable.empty())
        whenever(currencyManager.baseCurrencyUpdatedSignal).thenReturn(Observable.empty())
    }

    @Test
    fun initWallets() {
        val wallets = listOf<Wallet>()
        val currency = mock(Currency::class.java)

        whenever(currencyManager.baseCurrency).thenReturn(currency)
        whenever(walletManager.wallets).thenReturn(wallets)

        interactor.initWallets()

        verify(delegate).didUpdateWallets(wallets)
        verify(delegate).didUpdateCurrency(currency)
    }

    @Test
    fun initWallets_currencyUpdates() {
        val initialCurrency = mock(Currency::class.java)
        val updatedCurrency = mock(Currency::class.java)

        val currencyUpdatedSignal = PublishSubject.create<Unit>()

        whenever(currencyManager.baseCurrency).thenReturn(initialCurrency, updatedCurrency)
        whenever(currencyManager.baseCurrencyUpdatedSignal).thenReturn(currencyUpdatedSignal)

        interactor.initWallets()

        currencyUpdatedSignal.onNext(Unit)

        inOrder(delegate).let { inOrder ->
            inOrder.verify(delegate).didUpdateCurrency(initialCurrency)
            inOrder.verify(delegate).didUpdateCurrency(updatedCurrency)
            inOrder.verifyNoMoreInteractions()
        }
    }

    @Test
    fun initWallets_stateUpdate() {
        val coinCode = "coinCode"
        val state = AdapterState.Synced

        val wallet = mock(Wallet::class.java)
        val adapter = mock(IAdapter::class.java)

        val wallets = listOf(wallet)

        whenever(walletManager.wallets).thenReturn(wallets)
        whenever(wallet.coinCode).thenReturn(coinCode)
        whenever(wallet.adapter).thenReturn(adapter)
        whenever(adapter.balanceObservable).thenReturn(Flowable.empty())
        whenever(adapter.stateObservable).thenReturn(Flowable.just(state))

        interactor.initWallets()

        verify(delegate).didUpdateState(coinCode, state)
    }

    @Test
    fun initWallets_stateUpdate_twice() {
        val coinCode = "coinCode"
        val state1 = AdapterState.NotSynced
        val state1Updated = AdapterState.Syncing(BehaviorSubject.create())
        val state2 = AdapterState.Synced

        val wallet1 = mock(Wallet::class.java)
        val adapter1 = mock(IAdapter::class.java)

        val wallet2 = mock(Wallet::class.java)
        val adapter2 = mock(IAdapter::class.java)

        val wallets1 = listOf(wallet1)
        val wallets2 = listOf(wallet2)

        val state1Observable: BehaviorSubject<AdapterState> = BehaviorSubject.createDefault(state1)
        val walletsUpdatedSignal = PublishSubject.create<Unit>()

        whenever(walletManager.wallets).thenReturn(wallets1, wallets2)
        whenever(walletManager.walletsUpdatedSignal).thenReturn(walletsUpdatedSignal)
        whenever(wallet1.coinCode).thenReturn(coinCode)
        whenever(wallet1.adapter).thenReturn(adapter1)
        whenever(adapter1.balanceObservable).thenReturn(Flowable.empty())
        whenever(adapter1.stateObservable).thenReturn(state1Observable.toFlowable(BackpressureStrategy.DROP))

        whenever(wallet2.coinCode).thenReturn(coinCode)
        whenever(wallet2.adapter).thenReturn(adapter2)
        whenever(adapter2.balanceObservable).thenReturn(Flowable.empty())
        whenever(adapter2.stateObservable).thenReturn(Flowable.just(state2))

        interactor.initWallets()

        verify(delegate).didUpdateState(coinCode, state1)

        walletsUpdatedSignal.onNext(Unit)

        verify(delegate).didUpdateState(coinCode, state2)

        state1Observable.onNext(state1Updated)

        verify(delegate, never()).didUpdateState(coinCode, state1Updated)
    }

    @Test
    fun initWallets_balanceUpdate() {
        val coinCode = "coinCode"
        val balance = 1.toBigDecimal()

        val wallet = mock(Wallet::class.java)
        val adapter = mock(IAdapter::class.java)

        val wallets = listOf(wallet)

        whenever(walletManager.wallets).thenReturn(wallets)
        whenever(wallet.coinCode).thenReturn(coinCode)
        whenever(wallet.adapter).thenReturn(adapter)
        whenever(adapter.balanceObservable).thenReturn(Flowable.just(balance))
        whenever(adapter.stateObservable).thenReturn(Flowable.empty())

        interactor.initWallets()

        verify(delegate).didUpdateBalance(coinCode, balance)
    }

    @Test
    fun initWallets_balanceUpdate_twice() {
        val coinCode = "coinCode"
        val balance = 1.toBigDecimal()
        val balance1Updated = 1123.toBigDecimal()
        val balance2 = 2.toBigDecimal()

        val wallet = mock(Wallet::class.java)
        val adapter = mock(IAdapter::class.java)

        val wallet2 = mock(Wallet::class.java)
        val adapter2 = mock(IAdapter::class.java)

        val wallets = listOf(wallet)
        val walletsUpdated = listOf(wallet2)

        val balanceObservable1 = BehaviorSubject.createDefault(balance)
        val walletsUpdatedSignal = PublishSubject.create<Unit>()

        whenever(walletManager.wallets).thenReturn(wallets, walletsUpdated)
        whenever(walletManager.walletsUpdatedSignal).thenReturn(walletsUpdatedSignal)
        whenever(wallet.coinCode).thenReturn(coinCode)
        whenever(wallet.adapter).thenReturn(adapter)
        whenever(wallet2.coinCode).thenReturn(coinCode)
        whenever(wallet2.adapter).thenReturn(adapter2)
        whenever(adapter.balanceObservable).thenReturn(balanceObservable1.toFlowable(BackpressureStrategy.DROP))
        whenever(adapter.stateObservable).thenReturn(Flowable.empty())
        whenever(adapter2.balanceObservable).thenReturn(Flowable.just(balance2))
        whenever(adapter2.stateObservable).thenReturn(Flowable.empty())

        interactor.initWallets()

        verify(delegate).didUpdateBalance(coinCode, balance)

        walletsUpdatedSignal.onNext(Unit)

        verify(delegate).didUpdateBalance(coinCode, balance2)

        balanceObservable1.onNext(balance1Updated)

        verify(delegate, never()).didUpdateBalance(coinCode, balance1Updated)
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
