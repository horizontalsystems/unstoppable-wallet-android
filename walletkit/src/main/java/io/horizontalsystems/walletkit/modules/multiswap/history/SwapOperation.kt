package io.horizontalsystems.walletkit.modules.multiswap.history

/**
 * Which user-facing operation produced a swap record. The rail can be identical — the
 * NEAR_CONFIDENTIAL provider serves both private sends and confidential payments — so the
 * record must carry the operation itself; history renders each kind differently.
 */
enum class SwapOperation {
    Swap,
    PrivateSend,
    CrossPay;

    companion object {
        // Unknown values (a record written by a NEWER app version) read as a plain swap
        // rather than crashing history.
        fun fromString(value: String?): SwapOperation =
            entries.firstOrNull { it.name == value } ?: Swap
    }
}
