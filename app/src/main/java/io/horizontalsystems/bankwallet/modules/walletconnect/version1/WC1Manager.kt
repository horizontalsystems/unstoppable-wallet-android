package io.horizontalsystems.bankwallet.modules.walletconnect.version1

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.entities.Account

class WC1Manager(
        private val accountManager: IAccountManager,
        private val ethereumKitManager: EvmKitManager,
        private val binanceSmartChainKitManager: EvmKitManager
) {
    enum class SupportState {
        Supported, NotSupportedDueToNoActiveAccount, NotSupportedDueToWatchAccount
    }

    val activeAccount: Account?
        get() = accountManager.activeAccount

    fun evmKitWrapper(chainId: Int, account: Account): EvmKitWrapper? {
        val evmKitWrapper = ethereumKitManager.evmKitWrapper(account)
        if (evmKitWrapper.evmKit.networkType.chainId == chainId) {
            return evmKitWrapper
        } else {
            ethereumKitManager.unlink(account)
        }
        
        val bscKitWrapper = binanceSmartChainKitManager.evmKitWrapper(account)
        if (bscKitWrapper.evmKit.networkType.chainId == chainId) {
            return bscKitWrapper
        } else {
            binanceSmartChainKitManager.unlink(account)
        }
        
        return null
    }

    fun getWalletConnectSupportState(): SupportState {
        val tmpAccount = accountManager.activeAccount

        return if (tmpAccount == null) {
            SupportState.NotSupportedDueToNoActiveAccount
        } else if (tmpAccount.isWatchAccount) {
            SupportState.NotSupportedDueToWatchAccount
        } else {
            SupportState.Supported
        }
    }

}
