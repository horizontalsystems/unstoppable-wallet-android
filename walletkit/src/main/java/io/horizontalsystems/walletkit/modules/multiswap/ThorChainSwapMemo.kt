package io.horizontalsystems.walletkit.modules.multiswap

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
     * Throws when [memo] does not deliver to [expectedDestination]. Address case is ignored: nodes
     * may echo EVM addresses lowercased.
     */
    fun requireDestination(memo: String, expectedDestination: String) {
        val actual = destination(memo)
        if (actual == null || !actual.equals(expectedDestination, ignoreCase = true)) {
            throw IllegalStateException("Swap memo destination does not match the recipient address")
        }
    }
}
