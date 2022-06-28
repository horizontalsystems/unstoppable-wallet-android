package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.EvmAccountManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitManager
import io.horizontalsystems.bankwallet.core.managers.WalletActivator
import io.horizontalsystems.bankwallet.core.providers.TokenBalanceProvider
import io.horizontalsystems.bankwallet.core.storage.EvmAccountStateDao
import io.horizontalsystems.xxxkit.models.BlockchainType

class EvmAccountManagerFactory(
    private val accountManager: IAccountManager,
    private val provider: TokenBalanceProvider,
    private val walletActivator: WalletActivator,
    private val evmAccountStateDao: EvmAccountStateDao
) {

    fun evmAccountManager(blockchainType: BlockchainType, evmKitManager: EvmKitManager) =
        EvmAccountManager(
            blockchainType,
            accountManager,
            evmKitManager,
            provider,
            evmAccountStateDao,
            walletActivator
        )

}
