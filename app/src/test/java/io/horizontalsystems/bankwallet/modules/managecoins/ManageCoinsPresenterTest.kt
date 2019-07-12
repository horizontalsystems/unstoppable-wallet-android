package io.horizontalsystems.bankwallet.modules.managecoins

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.core.Wallet
import io.horizontalsystems.bankwallet.core.managers.WalletCreator
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.SyncMode
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ManageCoinsPresenterTest {

    private lateinit var presenter: ManageWalletsPresenter
    private val interactor = mock(ManageWalletsModule.IInteractor::class.java)
    private val router = mock(ManageWalletsModule.IRouter::class.java)
    private val walletCreator = mock(WalletCreator::class.java)
    private val view = mock(ManageWalletsModule.IView::class.java)
    private val state = mock(ManageWalletsModule.ManageWalletsPresenterState::class.java)

    private val bitCoin = Coin("Bitcoin", "BTC", CoinType.Bitcoin)
    private val bitCashCoin = Coin("Bitcoin Cash", "BCH", CoinType.BitcoinCash)
    private val ethereumCoin = Coin("Ethereum", "ETH", CoinType.Ethereum)
    private val disabledCoins = mutableListOf(bitCashCoin)

    private val wallet1 = com.nhaarman.mockito_kotlin.mock<Wallet> { on { coin } doReturn bitCoin }
    private val wallet2 = com.nhaarman.mockito_kotlin.mock<Wallet> { on { coin } doReturn ethereumCoin }
    private val wallet3 = com.nhaarman.mockito_kotlin.mock<Wallet> { on { coin } doReturn bitCashCoin }
    private val enabledCoins = mutableListOf(wallet1, wallet2)
    private val account = mock(Account::class.java)

    @Before
    fun setUp() {
        presenter = ManageWalletsPresenter(interactor, router, walletCreator, state)
        presenter.view = view

        whenever(state.enabledCoins).thenReturn(enabledCoins)
        whenever(state.allCoins).thenReturn(mutableListOf(bitCoin, ethereumCoin, bitCashCoin))
        whenever(state.disabledCoins).thenReturn(disabledCoins)
        whenever(account.defaultSyncMode).thenReturn(SyncMode.FAST)
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).load()
    }

    @Test
    fun disableCoin() {
        presenter.disableCoin(0)
        verify(state).disable(wallet1)
        verify(view).updateCoins()
    }

    @Test
    fun moveCoin() {
        presenter.moveCoin(1, 0)
        verify(state).move(wallet2, 0)
        verify(view).updateCoins()
    }

    @Test
    fun saveChanges() {
        presenter.saveChanges()
        verify(interactor).saveWallets(enabledCoins)
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

    @Test
    fun enabledItemForIndex() {
        Assert.assertEquals(wallet1, presenter.enabledItemForIndex(0))
    }

    @Test
    fun disabledItemForIndex() {
        Assert.assertEquals(bitCashCoin, presenter.disabledItemForIndex(0))
    }

    @Test
    fun enabledCoinsCount() {
        Assert.assertEquals(enabledCoins.size, presenter.enabledCoinsCount)
    }

    @Test
    fun disabledCoinsCount() {
        Assert.assertEquals(disabledCoins.size, presenter.disabledCoinsCount)
    }

    @Test
    fun onClear() {
        presenter.onClear()

        verify(interactor).clear()
    }

}
