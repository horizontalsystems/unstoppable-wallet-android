package io.horizontalsystems.bankwallet.modules.managecoins

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IEnabledCoinStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.EnabledCoin
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ManageCoinsInteractorTest {

    private lateinit var interactor: ManageCoinsInteractor

    private val delegate = mock(ManageCoinsModule.IInteractorDelegate::class.java)
    private val coinManager = mock(ICoinManager::class.java)
    private val coinStorage = mock(IEnabledCoinStorage::class.java)

    private val bitCoin = Coin("Bitcoin", "BTC", CoinType.Bitcoin)
    private val bitCashCoin = Coin("Bitcoin Cash", "BCH", CoinType.BitcoinCash)
    private val ethereumCoin = Coin("Ethereum", "ETH", CoinType.Ethereum)
    private val coins = mutableListOf(bitCoin, ethereumCoin)

    private val enabledBitcoin = EnabledCoin("BTC", 0)
    private val enabledEthereum = EnabledCoin( "ETH", 1)
    private val enabledCoins = listOf(enabledBitcoin, enabledEthereum)
    private val allCoins = mutableListOf(bitCoin, ethereumCoin, bitCashCoin)

    @Before
    fun setUp() {
        RxBaseTest.setup()

        whenever(coinStorage.enabledCoinsObservable()).thenReturn(Flowable.just(enabledCoins))
        whenever(coinManager.allCoins).thenReturn(allCoins)

        interactor = ManageCoinsInteractor(coinManager, coinStorage)
        interactor.delegate = delegate
    }

    @Test
    fun loadCoins() {
        val expectedCoins = listOf(bitCoin, ethereumCoin)
        interactor.loadCoins()
        verify(delegate).didLoadAllCoins(allCoins)
        verify(delegate).didLoadEnabledCoins(expectedCoins)
    }

    @Test
    fun saveEnabledCoins() {
        interactor.saveEnabledCoins(coins)
        verify(coinStorage).save(enabledCoins)
        verify(delegate).didSaveChanges()
    }
}
