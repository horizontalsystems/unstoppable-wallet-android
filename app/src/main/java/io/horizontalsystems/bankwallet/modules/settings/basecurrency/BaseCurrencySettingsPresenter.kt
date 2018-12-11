package io.horizontalsystems.bankwallet.modules.settings.basecurrency

class BaseCurrencySettingsPresenter(private val interactor: BaseCurrencySettingsModule.IBaseCurrencySettingsInteractor) :
        BaseCurrencySettingsModule.IBaseCurrencySettingsViewDelegate,
        BaseCurrencySettingsModule.IBaseCurrencySettingsInteractorDelegate {

    var view: BaseCurrencySettingsModule.IBaseCurrencySettingsView? = null

    override fun viewDidLoad() {
        showItems()
    }

    override fun didSelect(item: CurrencyItem) {
        if (!item.selected) {
            interactor.setBaseCurrency(item.code)
        }
    }

    override fun didSetBaseCurrency() {
        view?.close()
    }

    private fun showItems() {
        val baseCurrencyCode = interactor.baseCurrency.code

        val items = interactor.currencies.map { CurrencyItem(it.code, it.symbol, it.code == baseCurrencyCode) }
        view?.show(items)
    }
}
