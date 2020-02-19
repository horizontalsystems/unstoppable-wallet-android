package io.horizontalsystems.currencyswitcher

import androidx.lifecycle.ViewModel

class BaseCurrencySettingsPresenter(
        var view: BaseCurrencySettingsModule.IView,
        var router: BaseCurrencySettingsModule.IRouter,
        private val interactor: BaseCurrencySettingsModule.IInteractor)
    : ViewModel(), BaseCurrencySettingsModule.IViewDelegate {

    private val currencies = interactor.currencies

    override fun viewDidLoad() {
        val baseCurrency = interactor.baseCurrency
        val items = currencies.map { CurrencyViewItem(it.code, it.symbol, it == baseCurrency) }
        view.show(items)
    }

    override fun didSelect(position: Int) {
        val selected = currencies[position]
        if (selected != interactor.baseCurrency) {
            interactor.baseCurrency = selected
        }
        router.close()
    }
}
