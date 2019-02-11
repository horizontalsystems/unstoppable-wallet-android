package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IEthereumKitManager
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.ethereumkit.EthereumKit

class EthereumKitManager(appConfig: IAppConfigProvider) : IEthereumKitManager {
    private var kit: EthereumKit? = null
    private var useCount = 0

    private val network = if (appConfig.testMode)
        EthereumKit.NetworkType.Ropsten else
        EthereumKit.NetworkType.MainNet

    override fun ethereumKit(authData: AuthData): EthereumKit {
        useCount += 1

        kit?.let { return it }
        kit = EthereumKit(authData.seed, network, authData.walletId)

        return kit!!
    }

    override fun clear() {
        kit?.clear()
    }

    override fun unlink() {
        useCount -= 1

        if (useCount < 1) {
            kit?.stop()
            kit = null
        }
    }
}
