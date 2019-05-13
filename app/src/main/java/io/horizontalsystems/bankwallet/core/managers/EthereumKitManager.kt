package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IEthereumKitManager
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.ethereumkit.core.EthereumKit

class EthereumKitManager(appConfig: IAppConfigProvider) : IEthereumKitManager {
    private var kit: EthereumKit? = null
    private var useCount = 0
    private val testMode = appConfig.testMode
    private val infuraProjectId = App.instance.getString(R.string.infuraProjectId)
    private val infuraSecretKey = App.instance.getString(R.string.infuraSecretKey)
    private val etherscanKey = App.instance.getString(R.string.etherscanKey)

    override fun ethereumKit(authData: AuthData): EthereumKit {
        useCount += 1

        kit?.let { return it }
        val syncMode = EthereumKit.WordsSyncMode.ApiSyncMode()
        val infuraCredentials = EthereumKit.InfuraCredentials(infuraProjectId, infuraSecretKey)
        val networkType = if (testMode) EthereumKit.NetworkType.Ropsten else EthereumKit.NetworkType.MainNet
        kit = EthereumKit.getInstance(App.instance, authData.words, syncMode, networkType, infuraCredentials, etherscanKey, authData.walletId)

        return kit!!
    }

    override fun unlink() {
        useCount -= 1

        if (useCount < 1) {
            kit?.stop()
            kit = null
        }
    }
}
