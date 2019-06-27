package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IEnabledCoinStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test

class CoinManagerTest {

    private lateinit var coinManager: WalletManager
    private lateinit var configProvider: IAppConfigProvider
    private lateinit var enabledCoinStorage: IEnabledCoinStorage

    private val bitCoin = Coin("Bitcoin", "BTC", CoinType.Bitcoin)
    private val ethereumCoin = Coin("Ethereum", "ETH", CoinType.Ethereum)
    private val coins = listOf(bitCoin, ethereumCoin)

    private val bitcoinE = EnabledWallet("BTC", 1)
    private val bitCashCoinE = EnabledWallet("BCH", 0)
    private val enabledCoins = listOf(bitcoinE, bitCashCoinE)
    private val defaultCoins = listOf("BCH")

    @Before
    fun setUp() {
        RxBaseTest.setup()

        enabledCoinStorage = mock {
            on { enabledCoinsObservable() } doReturn Flowable.just(enabledCoins)
        }
        configProvider = mock {
            on { defaultCoinCodes } doReturn defaultCoins
        }

        coinManager = WalletManager(configProvider, enabledCoinStorage)
    }

    @Test
    fun updateSignal() {
        val testObserver = coinManager.walletsUpdatedSignal.test()

        coinManager.wallets = coins

        testObserver.assertValueCount(1)
    }

    @Test
    fun enableDefaultCoins() {
        coinManager.enableDefaultWallets()
        verify(enabledCoinStorage).save(listOf(bitCashCoinE))
    }

}
