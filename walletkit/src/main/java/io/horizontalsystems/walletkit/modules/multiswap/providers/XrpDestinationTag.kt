package io.horizontalsystems.walletkit.modules.multiswap.providers

/**
 * The XRPL Payment `DestinationTag` — a 32-bit UNSIGNED integer field of the transaction itself.
 *
 * This is where a counterparty's crediting identifier rides on XRP, and the reason XRP cannot be
 * folded into the memo-carrying chains: an exchange or swap provider reads the tag field, never
 * an XRPL memo, so a `text` attachment has nowhere to go and must fail rather than be dropped.
 */
object XrpDestinationTag {
    const val MAX = 4294967295L

    /** The tag [value] denotes, or null when it is not a whole number within the field's range. */
    fun parse(value: String): Long? = value.trim().toLongOrNull()?.takeIf { it in 0..MAX }
}
