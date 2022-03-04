package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.entities.Account

class WC2Manager(
        private val accountManager: IAccountManager,
        private val ethereumKitManager: EvmKitManager,
        private val binanceSmartChainKitManager: EvmKitManager
) {

    val activeAccount: Account?
        get() = accountManager.activeAccount

    fun evmKitWrapper(chainId: Int, account: Account): EvmKitWrapper? {
        val evmKitWrapper = ethereumKitManager.evmKitWrapper(account)
        if (evmKitWrapper.evmKit.chain.id == chainId) {
            return evmKitWrapper
        } else {
            ethereumKitManager.unlink(account)
        }
        
        val bscKitWrapper = binanceSmartChainKitManager.evmKitWrapper(account)
        if (bscKitWrapper.evmKit.chain.id == chainId) {
            return bscKitWrapper
        } else {
            binanceSmartChainKitManager.unlink(account)
        }
        
        return null
    }

}
