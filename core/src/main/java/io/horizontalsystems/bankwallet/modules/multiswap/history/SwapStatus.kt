package io.horizontalsystems.bankwallet.modules.multiswap.history

enum class SwapStatus {
    Depositing,
    Swapping,
    Sending,
    Completed,
    Refunded,
    Failed,
    ActionRequired,
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