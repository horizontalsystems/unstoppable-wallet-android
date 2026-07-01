package io.horizontalsystems.bankwallet.modules.multiswap

enum class SwapTimeStatus {
    None,
    Attention,
}

private const val SWAP_TIME_THRESHOLD_SECONDS = 30 * 60L // 30 minutes
private const val SWAP_TIME_RATIO = 2.0

/**
 * Swap time is highlighted (yellow) only when it is critical for the user, see swap_time_spec_v2.
 *
 * - Multiple providers: attention when ratio >= 2 (relative to the fastest provider) AND time > 30 min.
 * - Single provider: attention when time > 30 min (no relative comparison possible).
 *
 * In both modes the absolute threshold is a strict "> 30 min", so exactly 30 min stays silent.
 */
fun swapTimeStatus(estimationTime: Long?, allEstimationTimes: List<Long?>): SwapTimeStatus {
    if (estimationTime == null || estimationTime <= SWAP_TIME_THRESHOLD_SECONDS) {
        return SwapTimeStatus.None
    }

    val knownTimes = allEstimationTimes.filterNotNull()
    if (knownTimes.size <= 1) {
        // single provider — only the absolute threshold applies
        return SwapTimeStatus.Attention
    }

    val baseline = knownTimes.min()
    val ratio = estimationTime.toDouble() / baseline
    return if (ratio >= SWAP_TIME_RATIO) SwapTimeStatus.Attention else SwapTimeStatus.None
}
