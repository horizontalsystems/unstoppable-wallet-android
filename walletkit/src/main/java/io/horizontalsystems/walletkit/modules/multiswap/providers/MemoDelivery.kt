package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.marketkit.models.BlockchainType

/**
 * How far a memo travels on a plain transfer that THIS APP builds for a chain. The question
 * every flow handing an identifier to a counterparty must ask is NOT "does this chain have a
 * memo field" — it is "will a memo attached here actually reach the owner of the destination
 * address, readable by them".
 *
 * A swap or private-send provider matches an incoming deposit to its order by that identifier,
 * and a deposit it cannot match is typically unrecoverable, so only [OnChainPublic] is safe to
 * proceed on. Do not simplify the check to "not None".
 */
enum class MemoDelivery {
    /** Broadcast in the clear and readable by anyone (OP_RETURN, TON comment, Stellar text memo). */
    OnChainPublic,

    /** Encrypted on-chain (Zano transfer comment, shielded Zcash memo). Whether the counterparty
     * decrypts and reads it is unverifiable from the client; the failure mode is lost funds. */
    OnChainPrivate,

    /** Kept on this device only, never broadcast (Monero stores it as a wallet user note). */
    Local,

    /** No memo is carried at all (EVM, Tron, Solana). */
    None;

    val deliversAttachment: Boolean
        get() = this == OnChainPublic
}

/**
 * The single classification table behind the private-send deposit gate and the USwap deposit
 * builders, so the two cannot drift apart. Chains absent from the per-chain send transaction
 * builders default to [MemoDelivery.None].
 */
val BlockchainType.memoDelivery: MemoDelivery
    get() = when (this) {
        // A memo becomes an OP_RETURN output, broadcast in the clear (BitcoinCore OutputSetter).
        BlockchainType.Bitcoin,
        BlockchainType.BitcoinCash,
        BlockchainType.ECash,
        BlockchainType.Litecoin,
        BlockchainType.Dash,
            -> MemoDelivery.OnChainPublic

        // A TON comment, a Stellar text memo and a THORChain/Maya memo are all plainly
        // readable on-chain.
        BlockchainType.Ton,
        BlockchainType.Stellar,
        BlockchainType.Thorchain,
        BlockchainType.Mayachain,
            -> MemoDelivery.OnChainPublic

        // Shielded memos are encrypted to the recipient; transparent addresses carry no memo.
        BlockchainType.Zcash -> MemoDelivery.OnChainPrivate

        // ZanoKit passes the memo as the transfer's "comment", encrypted on-chain.
        BlockchainType.Zano -> MemoDelivery.OnChainPrivate

        // MoneroKit keeps the memo as a local note on this device's wallet cache, keyed by
        // tx id. Nothing about it is broadcast.
        BlockchainType.Monero -> MemoDelivery.Local

        // Nothing on these send paths carries a memo to the chain.
        else -> MemoDelivery.None
    }
