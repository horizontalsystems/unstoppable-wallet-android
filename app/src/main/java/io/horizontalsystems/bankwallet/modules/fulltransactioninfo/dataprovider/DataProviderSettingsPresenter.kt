package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider

import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class DataProviderSettingsPresenter(val coinCode: CoinCode, private val interactor: DataProviderSettingsModule.Interactor)
    : DataProviderSettingsModule.ViewDelegate, DataProviderSettingsModule.InteractorDelegate {

    var view: DataProviderSettingsModule.View? = null

    override fun viewDidLoad() {
        showItems()
    }

    override fun onSelect(item: DataProviderSettingsItem) {
        if (!item.selected) {
            interactor.setBaseProvider(item.name, coinCode)
        }
    }

    override fun onSetDataProvider() {
        view?.close()
    }

    private fun showItems() {
        val baseProviderName = interactor.baseProvider(coinCode).name

        val items = interactor.providers(coinCode).map {
            DataProviderSettingsItem(name = it.name, online = true, selected = it.name == baseProviderName)
        }

        view?.show(items)
    }
}
