package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.entities.Account

/**
 * Registration seam for externally-owned Tron watch addresses (iOS analog:
 * IAccountAddressProvider.tronAddress). An app whose account type carries no
 * Tron key material — e.g. a GasFree wallet whose address derives from a
 * profile store — registers a provider; the kit then runs in watch mode
 * (no signer, sends go through the app's own pipeline).
 */
object TronAddressResolver {

    interface Provider {
        /** Base58 watch address for accounts the provider owns; null → not this provider's account. */
        fun tronAddress(account: Account): String?
    }

    @Volatile
    var provider: Provider? = null
}
