package cash.p.terminal.core.factories

import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.IWalletManager
import cash.p.terminal.core.managers.EvmAccountManager
import cash.p.terminal.core.managers.EvmKitManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.storage.EvmAccountStateDao
import io.horizontalsystems.marketkit.models.BlockchainType

class EvmAccountManagerFactory(
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val marketKit: MarketKitWrapper,
    private val evmAccountStateDao: EvmAccountStateDao
) {

    fun evmAccountManager(blockchainType: BlockchainType, evmKitManager: EvmKitManager) =
        EvmAccountManager(
            blockchainType,
            accountManager,
            walletManager,
            marketKit,
            evmKitManager,
            evmAccountStateDao
        )

}
