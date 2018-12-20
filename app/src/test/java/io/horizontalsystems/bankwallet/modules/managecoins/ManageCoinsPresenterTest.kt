package io.horizontalsystems.bankwallet.modules.managecoins

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ManageCoinsPresenterTest {

    private lateinit var presenter: ManageCoinsPresenter
    private val interactor = mock(ManageCoinsModule.IInteractor::class.java)
    private val router = mock(ManageCoinsModule.IRouter::class.java)
    private val view = mock(ManageCoinsModule.IView::class.java)
    private val state = mock(ManageCoinsModule.ManageCoinsPresenterState::class.java)

    private val bitCoin = Coin("Bitcoin", "BTC", CoinType.Bitcoin)
    private val bitCashCoin = Coin("Bitcoin Cash", "BCH", CoinType.BitcoinCash)
    private val ethereumCoin = Coin("Ethereum", "ETH", CoinType.Ethereum)
    private val enabledCoins = mutableListOf(bitCoin, ethereumCoin)
    private val disabledCoins = mutableListOf(bitCashCoin)
    private val allCoins = mutableListOf(bitCoin, ethereumCoin, bitCashCoin)

    @Before
    fun setUp() {
        presenter = ManageCoinsPresenter(interactor, router, state)
        presenter.view = view

        whenever(state.enabledCoins).thenReturn(enabledCoins)
        whenever(state.allCoins).thenReturn(mutableListOf(bitCoin, ethereumCoin, bitCashCoin))
        whenever(state.disabledCoins).thenReturn(disabledCoins)
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()
        verify(interactor).loadCoins()
    }

    @Test
    fun didLoadCoins() {
        presenter.didLoadCoins(allCoins, enabledCoins)
        verify(view).showCoins(enabledCoins, disabledCoins)
    }

    @Test
    fun enableCoin() {
        presenter.enableCoin(bitCashCoin)
        verify(state).enable(bitCashCoin)
        verify(view).showCoins(enabledCoins, disabledCoins)
    }

    @Test
    fun disableCoin() {
        presenter.disableCoin(bitCoin)
        verify(state).disable(bitCoin)
        verify(view).showCoins(enabledCoins, disabledCoins)
    }

    @Test
    fun moveCoin() {
        presenter.moveCoin(ethereumCoin, 0)
        verify(state).move(ethereumCoin, 0)
        verify(view).showCoins(enabledCoins, disabledCoins)
    }

    @Test
    fun saveChanges() {
        presenter.saveChanges()
        verify(interactor).saveEnabledCoins(enabledCoins)
    }

    @Test
    fun didSave() {
        presenter.didSaveChanges()
        verify(router).close()
    }

    @Test
    fun didFailedToSaveEnabledCoins() {
        presenter.didFailedToSave()
        verify(view).showFailedToSaveError()
    }

}
