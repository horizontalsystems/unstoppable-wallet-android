package io.horizontalsystems.walletkit.modules.multiswap

enum class SwapTimeStatus {
    None,
    Attention,
}

private const val SWAP_TIME_THRESHOLD_SECONDS = 30 * 60L // 30 minutes
private const val SWAP_TIME_RATIO = 2.0

/**
 * Swap time is highlighted (yellow) on the Swap screen only when it is critical for the user.
 *
 * - Multiple providers: attention when time >= 2x the fastest provider AND time > 30 min.
 * - Single provider: attention when time > 30 min (no relative comparison possible).
 *
 * Both the time and the baseline are the quoted estimates. The ±25% range shown for CEX
 * providers is display only and never takes part in this decision.
 */
fun swapTimeStatus(estimationTime: Long?, allEstimationTimes: List<Long?>): SwapTimeStatus {
    if (estimationTime == null || estimationTime <= SWAP_TIME_THRESHOLD_SECONDS) {
        return SwapTimeStatus.None
    }

    val knownTimes = allEstimationTimes.filterNotNull()
    if (knownTimes.size <= 1) {
        return SwapTimeStatus.Attention
    }

    val baseline = knownTimes.min()
    val ratio = estimationTime.toDouble() / baseline
    return if (ratio >= SWAP_TIME_RATIO) SwapTimeStatus.Attention else SwapTimeStatus.None
}
