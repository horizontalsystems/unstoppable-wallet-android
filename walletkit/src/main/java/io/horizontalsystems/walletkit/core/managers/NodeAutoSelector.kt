package io.horizontalsystems.walletkit.core.managers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Auto-Select for a chain that syncs from one node at a time: probes the candidate nodes and
 * moves the chain to another one, at startup and when the current node has stopped working.
 * Moving rebuilds the chain's kit, which [switchTo] triggers by saving the node.
 *
 * @param probe probes every candidate; an empty list when probing is not available yet.
 * @param startupChoice the node to move to at startup, or null to stay on [current].
 * @param lagThreshold how many blocks behind the best a node may be and still be picked.
 */
class NodeAutoSelector<T>(
    private val isEnabled: () -> Boolean,
    private val current: () -> T,
    private val probe: suspend () -> List<SourceProbe<T>>,
    private val switchTo: (T) -> Unit,
    private val startupChoice: (current: T, probes: List<SourceProbe<T>>) -> T?,
    private val lagThreshold: Long = Long.MAX_VALUE,
    private val timeout: Duration = PROBE_TIMEOUT,
) {
    private val reselectMutex = Mutex()

    /**
     * The startup pick. The chain's adapter has already started on the stored node and is not
     * waited on; a switch rebuilds it once. Any probe failure keeps the stored node.
     */
    suspend fun pickOnStartup() {
        if (!isEnabled()) return

        try {
            // Capped so a probe that never returns cannot hold the startup hooks.
            withTimeoutOrNull(timeout) {
                val before = current()
                val winner = startupChoice(before, probe()) ?: return@withTimeoutOrNull
                switchIfUnchanged(before, winner)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // keep the stored node
        }
    }

    /** What [reselect] did. */
    enum class Reselection {
        /** The chain was moved to another node. */
        Switched,

        /** The probe ran and found nothing better: the current node stays. */
        Stayed,

        /**
         * Nothing was decided here: Auto-Select is off, another probe is running, or the node
         * was changed while this one ran. Recovery belongs to whoever did that.
         */
        Skipped,
    }

    /**
     * Moves to the fastest node that answers, for when the current one has died while the app
     * was running.
     */
    suspend fun reselect(): Reselection {
        if (!isEnabled()) return Reselection.Skipped
        if (!reselectMutex.tryLock()) return Reselection.Skipped // a probe is already in flight

        return try {
            val before = current()
            // Capped like the startup probe: one that never returned would hold the mutex
            // forever and block every later recovery. Every node failing means the device has
            // no usable network, not that the current node is bad, so nothing is switched.
            val fastest = withTimeoutOrNull(timeout) { SourceRepick.fastest(probe(), lagThreshold) }
            when {
                // The user may have picked a node or turned Auto-Select off while the probe ran
                !isEnabled() || current() != before -> Reselection.Skipped
                fastest == null || fastest == before -> Reselection.Stayed
                else -> {
                    switchTo(fastest)
                    Reselection.Switched
                }
            }
        } finally {
            reselectMutex.unlock()
        }
    }

    /**
     * Moves to the fastest other node that answers, for a node that failed a request (a send)
     * although it may still answer probes. A node that is behind is not moved to, and what
     * counts as behind is measured against every node that answered, the current one included.
     *
     * @return the node moved to, or null when Auto-Select is off or no other node will do.
     */
    suspend fun switchAway(): T? {
        if (!isEnabled()) return null
        if (!reselectMutex.tryLock()) return null // a probe is already in flight

        return try {
            val before = current()
            val probes = withTimeoutOrNull(timeout) { probe() } ?: return null
            val winner = SourceRepick.fastestOther(before, probes, lagThreshold) ?: return null
            winner.takeIf { switchIfUnchanged(before, it) }
        } finally {
            reselectMutex.unlock()
        }
    }

    /**
     * Whether the current node fails [probeNode], for a chain whose kit cannot tell that its
     * node has died. False when Auto-Select is off or probing is not available: the node is
     * then not this class's business.
     */
    suspend fun isCurrentDown(probeNode: suspend (T) -> List<SourceProbe<T>>): Boolean {
        if (!isEnabled()) return false

        val node = current()
        // A probe that does not return in time is a node that does not answer.
        val probes = withTimeoutOrNull(timeout) { probeNode(node) } ?: return true
        return probes.isNotEmpty() && probes.none { it.source == node && it.isHealthy }
    }

    // The user may have picked a node or turned Auto-Select off while the probe ran.
    private fun switchIfUnchanged(before: T, winner: T): Boolean {
        if (winner == before || !isEnabled() || current() != before) return false
        switchTo(winner)
        return true
    }

    companion object {
        val PROBE_TIMEOUT = 8.seconds

        /** The startup rule of a chain that always moves to the fastest node. */
        fun <T> fastest(probes: List<SourceProbe<T>>): T? = SourceRepick.fastest(probes, Long.MAX_VALUE)
    }
}
