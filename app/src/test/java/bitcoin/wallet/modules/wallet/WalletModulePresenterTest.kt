package bitcoin.wallet.modules.wallet

import bitcoin.wallet.entities.Coin
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class WalletModulePresenterTest {

    private val presenter = WalletModulePresenter()
    private val interactor = mock(WalletModule.IInteractor::class.java)
    private val view = mock(WalletModule.IView::class.java)

    @Before
    fun before() {
        presenter.interactor = interactor
        presenter.view = view
    }

    @Test
    fun start() {
        presenter.start()

        verify(interactor).retrieveInitialData()
    }

    @Test
    fun didCoinItemsRetrieved() {
        val coinItems = listOf<Coin>()

        presenter.didCoinItemsRetrieved(coinItems)

        verify(view).showCoinItems(coinItems)
    }

    @Test
    fun didTotalBalanceRetrieved() {

        presenter.didTotalBalanceRetrieved(111.23)

        verify(view).showTotalBalance(111.23)
    }
}