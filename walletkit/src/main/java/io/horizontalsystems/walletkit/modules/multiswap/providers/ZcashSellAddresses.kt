package io.horizontalsystems.walletkit.modules.multiswap.providers

/**
 * The addresses a ZEC sell names to a USwap provider in /v3/preview.
 *
 * One key decides both: [UProvider.acceptsUnifiedZcashRefund]. Without it the refund is the
 * transparent address, which every transfer venue validates, and no source is sent: a transfer
 * venue builds nothing from it in v3, while StealthEX and Exolix refuse a source that differs
 * from the refund. With it the refund is the shielded-only unified address, so the refund lands
 * in the shielded pool and a route memo names no transparent receiver, and the source IS that
 * address: mayanode sizes the memo from `from_address` and the server marks the spend as shielded
 * by it. The source is therefore never gated on the chain's server-built-tx signal, which is off
 * for Zcash. A mixed pair (transparent refund with a unified source, or the reverse) cannot occur.
 */
internal data class ZcashSellAddresses(val refund: String, val source: String?) {

    companion object {
        /**
         * [shieldedOnlyAddress] is asked only when [provider] takes a unified refund; it must not
         * fall back to an address with a transparent receiver.
         */
        suspend fun resolve(
            provider: UProvider,
            transparentAddress: suspend () -> String,
            shieldedOnlyAddress: suspend () -> String,
        ): ZcashSellAddresses {
            if (!provider.acceptsUnifiedZcashRefund) {
                return ZcashSellAddresses(refund = transparentAddress(), source = null)
            }
            val address = shieldedOnlyAddress()
            return ZcashSellAddresses(refund = address, source = address)
        }
    }
}
