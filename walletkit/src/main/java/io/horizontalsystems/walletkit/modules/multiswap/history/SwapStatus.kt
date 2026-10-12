package io.horizontalsystems.walletkit.modules.multiswap.history

enum class SwapStatus {
    Depositing,
    Swapping,
    Sending,
    Completed,
    Refunded,
    Failed,
    ActionRequired,
    // Terminal: no deposit was detected before the order's deadline, so tracking stopped. The
    // user most likely never sent funds; a late deposit may still be resolved server-side.
    Expired,
}

enum class PauseReason {
    OverdueWithFunds,
    Aml,
    Frozen;

    companion object {
        fun fromApi(value: String?): PauseReason? = when (value) {
            "overdue_with_funds" -> OverdueWithFunds
            "aml" -> Aml
            "frozen" -> Frozen
            else -> null
        }
    }
}