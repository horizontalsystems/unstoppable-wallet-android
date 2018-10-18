package bitcoin.wallet.modules.currencyswitcher

import android.content.Context
import android.content.Intent
import bitcoin.wallet.core.App
import bitcoin.wallet.core.NetworkManager
import bitcoin.wallet.entities.Currency

object CurrencySwitcherModule {

    interface IView {
        fun updateCurrencyList(currencyList: List<CurrencyViewItem>)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onItemClick(currency: Currency)
    }

    interface IInteractor {
        fun getAvailableCurrencies()
        fun setBaseCurrency(currency: Currency)
    }

    interface IInteractorDelegate {
        fun currencyListUpdated(currencyList: List<CurrencyViewItem>)
    }

    fun start(context: Context) {
        val intent = Intent(context, CurrencySwitcherActivity::class.java)
        context.startActivity(intent)
    }

    fun init(view: CurrencySwitcherViewModel) {
        val interactor = CurrencySwitcherInteractor(NetworkManager(), App.localStorage)
        val presenter = CurrencySwitcherPresenter(interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}

data class CurrencyViewItem(val currency: Currency, var selected: Boolean)