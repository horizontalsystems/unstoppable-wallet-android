package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.marketkit.models.BlockchainType

private typealias Rules = UnstoppableAPI.Response.ZcashAddressRules

/** Which kind of Zcash address a route takes for one of its addresses. */
sealed class ZcashAddressKind {
    object Transparent : ZcashAddressKind()

    /**
     * A unified address built from [receivers] (`orchard` / `sapling` / `transparent`, as the
     * server names them), carrying at least one of [requiresOneOf].
     */
    data class Unified(val receivers: List<String>, val requiresOneOf: List<String>) : ZcashAddressKind() {
        /** Whether the address to build carries a transparent receiver beside the shielded ones. */
        val carriesTransparent: Boolean
            get() = receivers.any { it.equals(USwapZcashAddressPolicy.RECEIVER_TRANSPARENT, ignoreCase = true) }
    }
}

/** What to send as `sourceAddress` on a ZEC sell. */
sealed class ZcashSourcePolicy {
    /** No `source` rule: a transfer venue builds nothing from it, so nothing is sent. */
    object Omit : ZcashSourcePolicy()

    /** `source.sameAsRefund`: the provider refunds to the sender, so the source IS the refund address. */
    object SameAsRefund : ZcashSourcePolicy()

    /** A source rule of its own kind. */
    data class Own(val kind: ZcashAddressKind) : ZcashSourcePolicy()
}

/**
 * The one place that reads a route's `addressRules` into address kinds. The rule for every address
 * alike: `unified` present means a unified address built from its receivers, otherwise transparent.
 * No rules at all (a non-ZEC route, or a server that sends none yet) means transparent everywhere,
 * which every route takes; the server validates at preview and the provider previews again once
 * with the rules a `400` corrects them to.
 */
object USwapZcashAddressPolicy {
    const val RECEIVER_ORCHARD = "orchard"
    const val RECEIVER_SAPLING = "sapling"
    const val RECEIVER_TRANSPARENT = "transparent"

    /**
     * The buy-side kind. A vault deposit (`thorchain_deposit`) sold from a chain whose memo rides
     * an OP_RETURN output cannot name a unified address in that memo, so the destination is forced
     * to transparent whatever the rule recommends; every such route takes a transparent address.
     */
    fun destinationKind(rules: Rules?, sellBlockchainType: BlockchainType, executionType: String?): ZcashAddressKind {
        if (forcesTransparentDestination(sellBlockchainType, executionType)) return ZcashAddressKind.Transparent
        return kind(rules?.destination)
    }

    fun refundKind(rules: Rules?): ZcashAddressKind = kind(rules?.refund)

    fun sourcePolicy(rules: Rules?): ZcashSourcePolicy {
        val source = rules?.source ?: return ZcashSourcePolicy.Omit
        if (source.sameAsRefund == true) return ZcashSourcePolicy.SameAsRefund
        return ZcashSourcePolicy.Own(kind(source))
    }

    /**
     * Whether a typed ZEC recipient for this route must be a transparent address: the route
     * recommends no unified destination, or the memo cannot carry one.
     */
    fun transparentRecipientOnly(rules: Rules?, sellBlockchainType: BlockchainType, executionType: String?): Boolean =
        destinationKind(rules, sellBlockchainType, executionType) is ZcashAddressKind.Transparent

    private fun forcesTransparentDestination(sellBlockchainType: BlockchainType, executionType: String?): Boolean =
        executionType == USwapRouteFilter.VAULT_EXECUTION_TYPE && sellBlockchainType.memoRidesOpReturn

    private fun kind(rule: UnstoppableAPI.Response.ZcashAddressRules.Rule?): ZcashAddressKind {
        val unified = rule?.unified ?: return ZcashAddressKind.Transparent
        return ZcashAddressKind.Unified(
            receivers = unified.receivers.orEmpty(),
            requiresOneOf = unified.requiresOneOf.orEmpty(),
        )
    }
}
