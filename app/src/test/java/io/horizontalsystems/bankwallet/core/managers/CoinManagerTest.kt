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
    private val erc20tokens = mutableListOf(mock<Coin>(), mock<Coin>())

    @Before
    fun setUp() {
        RxBaseTest.setup()

        coinStorage = mock {
            on { enabledCoinsObservable() } doReturn Flowable.just(enabledCoins)
        }
        configProvider = mock {
            on { defaultCoins } doReturn defaultCoins
            on { erc20tokens } doReturn erc20tokens
        }

        coinManager = CoinManager(configProvider, coinStorage)
    }

    @Test
    fun allCoins() {
        Assert.assertArrayEquals((defaultCoins + erc20tokens).toTypedArray(), coinManager.allCoins.toTypedArray())
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
