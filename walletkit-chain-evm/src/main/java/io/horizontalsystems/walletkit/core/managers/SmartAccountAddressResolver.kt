package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.entities.Account

/**
 * Registration seam for smart-account (AccountType.Passkey) address resolution.
 *
 * The smart-account address is derived and persisted by the host app's
 * account-abstraction layer, not by core. The app registers a provider at
 * startup; EvmKitManager consults it to build a watch-mode kit for passkey
 * accounts. A null result means the account has no address for that chain.
 */
object SmartAccountAddressResolver {

    interface Provider {
        fun evmAddress(account: Account, blockchainType: BlockchainType): Address?
    }

    @Volatile
    var provider: Provider? = null
}
