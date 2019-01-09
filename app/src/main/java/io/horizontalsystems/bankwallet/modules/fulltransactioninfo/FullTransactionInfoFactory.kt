package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class FullTransactionInfoFactory(val networkManager: INetworkManager, val appConfigProvider: IAppConfigProvider, val localStorage: ILocalStorage)
    : FullTransactionInfoModule.ProviderFactory {

    override fun providerFor(coinCode: CoinCode): FullTransactionInfoModule.Provider {
        TODO("not implemented")
    }

}
