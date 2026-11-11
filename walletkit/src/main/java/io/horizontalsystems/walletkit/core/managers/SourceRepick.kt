package io.horizontalsystems.walletkit.core.managers

/** One probe of a sync source: round trip and reported chain height, both null when it failed. */
data class SourceProbe<T>(val source: T, val responseTimeMs: Double?, val height: Long?) {
    val isHealthy: Boolean get() = responseTimeMs != null && height != null
}

/**
 * Decides whether a startup probe should move a chain to another sync source.
 *
 * Switching rebuilds the chain's kit, so the current source is kept unless it is clearly worse:
 * it failed, it lags behind the best height by more than [lagThreshold] blocks, or it is both
 * more than [SLOWER_FACTOR] times and more than [MIN_GAIN_MS] slower than the fastest fresh one.
 */
object SourceRepick {
    const val SLOWER_FACTOR = 2.0
    const val MIN_GAIN_MS = 300.0

    /** @return the source to switch to, or null to stay where the chain is. */
    fun <T> choose(current: T, probes: List<SourceProbe<T>>, lagThreshold: Long): T? {
        val healthy = probes.filter { it.isHealthy }
        // Every source failing means the device has no usable network, not that the current
        // source is bad.
        if (healthy.isEmpty()) return null

        val bestHeight = healthy.maxOf { it.height!! }
        val fresh = healthy.filter { bestHeight - it.height!! <= lagThreshold }
        val fastest = fresh.minBy { it.responseTimeMs!! }

        val currentProbe = fresh.firstOrNull { it.source == current }
            ?: return fastest.source.takeIf { it != current }

        val currentTime = currentProbe.responseTimeMs!!
        val fastestTime = fastest.responseTimeMs!!
        val clearlySlower = currentTime > fastestTime * SLOWER_FACTOR && currentTime - fastestTime > MIN_GAIN_MS

        return if (clearlySlower) fastest.source else null
    }
}
