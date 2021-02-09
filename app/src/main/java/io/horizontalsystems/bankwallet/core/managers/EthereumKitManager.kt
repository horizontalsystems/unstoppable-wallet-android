package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IEthereumKitManager
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CommunicationMode
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EthereumKit.*

class EthereumKitManager(
        private val infuraProjectId: String,
        private val infuraSecretKey: String,
        private val etherscanApiKey: String,
        private val testMode: Boolean,
        private val backgroundManager: BackgroundManager
) : IEthereumKitManager, BackgroundManager.Listener {

    init {
        backgroundManager.registerListener(this)
    }

    private var kit: EthereumKit? = null
    private var useCount = 0
    private var currentAccount: Account? = null

    override val ethereumKit: EthereumKit?
        get() = kit

    override val statusInfo: Map<String, Any>?
        get() = ethereumKit?.statusInfo()

    override fun ethereumKit(wallet: Wallet, communicationMode: CommunicationMode?): EthereumKit {
        val account = wallet.account
        val accountType = account.type

        if (kit != null && currentAccount != account) {
            kit?.stop()
            kit = null
            currentAccount = null
        }

        if (kit == null) {
            if (accountType !is AccountType.Mnemonic || accountType.words.size != 12)
                throw UnsupportedAccountException()

            useCount = 0

            kit = createKitInstance(communicationMode, accountType, account)
            currentAccount = account
        }

        useCount++
        return kit!!
    }

    private fun createKitInstance(communicationMode: CommunicationMode?, accountType: AccountType.Mnemonic, account: Account): EthereumKit {
        val syncMode = WordsSyncMode.ApiSyncMode()
        val networkType = if (testMode) NetworkType.Ropsten else NetworkType.MainNet
        val rpcApi = when (communicationMode) {
            CommunicationMode.Infura -> SyncSource.InfuraWebSocket(infuraProjectId, infuraSecretKey)
            CommunicationMode.Incubed -> SyncSource.Incubed
            else -> throw Exception("Invalid communication mode for Ethereum: ${communicationMode?.value}")
        }
        val kit = Companion.getInstance(App.instance, accountType.words, syncMode, networkType, rpcApi, etherscanApiKey, account.id)
        kit.start()

        return kit
    }

    override fun unlink() {
        useCount -= 1

        if (useCount < 1) {
            kit?.stop()
            kit = null
            currentAccount = null
        }
    }

    //
    // BackgroundManager.Listener
    //

    override fun willEnterForeground() {
        super.willEnterForeground()
        kit?.onEnterForeground()
    }

    override fun didEnterBackground() {
        super.didEnterBackground()
        kit?.onEnterBackground()
    }

}
