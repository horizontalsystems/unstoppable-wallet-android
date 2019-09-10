package io.horizontalsystems.bankwallet.modules.settings.basecurrency

class BaseCurrencySettingsPresenter(private val interactor: BaseCurrencySettingsModule.IInteractor) : BaseCurrencySettingsModule.IViewDelegate {

    var view: BaseCurrencySettingsModule.IView? = null

    private val currencies = interactor.currencies

    override fun viewDidLoad() {
        val baseCurrency = interactor.baseCurrency
        val items = currencies.map { CurrencyViewItem(it.code, it.symbol, it == baseCurrency) }
        view?.show(items)
    }

    override fun didSelect(position: Int) {
        val selected = currencies[position]
        if (selected != interactor.baseCurrency) {
            interactor.setBaseCurrency(selected)
        }
        view?.close()
    }

}
