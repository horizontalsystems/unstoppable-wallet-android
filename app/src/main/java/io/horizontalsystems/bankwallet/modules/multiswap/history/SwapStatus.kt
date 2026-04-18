package io.horizontalsystems.bankwallet.modules.multiswap.history

enum class SwapStatus {
    Depositing,
    Swapping,
    Sending,
    Completed,
    Refunded,
    Failed,
}
