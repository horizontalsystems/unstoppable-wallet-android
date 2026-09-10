package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.walletkit.core.isEvm
import io.horizontalsystems.marketkit.models.BlockchainType

/**
 * THORChain / Maya swap memo: `=:ASSET:DESTINATION[/REFUND]:LIMIT/INTERVAL/QUANTITY:AFFILIATE:FEE`
 * (`SWAP` and `s` are accepted aliases of `=`). The memo is what the network uses to route the
 * outbound leg, and it is returned verbatim by the quote endpoint — so before it is embedded in
 * the deposit transaction, the destination it names must be the recipient the user saw.
 */
object ThorChainSwapMemo {

    /** The destination address named by [memo], or null when the memo has none. */
    fun destination(memo: String): String? =
        memo.split(":").getOrNull(2)
            ?.substringBefore("/") // an optional refund address may follow the destination
            ?.takeIf { it.isNotBlank() }

    /**
     * Throws when [memo] does not deliver to [expectedDestination] on [destinationChain].
     *
     * Only EVM addresses are compared ignoring case: hex is case-insensitive and nodes echo it
     * lowercased. Every other format (Base58, bech32 as issued, Zcash) is compared exactly — a
     * case-altered Base58 address decodes to different bytes, so it is a different destination.
     */
    fun requireDestination(memo: String, expectedDestination: String, destinationChain: BlockchainType) {
        val actual = destination(memo)
        val matches = actual != null && actual.equals(expectedDestination, ignoreCase = destinationChain.isEvm)
        if (!matches) {
            throw IllegalStateException("Swap memo destination does not match the recipient address")
        }
    }
}
