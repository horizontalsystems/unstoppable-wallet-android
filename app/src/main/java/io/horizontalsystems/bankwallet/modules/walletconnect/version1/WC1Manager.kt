package io.horizontalsystems.bankwallet.modules.walletconnect.version1

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.entities.Account

class WC1Manager(
        private val accountManager: IAccountManager,
        private val evmBlockchainManager: EvmBlockchainManager
) {
    enum class SupportState {
        Supported, NotSupportedDueToNoActiveAccount, NotSupportedDueToWatchAccount
    }

    val activeAccount: Account?
        get() = accountManager.activeAccount

    fun getEvmKitWrapper(chainId: Int, account: Account): EvmKitWrapper? {
        val blockchain = evmBlockchainManager.getBlockchain(chainId) ?: return null
        val evmKitManager = evmBlockchainManager.getEvmKitManager(blockchain)
        val evmKitWrapper = evmKitManager.getEvmKitWrapper(account, blockchain)

        return if (evmKitWrapper.evmKit.chain.id == chainId) {
            evmKitWrapper
        } else {
            evmKitManager.unlink(account)
            null
        }
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
