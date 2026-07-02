package io.horizontalsystems.core.core.factories

import io.horizontalsystems.core.core.IAccountManager
import io.horizontalsystems.core.core.managers.EvmAccountManager
import io.horizontalsystems.core.core.managers.EvmKitManager
import io.horizontalsystems.core.core.managers.MarketKitWrapper
import io.horizontalsystems.core.core.managers.TokenAutoEnableManager
import io.horizontalsystems.core.core.managers.WalletManager
import io.horizontalsystems.marketkit.models.BlockchainType

class EvmAccountManagerFactory(
    private val accountManager: IAccountManager,
    private val walletManager: WalletManager,
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
