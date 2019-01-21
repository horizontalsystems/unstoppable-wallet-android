package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ICoinStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Flowable
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CoinManagerTest {

    private lateinit var coinManager: CoinManager
    private lateinit var configProvider: IAppConfigProvider
    private lateinit var coinStorage: ICoinStorage

    private val bitCoin = Coin("Bitcoin", "BTC", CoinType.Bitcoin)
    private val bitCashCoin = Coin("Bitcoin Cash", "BCH", CoinType.BitcoinCash)
    private val ethereumCoin = Coin("Ethereum", "ETH", CoinType.Ethereum)
    private val enabledCoins = listOf(bitCoin, ethereumCoin)
    private val defaultCoins = mutableListOf(bitCashCoin)
    private val allCoins = listOf(bitCoin, ethereumCoin, bitCashCoin)

    @Before
    fun setUp() {
        RxBaseTest.setup()

        coinStorage = mock {
            on { enabledCoinsObservable() } doReturn Flowable.just(enabledCoins)
            on { allCoinsObservable() } doReturn Flowable.just(allCoins)
        }
        configProvider = mock {
            on { defaultCoins } doReturn defaultCoins
        }

        coinManager = CoinManager(configProvider, coinStorage)
    }

    @Test
    fun getAllCoinsObservable() {
        var fetchedFromDbCoins = listOf<Coin>()
        coinManager.allCoinsObservable.subscribe {
            fetchedFromDbCoins = it
        }
        val expectedCoins = allCoins
        Assert.assertTrue(expectedCoins.containsAll(fetchedFromDbCoins))
    }

    @Test
    fun updateSignal() {
        val testObserver = coinManager.coinsUpdatedSignal.test()

        coinManager.coins = enabledCoins

        testObserver.assertValueCount(1)
    }

    @Test
    fun enableDefaultCoins() {
        coinManager.enableDefaultCoins()
        verify(coinStorage).save(defaultCoins)
    }

}
