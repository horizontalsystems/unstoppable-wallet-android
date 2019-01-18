package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

object DataProviderSettingsModule {
    const val COIN_CODE = "coin_code"

    interface View {
        fun show(items: List<DataProviderSettingsItem>)
        fun close()
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onSelect(item: DataProviderSettingsItem)
    }

    interface Interactor {
        fun providers(coinCode: CoinCode): List<FullTransactionInfoModule.Provider>
        fun baseProvider(coinCode: CoinCode): FullTransactionInfoModule.Provider
        fun setBaseProvider(name: String, coinCode: CoinCode)
    }

    interface InteractorDelegate {
        fun onSetDataProvider()
    }

    fun init(coinCode: CoinCode, view: DataProviderSettingsViewModel) {
        val interactor = DataProviderSettingsInteractor(App.transactionDataProviderManager)
        val presenter = DataProviderSettingsPresenter(coinCode, interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(context: Context, coinCode: CoinCode) {
        val intent = Intent(context, DataProviderSettingsActivity::class.java)
        intent.putExtra(COIN_CODE, coinCode)
        context.startActivity(intent)
    }

}

data class DataProviderSettingsItem(val name: String, val online: Boolean, val selected: Boolean) {

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
        return result
    }
}
