package bitcoin.wallet.modules.currencyswitcher

import bitcoin.wallet.entities.Currency

class CurrencySwitcherPresenter(private  val interactor: CurrencySwitcherModule.IInteractor): CurrencySwitcherModule.IViewDelegate, CurrencySwitcherModule.IInteractorDelegate  {
    var view: CurrencySwitcherModule.IView? = null

    override fun viewDidLoad() {
        interactor.getAvailableCurrencies()
    }

    override fun onItemClick(currency: Currency) {
        interactor.setBaseCurrency(currency)
    }

    override fun currencyListUpdated(currencyList: List<CurrencyViewItem>) {
        view?.updateCurrencyList(currencyList)
    }
}
