package io.horizontalsystems.bankwallet.modules.walletconnect

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.ethereumkit.core.EthereumKit

class WalletConnectManager(
        private val accountManager: IAccountManager,
        private val ethereumKitManager: EvmKitManager,
        private val binanceSmartChainKitManager: EvmKitManager
) {

    val activeAccount: Account?
        get() = accountManager.activeAccount

    fun evmKit(chainId: Int, account: Account): EthereumKit? {
        val ethKit = ethereumKitManager.evmKit(account)
        if (ethKit.networkType.chainId == chainId) {
            return ethKit
        } else {
            ethereumKitManager.unlink(account)
        }
        
        val bscKit = binanceSmartChainKitManager.evmKit(account)
        if (bscKit.networkType.chainId == chainId) {
            return bscKit
        } else {
            binanceSmartChainKitManager.unlink(account)
        }
        
        return null
    }

}
