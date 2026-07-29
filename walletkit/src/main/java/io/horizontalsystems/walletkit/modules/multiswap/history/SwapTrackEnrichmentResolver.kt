package io.horizontalsystems.walletkit.modules.multiswap.history

import io.horizontalsystems.walletkit.entities.SwapRecord

/**
 * Registration seam for app-resolved track-request context. An app whose
 * broadcast transactions do not originate from the user's own address (e.g.
 * bundled or relayed sends, where the on-chain `from` is an operator account)
 * registers a provider so the tracker still learns the real sending address.
 */
object SwapTrackEnrichmentResolver {

    interface Provider {
        /** The actual sending address for the record's inbound leg; null → nothing to add. */
        fun fromAddress(record: SwapRecord): String?
    }

    @Volatile
    var provider: Provider? = null
}
