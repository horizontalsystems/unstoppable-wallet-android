package io.horizontalsystems.walletkit.modules.multiswap

/**
 * Registration seam for accounts that manage router approvals inside their own
 * swap transaction (e.g. smart accounts batching approve + swap into one
 * operation). For such accounts the quote must not surface a separate
 * approve/revoke step — the execution layer owns the allowance.
 */
object SwapApprovalPolicy {

    interface Provider {
        fun approvalsBundledWithSwap(): Boolean
    }

    @Volatile
    var provider: Provider? = null
}
