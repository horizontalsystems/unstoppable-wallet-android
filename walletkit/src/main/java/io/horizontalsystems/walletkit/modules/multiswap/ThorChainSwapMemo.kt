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

    // Chains whose addresses may be bech32 (BIP-173) or CashAddr: both encodings define an
    // all-uppercase form as the same address as the all-lowercase one, while a mixed-case string is
    // invalid. Their legacy Base58 forms are mixed-case by construction, so single-case folding
    // never touches them.
    private val caseInsensitiveEncodingChains = setOf(
        BlockchainType.Bitcoin,
        BlockchainType.Litecoin,
        BlockchainType.BitcoinCash,
        BlockchainType.ECash,
        BlockchainType.Thorchain,
        BlockchainType.Mayachain,
    )

    /** The destination address named by [memo], or null when the memo has none. */
    fun destination(memo: String): String? =
        memo.split(":").getOrNull(2)
            ?.substringBefore("/") // an optional refund address may follow the destination
            ?.takeIf { it.isNotBlank() }

    /**
     * Throws when [memo] does not deliver to [expectedDestination] on [destinationChain].
     *
     * Addresses are compared in the canonical form of their encoding: EVM hex ignoring case, and
     * single-case bech32 / CashAddr folded to lowercase. Everything else — Base58 (Solana, Tron,
     * legacy Bitcoin), Zcash, and any mixed-case string — is compared exactly, because a case
     * change there yields a different or invalid address.
     */
    fun requireDestination(memo: String, expectedDestination: String, destinationChain: BlockchainType) {
        val actual = destination(memo)
        val matches = actual != null &&
                canonical(actual, destinationChain) == canonical(expectedDestination, destinationChain)
        if (!matches) {
            throw IllegalStateException("Swap memo destination does not match the recipient address")
        }
    }

    private fun canonical(address: String, chain: BlockchainType): String = when {
        chain.isEvm -> address.lowercase()
        chain in caseInsensitiveEncodingChains && address.isSingleCase() -> address.lowercase()
        else -> address
    }

    private fun String.isSingleCase(): Boolean = this == lowercase() || this == uppercase()
}
