package bitcoin.wallet.modules.wallet

import bitcoin.wallet.entities.*
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import io.reactivex.subjects.BehaviorSubject
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

        verify(interactor).checkIfPinSet()
        verify(interactor).notifyWalletBalances()
    }

    @Test
    fun updateView() {

        val coinValues = mutableMapOf<String, CoinValue>()
        val rates = mutableMapOf<String, Double>()
        val progresses = mutableMapOf<String, BehaviorSubject<Double>>()
        val currency: Currency = DollarCurrency()
        val coin1 = Bitcoin()
        val coin2 = Ethereum()
        val bhvSubject: BehaviorSubject<Double> = BehaviorSubject.create()

        val expectedTotalBalance = CurrencyValue(DollarCurrency(), 3500.0)

        val adapterId1 = "id1"
        val adapterId2 = "id2"
        coinValues[adapterId1] = CoinValue(coin1, 0.5)
        coinValues[adapterId2] = CoinValue(coin2, 1.0)
        progresses[adapterId1] = bhvSubject
        progresses[adapterId2] = bhvSubject
        rates["BTC"] = 5000.0
        rates["ETH"] = 1000.0

        presenter.didInitialFetch(coinValues, rates, progresses, currency)

        val expectedViewItems = listOf(
                WalletBalanceViewItem(adapterId1, CoinValue(coin1, 0.5), CurrencyValue(DollarCurrency(), 5000.0), CurrencyValue(DollarCurrency(), 2500.0), bhvSubject),
                WalletBalanceViewItem(adapterId2, CoinValue(coin2, 1.0), CurrencyValue(DollarCurrency(), 1000.0), CurrencyValue(DollarCurrency(), 1000.0), bhvSubject)
        )

        verify(view).showWalletBalances(expectedViewItems)
        verify(view).showTotalBalance(expectedTotalBalance)
    }

    @Test
    fun onPinNotSet() {
        presenter.onPinNotSet()

        verify(router).navigateToSetPin()
    }

}
