package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.factories.WalletFactory
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class WalletManagerTest {
    private lateinit var manager: WalletManager

    private val coinManager = mock(CoinManager::class.java)
    private val wordsManager = mock(WordsManager::class.java)
    private val walletFactory = mock(WalletFactory::class.java)

    @Before
    fun setUp() {
        RxBaseTest.setup()
    }

    @Test
    fun initial() {
        val coin = mock(Coin::class.java)
        val coins = listOf(coin)
        val words = listOf("ad")
        val wallet = mock(Wallet::class.java)

        whenever(coinManager.coinsObservable).thenReturn(Flowable.just(coins))
        whenever(wordsManager.wordsObservable).thenReturn(Flowable.just(words))
        whenever(walletFactory.createWallet(coin, words)).thenReturn(wallet)

        manager = WalletManager(coinManager, wordsManager, walletFactory)

        manager.walletsObservable
                .test()
                .assertValue(listOf(wallet))
    }

    @Test
    fun emptyWords() {
        val coin = mock(Coin::class.java)
        val coins = listOf(coin)
        val words = listOf<String>()

        whenever(coinManager.coinsObservable).thenReturn(Flowable.just(coins))
        whenever(wordsManager.wordsObservable).thenReturn(Flowable.just(words))

        manager = WalletManager(coinManager, wordsManager, walletFactory)

        Thread.sleep(300)

        manager.walletsObservable
                .test()
                .assertValue(listOf())


    }

    @Test
    fun createOnlyNew() {
        val coinCode1 = "coinCode1"
        val coinCode2 = "coinCode2"
        val coin1 = mock(Coin::class.java)
        val coin2 = mock(Coin::class.java)
        val coins = listOf(coin1)
        val coinsUpdated = listOf(coin1, coin2)
        val words = listOf("ad")
        val wallet1 = mock(Wallet::class.java)
        val wallet2 = mock(Wallet::class.java)

        val coinsSubject = BehaviorSubject.createDefault(coins)

        whenever(coin1.code).thenReturn(coinCode1)
        whenever(wallet1.coinCode).thenReturn(coinCode1)
        whenever(coin2.code).thenReturn(coinCode2)
        whenever(wallet2.coinCode).thenReturn(coinCode2)

        whenever(coinManager.coinsObservable).thenReturn(coinsSubject.toFlowable(BackpressureStrategy.DROP))
        whenever(wordsManager.wordsObservable).thenReturn(Flowable.just(words))
        whenever(walletFactory.createWallet(coin1, words)).thenReturn(wallet1)
        whenever(walletFactory.createWallet(coin2, words)).thenReturn(wallet2)

        manager = WalletManager(coinManager, wordsManager, walletFactory)

        coinsSubject.onNext(coinsUpdated)

        verify(walletFactory).createWallet(coin1, words)
        verify(walletFactory).createWallet(coin2, words)

        manager.walletsObservable
                .test()
                .assertValue(listOf(wallet1, wallet2))
    }
}
