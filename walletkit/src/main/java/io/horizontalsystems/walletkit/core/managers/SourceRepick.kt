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

    /**
     * @param switchForSpeed false for a chain whose kit is costly to rebuild: the current source
     * is then left only when it failed or lags, never for being slower.
     * @return the source to switch to, or null to stay where the chain is.
     */
    fun <T> choose(current: T, probes: List<SourceProbe<T>>, lagThreshold: Long, switchForSpeed: Boolean = true): T? {
        val fresh = fresh(probes, lagThreshold)
        // Every source failing means the device has no usable network, not that the current
        // source is bad.
        if (fresh.isEmpty()) return null

        val fastest = fresh.minBy { it.responseTimeMs!! }

        val currentProbe = fresh.firstOrNull { it.source == current }
            ?: return fastest.source.takeIf { it != current }
        if (!switchForSpeed) return null

        val currentTime = currentProbe.responseTimeMs!!
        val fastestTime = fastest.responseTimeMs!!
        val clearlySlower = currentTime > fastestTime * SLOWER_FACTOR && currentTime - fastestTime > MIN_GAIN_MS

        return if (clearlySlower) fastest.source else null
    }

    /** The fastest source that answered and is not behind, or null when none answered. */
    fun <T> fastest(probes: List<SourceProbe<T>>, lagThreshold: Long): T? =
        fresh(probes, lagThreshold).minByOrNull { it.responseTimeMs!! }?.source

    private fun <T> fresh(probes: List<SourceProbe<T>>, lagThreshold: Long): List<SourceProbe<T>> {
        val healthy = probes.filter { it.isHealthy }
        if (healthy.isEmpty()) return emptyList()

        val bestHeight = healthy.maxOf { it.height!! }
        return healthy.filter { bestHeight - it.height!! <= lagThreshold }
    }
}
