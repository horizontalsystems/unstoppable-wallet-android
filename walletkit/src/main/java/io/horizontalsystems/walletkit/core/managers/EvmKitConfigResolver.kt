package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.entities.Account

/**
 * Registration seam for per-account transaction syncer/decorator setup on EVM
 * kits.
 *
 * Kits are created asynchronously on wallet events, so the app must register
 * its provider before any kit can be built. When the provider reports that it
 * configured the kit, the default syncer/decorator registrations are skipped
 * entirely — the provider owns the kit's transaction pipeline for that account.
 */
object EvmKitConfigResolver {

    interface Provider {
        /**
         * Returns true when this provider fully configured the kit's syncers
         * and decorators for the account; false falls back to the defaults.
         */
        fun configure(evmKit: EthereumKit, account: Account, blockchainType: BlockchainType): Boolean
    }

    @Volatile
    var provider: Provider? = null
}
