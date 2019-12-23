package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IEthereumKitManager
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.ethereumkit.core.EthereumKit

class EthereumKitManager(appConfig: IAppConfigProvider) : IEthereumKitManager {
    private var kit: EthereumKit? = null
    private var useCount = 0
    private val testMode = appConfig.testMode
    private val infuraProjectId = App.instance.getString(R.string.infuraProjectId)
    private val infuraSecretKey = App.instance.getString(R.string.infuraSecretKey)
    private val etherscanKey = App.instance.getString(R.string.etherscanKey)

    override val ethereumKit: EthereumKit?
        get() = kit

    override val statusInfo: Map<String, Any>?
        get() = ethereumKit?.statusInfo()

    override fun ethereumKit(wallet: Wallet): EthereumKit {
        val account = wallet.account
        val accountType = account.type
        if (accountType is AccountType.Mnemonic && accountType.words.size == 12) {
            useCount += 1

            kit?.let { return it }
            val syncMode = EthereumKit.WordsSyncMode.ApiSyncMode()
            val infuraCredentials = EthereumKit.InfuraCredentials(infuraProjectId, infuraSecretKey)
            val networkType = if (testMode) EthereumKit.NetworkType.Ropsten else EthereumKit.NetworkType.MainNet

            kit = EthereumKit.getInstance(App.instance, accountType.words, syncMode, networkType, infuraCredentials, etherscanKey, account.id)
            kit?.start()

            return kit!!
        }

        throw UnsupportedAccountException()

    }

    override fun unlink() {
        useCount -= 1

        if (useCount < 1) {
            kit?.stop()
            kit = null
        }
    }
}
