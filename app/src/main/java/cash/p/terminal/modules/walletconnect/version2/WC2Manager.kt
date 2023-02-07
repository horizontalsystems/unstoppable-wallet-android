package cash.p.terminal.modules.walletconnect.version2

import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmKitWrapper
import cash.p.terminal.entities.Account

class WC2Manager(
        private val accountManager: IAccountManager,
        private val evmBlockchainManager: EvmBlockchainManager
) {

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

}
