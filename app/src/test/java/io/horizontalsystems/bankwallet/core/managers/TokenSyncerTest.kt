package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.ICoinStorage
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TokenSyncerTest {
    private val coinStorage = mock(ICoinStorage::class.java)
    private val networkManager = mock(INetworkManager::class.java)

    private val coinA = Coin("A", "A", CoinType.Erc20("a", 18))
    private val coinB = Coin("B", "B", CoinType.Erc20("b", 18))
    private val coinC = Coin("C", "C", CoinType.Erc20("c", 18))

    private lateinit var tokenSyncer: TokenSyncer

    @Before
    fun setup() {
        RxBaseTest.setup()

        tokenSyncer = TokenSyncer(networkManager, coinStorage)
    }

    @Test
    fun sync() {
        whenever(networkManager.getTokens())
                .thenReturn(Flowable.just(listOf()))
        whenever(coinStorage.allCoinsObservable())
                .thenReturn(Flowable.just(listOf()))
        whenever(coinStorage.enabledCoinsObservable())
                .thenReturn(Flowable.just(listOf()))

        tokenSyncer.sync()

        verify(networkManager).getTokens()
        verify(coinStorage).allCoinsObservable()
        verify(coinStorage).enabledCoinsObservable()

        // should not update with empty list
        verifyNoMoreInteractions(coinStorage)
    }

    @Test
    fun sync_withoutAnyCoins() {
        whenever(networkManager.getTokens())
                .thenReturn(Flowable.just(listOf(coinA, coinB, coinC)))

        whenever(coinStorage.enabledCoinsObservable())
                .thenReturn(Flowable.just(listOf()))

        whenever(coinStorage.allCoinsObservable())
                .thenReturn(Flowable.just(listOf()))

        tokenSyncer.sync()

        verify(coinStorage).update(listOf(coinA, coinB, coinC), listOf())
    }

    @Test
    fun sync_withoutEnabledCoins() {
        whenever(networkManager.getTokens())
                .thenReturn(Flowable.just(listOf(coinB, coinC)))

        whenever(coinStorage.enabledCoinsObservable())
                .thenReturn(Flowable.just(listOf()))

        whenever(coinStorage.allCoinsObservable())
                .thenReturn(Flowable.just(listOf(coinA)))

        tokenSyncer.sync()

        verify(coinStorage).update(listOf(coinB, coinC), listOf(coinA))
    }

    @Test
    fun sync_withEnabledCoins() {
        whenever(networkManager.getTokens())
                .thenReturn(Flowable.just(listOf(coinB, coinC)))

        whenever(coinStorage.enabledCoinsObservable())
                .thenReturn(Flowable.just(listOf(coinA)))

        whenever(coinStorage.allCoinsObservable())
                .thenReturn(Flowable.just(listOf(coinA)))

        tokenSyncer.sync()

        verify(coinStorage).update(listOf(coinB, coinC), listOf())
    }
}
