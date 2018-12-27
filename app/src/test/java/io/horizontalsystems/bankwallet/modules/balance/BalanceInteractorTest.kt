package io.horizontalsystems.bankwallet.modules.balance

import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
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

//        val wallets = listOf(wallet)
//
//        whenever(adapter.stateSubject).thenReturn(stateSubject)
//        whenever(adapter.balanceSubject).thenReturn(balanceSubject)
//        whenever(walletManager.walletsSubject).thenReturn(walletsSubject)
//        whenever(rateManager.subject).thenReturn(rateSubject)
//        whenever(currencyManager.subject).thenReturn(currencySubject)
//
//        whenever(wallet.adapter).thenReturn(adapter)
//        whenever(walletManager.wallets).thenReturn(wallets)
//
        interactor = BalanceInteractor(walletManager, rateStorage, currencyManager)
        interactor.delegate = delegate

        whenever(walletManager.walletsObservable).thenReturn(Flowable.empty())
        whenever(currencyManager.baseCurrencyObservable).thenReturn(Flowable.empty())
    }

    @Test
    fun initWallets_currencyUpdates() {
        val currency = mock(Currency::class.java)

        whenever(currencyManager.baseCurrencyObservable).thenReturn(Flowable.just(currency))

        interactor.initWallets()

        verify(delegate).didUpdateCurrency(currency)
    }

    @Test
    fun initWallets() {
        val wallets = listOf<Wallet>()

        whenever(walletManager.walletsObservable).thenReturn(Flowable.just(wallets))

        interactor.initWallets()

        verify(delegate).didUpdateWallets(wallets)
    }

    @Test
    fun initWallets_stateUpdate() {
        val coinCode = "coinCode"
        val state = AdapterState.Synced

        val wallet = mock(Wallet::class.java)
        val adapter = mock(IAdapter::class.java)

        val wallets = listOf(wallet)

        whenever(walletManager.walletsObservable).thenReturn(Flowable.just(wallets))
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

        whenever(walletManager.walletsObservable).thenReturn(Flowable.just(wallets1, wallets2))
        whenever(wallet1.coinCode).thenReturn(coinCode)
        whenever(wallet1.adapter).thenReturn(adapter1)
        whenever(adapter1.balanceObservable).thenReturn(Flowable.empty())
        whenever(adapter1.stateObservable).thenReturn(state1Observable.toFlowable(BackpressureStrategy.DROP))

        whenever(wallet2.coinCode).thenReturn(coinCode)
        whenever(wallet2.adapter).thenReturn(adapter2)
        whenever(adapter2.balanceObservable).thenReturn(Flowable.empty())
        whenever(adapter2.stateObservable).thenReturn(Flowable.just(state2))

        interactor.initWallets()

        state1Observable.onNext(state1Updated)

        verify(delegate).didUpdateState(coinCode, state1)
        verify(delegate).didUpdateState(coinCode, state2)
        verify(delegate, never()).didUpdateState(coinCode, state1Updated)
    }

    @Test
    fun initWallets_balanceUpdate() {
        val coinCode = "coinCode"
        val balance = 1.0

        val wallet = mock(Wallet::class.java)
        val adapter = mock(IAdapter::class.java)

        val wallets = listOf(wallet)

        whenever(walletManager.walletsObservable).thenReturn(Flowable.just(wallets))
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
        val balance = 1.0
        val balance1Updated = 1123.0
        val balance2 = 2.0

        val wallet = mock(Wallet::class.java)
        val adapter = mock(IAdapter::class.java)

        val wallet2 = mock(Wallet::class.java)
        val adapter2 = mock(IAdapter::class.java)

        val wallets = listOf(wallet)
        val walletsUpdated = listOf(wallet2)

        val balanceObservable1 = BehaviorSubject.createDefault(balance)

        whenever(walletManager.walletsObservable).thenReturn(Flowable.just(wallets, walletsUpdated))
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

        whenever(rateStorage.rateObservable(coinCode1, currencyCode)).thenReturn(Flowable.just(rate1, rate1Update))
        whenever(rateStorage.rateObservable(coinCode2, currencyCode)).thenReturn(Flowable.just(rate2))

        interactor.fetchRates(currencyCode, coinCodes)

        verify(delegate).didUpdateRate(rate1)
        verify(delegate).didUpdateRate(rate2)
        verify(delegate).didUpdateRate(rate1Update)

    }
}
