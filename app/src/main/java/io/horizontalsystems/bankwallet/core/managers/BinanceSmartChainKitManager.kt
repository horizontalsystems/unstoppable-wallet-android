package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.uniswapkit.UniswapKit

class BinanceSmartChainKitManager(
        private val bscscanApiKey: String,
        private val testMode: Boolean,
        private val backgroundManager: BackgroundManager
) : BackgroundManager.Listener {

    init {
        backgroundManager.registerListener(this)
    }

    var evmKit: EthereumKit? = null
        private set

    private var useCount = 0
    private var currentAccount: Account? = null


    val statusInfo: Map<String, Any>?
        get() = evmKit?.statusInfo()

    fun evmKit(account: Account): EthereumKit {
        if (this.evmKit != null && currentAccount != account) {
            this.evmKit?.stop()
            this.evmKit = null
            currentAccount = null
        }

        if (this.evmKit == null) {
            if (account.type !is AccountType.Mnemonic || account.type.words.size != 24)
                throw UnsupportedAccountException()

            useCount = 0

            this.evmKit = createKitInstance(account.type, account)
            currentAccount = account
        }

        useCount++
        return this.evmKit!!
    }

    private fun createKitInstance(accountType: AccountType.Mnemonic, account: Account): EthereumKit {
        val syncSource = EthereumKit.defaultBscWebSocketSyncSource()
        val kit = EthereumKit.getInstance(App.instance, accountType.words, NetworkType.BscMainNet, syncSource, bscscanApiKey, account.id)

        kit.addDecorator(Erc20Kit.getDecorator())
        kit.addDecorator(UniswapKit.getDecorator())

        kit.start()

        return kit
    }

    fun unlink() {
        useCount -= 1

        if (useCount < 1) {
            this.evmKit?.stop()
            this.evmKit = null
            currentAccount = null
        }
    }

    //
    // BackgroundManager.Listener
    //

    override fun willEnterForeground() {
        super.willEnterForeground()
        this.evmKit?.onEnterForeground()
    }

    override fun didEnterBackground() {
        super.didEnterBackground()
        this.evmKit?.onEnterBackground()
    }
}
