package bitcoin.wallet.modules.wallet

import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyType
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.entities.coins.bitcoinCash.BitcoinCash
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

    private val dollarCurrency = Currency().apply {
        code = "USD"
        symbol = "U+0024"
        name = "US Dollar"
        type = CurrencyType.FIAT
        codeNumeric = 840
    }

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
        val rates = mutableMapOf<Coin, CurrencyValue>()
        val progresses = mutableMapOf<String, BehaviorSubject<Double>>()
        val coin1 = Bitcoin()
        val coin2 = BitcoinCash()
        val bhvSubject: BehaviorSubject<Double> = BehaviorSubject.create()

        val expectedTotalBalance = CurrencyValue(dollarCurrency, 3500.0)

        val adapterId1 = "id1"
        val adapterId2 = "id2"
        coinValues[adapterId1] = CoinValue(coin1, 0.5)
        coinValues[adapterId2] = CoinValue(coin2, 1.0)
        progresses[adapterId1] = bhvSubject
        progresses[adapterId2] = bhvSubject
        rates[coin1] = CurrencyValue(dollarCurrency, 5000.0)
        rates[coin2] = CurrencyValue(dollarCurrency, 1000.0)

        presenter.didInitialFetch(coinValues, rates, progresses)

        val expectedViewItems = listOf(
                WalletBalanceViewItem(adapterId1, CoinValue(coin1, 0.5), CurrencyValue(dollarCurrency, 5000.0), CurrencyValue(dollarCurrency, 2500.0), bhvSubject),
                WalletBalanceViewItem(adapterId2, CoinValue(coin2, 1.0), CurrencyValue(dollarCurrency, 1000.0), CurrencyValue(dollarCurrency, 1000.0), bhvSubject)
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
