package cash.p.terminal.core.factories

import cash.p.terminal.core.managers.EvmAccountManager
import cash.p.terminal.core.managers.EvmKitManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.core.managers.TokenAutoEnableManager
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.IWalletManager

class EvmAccountManagerFactory(
    private val accountManager: cash.p.terminal.wallet.IAccountManager,
    private val walletManager: IWalletManager,
    private val marketKit: MarketKitWrapper,
    private val tokenAutoEnableManager: TokenAutoEnableManager
) {

    fun evmAccountManager(blockchainType: BlockchainType, evmKitManager: EvmKitManager) =
        EvmAccountManager(
            blockchainType,
            accountManager,
            walletManager,
            marketKit,
            evmKitManager,
            tokenAutoEnableManager
        )

}
