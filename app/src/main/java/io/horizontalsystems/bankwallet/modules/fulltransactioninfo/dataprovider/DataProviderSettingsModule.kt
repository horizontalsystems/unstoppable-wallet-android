package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule

object DataProviderSettingsModule {

    interface View {
        fun show(items: List<DataProviderSettingsItem>)
        fun close()
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onSelect(item: DataProviderSettingsItem)
    }

    interface Interactor {
        fun pingProvider(name: String, url: String, isTrusted: Boolean)
        fun providers(coin: Coin): List<FullTransactionInfoModule.Provider>
        fun baseProvider(coin: Coin): FullTransactionInfoModule.Provider
        fun setBaseProvider(name: String, coin: Coin)
    }

    interface InteractorDelegate {
        fun onPingSuccess(name: String)
        fun onPingFailure(name: String)
        fun onSetDataProvider()
    }

    fun init(coin: Coin, view: DataProviderSettingsViewModel) {
        val interactor = DataProviderSettingsInteractor(App.transactionDataProviderManager, App.networkManager)
        val presenter = DataProviderSettingsPresenter(coin, interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}

data class DataProviderSettingsItem(val name: String, var online: Boolean, val selected: Boolean, var checking: Boolean) {

    override fun equals(other: Any?): Boolean {
        if (other is DataProviderSettingsItem) {
            return name == other.name
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + online.hashCode()
        result = 31 * result + selected.hashCode()
        result = 31 * result + checking.hashCode()
        return result
    }
}
