package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapProviderType

enum class SwapTimeStatus {
    None,
    Attention,
}

private const val SWAP_TIME_THRESHOLD_SECONDS = 30 * 60L // 30 minutes

/**
 * Swap time is highlighted (orange) when the highest time the user may have to wait is 30 min or more.
 *
 * The comparison uses the same upper limit that is displayed:
 * - CEX providers show a ±25% range, so the top of that range is used.
 * - DEX providers show the single quoted value, which is used directly.
 */
fun swapTimeStatus(estimationTime: Long?, providerType: SwapProviderType?): SwapTimeStatus {
    if (estimationTime == null || providerType == null) {
        return SwapTimeStatus.None
    }

    val upperLimitSeconds = swapTimeUpperLimitSeconds(estimationTime, providerType)
    return if (upperLimitSeconds >= SWAP_TIME_THRESHOLD_SECONDS) {
        SwapTimeStatus.Attention
    } else {
        SwapTimeStatus.None
    }
}

private fun swapTimeUpperLimitSeconds(estimationTime: Long, providerType: SwapProviderType): Long =
    when (providerType) {
        SwapProviderType.CEX -> roundSecondsToMinutes(estimationTime * 1.25)
        SwapProviderType.DEX -> estimationTime
    }