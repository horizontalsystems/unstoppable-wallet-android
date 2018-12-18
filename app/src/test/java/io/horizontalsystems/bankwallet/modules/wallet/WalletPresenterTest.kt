package io.horizontalsystems.bankwallet.modules.wallet

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Maybe
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class WalletPresenterTest {

    private val interactor = mock(WalletModule.IInteractor::class.java)
    private val view = mock(WalletModule.IView::class.java)
    private val router = mock(WalletModule.IRouter::class.java)

    private val wallet = mock(Wallet::class.java)
    private val adapter = mock(IAdapter::class.java)
    private val coin: CoinCode = "BTC"

    private lateinit var presenter: WalletPresenter

    @Before
    fun setup() {
        RxBaseTest.setup()

        val currency = Currency("USD", "\u0024")
        val rate = Rate(coin, "USD", 0.1, System.currentTimeMillis())
        val wallets = listOf(wallet)
        val state = AdapterState.Synced

        whenever(wallet.coinCode).thenReturn(coin)
        whenever(wallet.adapter).thenReturn(adapter)
        whenever(wallet.adapter.state).thenReturn(state)
        whenever(interactor.baseCurrency).thenReturn(currency)
        whenever(interactor.wallets).thenReturn(wallets)
        whenever(interactor.rate(any())).thenReturn(Maybe.just(rate))

        presenter = WalletPresenter(interactor, router)
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).loadWallets()

        verify(view).setTitle(R.string.Balance_Title)
        verify(view).updateBalanceColor(any())
        verify(view).show(totalBalance = any())
        verify(view).show(wallets = any())
    }

    @Test
    fun refresh() {
        presenter.refresh()

        verify(interactor).refresh()
    }

    @Test
    fun onReceive() {
        presenter.onReceive(coin)

        verify(router).openReceiveDialog(coin)
    }

    @Test
    fun onPay() {
        presenter.onPay(coin)

        verify(router).openSendDialog(coin)
    }
}
