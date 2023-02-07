package cash.p.terminal.modules.walletconnect.version1

import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmKitWrapper
import cash.p.terminal.entities.Account

class WC1Manager(
        private val accountManager: IAccountManager,
        private val evmBlockchainManager: EvmBlockchainManager
) {
    sealed class SupportState {
        object Supported : SupportState()
        object NotSupportedDueToNoActiveAccount : SupportState()
        class NotSupportedDueToNonBackedUpAccount(val account: Account) : SupportState()
        class NotSupported(val accountTypeDescription: String) : SupportState()
    }

    val activeAccount: Account?
        get() = accountManager.activeAccount

    fun getEvmKitWrapper(chainId: Int, account: Account): EvmKitWrapper? {
        val blockchain = evmBlockchainManager.getBlockchain(chainId) ?: return null
        val evmKitManager = evmBlockchainManager.getEvmKitManager(blockchain.type)
        val evmKitWrapper = evmKitManager.getEvmKitWrapper(account, blockchain.type)

        return if (evmKitWrapper.evmKit.chain.id == chainId) {
            evmKitWrapper
        } else {
            evmKitManager.unlink(account)
            null
        }
    }

    fun getWalletConnectSupportState(): SupportState {
        val tmpAccount = accountManager.activeAccount

        return when {
            tmpAccount == null -> SupportState.NotSupportedDueToNoActiveAccount
            !tmpAccount.isBackedUp -> SupportState.NotSupportedDueToNonBackedUpAccount(tmpAccount)
            tmpAccount.type.supportsWalletConnect -> SupportState.Supported
            else -> SupportState.NotSupported(tmpAccount.type.description)
        }
    }

}
