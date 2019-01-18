package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider

import io.horizontalsystems.bankwallet.core.ITransactionDataProviderManager
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class DataProviderSettingsInteractor(private val dataProviderManager: ITransactionDataProviderManager)
    : DataProviderSettingsModule.Interactor {

    var delegate: DataProviderSettingsModule.InteractorDelegate? = null

    override fun providers(coinCode: CoinCode): List<FullTransactionInfoModule.Provider> {
        return dataProviderManager.providers(coinCode)
    }

    override fun baseProvider(coinCode: CoinCode): FullTransactionInfoModule.Provider {
        return dataProviderManager.baseProvider(coinCode)
    }

    override fun setBaseProvider(name: String, coinCode: CoinCode) {
        dataProviderManager.setBaseProvider(name, coinCode)

        delegate?.onSetDataProvider()
    }
}
