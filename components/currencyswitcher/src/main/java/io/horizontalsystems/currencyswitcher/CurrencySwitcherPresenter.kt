package io.horizontalsystems.currencyswitcher

import androidx.lifecycle.ViewModel
import io.horizontalsystems.views.ListPosition

class CurrencySwitcherPresenter(
        var view: CurrencySwitcherModule.IView,
        var router: CurrencySwitcherModule.IRouter,
        private val interactor: CurrencySwitcherModule.IInteractor)
    : ViewModel(), CurrencySwitcherModule.IViewDelegate {

    private val currencies = interactor.currencies

    override fun viewDidLoad() {
        val baseCurrency = interactor.baseCurrency
        val items = currencies.mapIndexed { index, currency ->
            CurrencyViewItem(
                    currency.code,
                    currency.symbol,
                    currency == baseCurrency,
                    listPosition = ListPosition.getListPosition(currencies.size, index)
            )
        }
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
