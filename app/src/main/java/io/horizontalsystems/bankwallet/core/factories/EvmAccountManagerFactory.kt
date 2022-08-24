package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.EvmAccountManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.storage.EvmAccountStateDao
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
