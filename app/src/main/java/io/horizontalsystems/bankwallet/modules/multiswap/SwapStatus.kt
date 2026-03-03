package io.horizontalsystems.bankwallet.modules.multiswap

enum class SwapStatus {
    Depositing,
    Swapping,
    Sending,
    Completed,
    Refunded,
    Failed,
}
