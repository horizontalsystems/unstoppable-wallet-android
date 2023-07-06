package cash.p.terminal.modules.walletconnect.version2

import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmKitWrapper
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.AccountType
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain

class WC2Manager(
    private val accountManager: IAccountManager,
    private val evmBlockchainManager: EvmBlockchainManager
) {

    val activeAccount: Account?
        get() = accountManager.activeAccount

    fun getEvmAddress(account: Account, chain: Chain) =
        when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                val seed: ByteArray = accountType.seed
                Signer.address(seed, chain)
            }

            is AccountType.EvmPrivateKey -> {
                Signer.address(accountType.key)
            }

            is AccountType.EvmAddress -> {
                Address(accountType.address)
            }

            else -> throw UnsupportedAccountException()
        }

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
