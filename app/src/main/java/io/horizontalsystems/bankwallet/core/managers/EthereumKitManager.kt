package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IEthereumKitManager
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.ethereumkit.EthereumKit

class EthereumKitManager(appConfig: IAppConfigProvider) : IEthereumKitManager {
    private var kit: EthereumKit? = null
    private var useCount = 0
    private val testMode = appConfig.testMode
    private val infuraKey = App.instance.getString(R.string.infuraKey)
    private val etherscanKey = App.instance.getString(R.string.etherscanKey)

    override fun ethereumKit(authData: AuthData): EthereumKit {
        useCount += 1

        kit?.let { return it }
        kit = EthereumKit.ethereumKit(App.instance, authData.seed, authData.walletId, testMode, infuraKey, etherscanKey)

        return kit!!
    }

    override fun clear() {
        kit?.clear()
        kit?.stop()
        kit = null
    }

    override fun unlink() {
        useCount -= 1

        if (useCount < 1) {
            kit?.stop()
            kit = null
        }
    }
}
