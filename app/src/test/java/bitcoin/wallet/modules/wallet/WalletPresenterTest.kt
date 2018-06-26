package bitcoin.wallet.modules.wallet

import bitcoin.wallet.entities.*
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class WalletPresenterTest {

    private val interactor = mock(WalletModule.IInteractor::class.java)
    private val view = mock(WalletModule.IView::class.java)
    private val router = mock(WalletModule.IRouter::class.java)

    private val presenter = WalletPresenter(interactor, router)

    @Before
    fun before() {
        presenter.view = view
    }

    @Test
    fun start() {
        presenter.viewDidLoad()

        verify(interactor).notifyWalletBalances()
    }

    @Test
    fun didFetchWalletBalances() {
        val walletBalances: List<WalletBalanceItem> = listOf(
                WalletBalanceItem(CoinValue(Bitcoin(), 0.5), 5000.0, DollarCurrency()),
                WalletBalanceItem(CoinValue(Ethereum(), 1.0), 1000.0, DollarCurrency())
        )

        val expectedViewItems = listOf(
                WalletBalanceViewItem(CoinValue(Bitcoin(), 0.5), CurrencyValue(DollarCurrency(), 5000.0), CurrencyValue(DollarCurrency(), 2500.0)),
                WalletBalanceViewItem(CoinValue(Ethereum(), 1.0), CurrencyValue(DollarCurrency(), 1000.0), CurrencyValue(DollarCurrency(), 1000.0))
        )

        val expectedTotalBalance = CurrencyValue(DollarCurrency(), 3500.0)

        presenter.didFetchWalletBalances(walletBalances)

        verify(view).showWalletBalances(expectedViewItems)
        verify(view).showTotalBalance(expectedTotalBalance)
    }

}
