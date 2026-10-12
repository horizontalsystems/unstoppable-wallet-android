package io.horizontalsystems.walletkit.core.adapters.zcash

import cash.z.ecc.android.sdk.Synchronizer
import io.horizontalsystems.walletkit.core.AdapterState

/**
 * Pure sync-state transitions for [ZcashAdapter], separated so they can be unit tested — the
 * adapter itself constructs a live synchronizer and cannot be instantiated in a plain test.
 *
 * Status and progress arrive on separate coroutines. Status is the sole authority for entering
 * and leaving NotSynced; progress only refines an ongoing sync. In particular a progress emission
 * that lands after a terminal STOPPED (the CompactBlockProcessor loop has exited) must not
 * resurrect Syncing: STOPPED never re-emits, so the overwrite would be permanent and the
 * stalled-sync recovery gate, which acts on NotSynced, would never fire.
 */
internal object ZcashSyncState {

    fun fromStatus(status: Synchronizer.Status, lastError: Throwable?): AdapterState = when (status) {
        // STOPPED is terminal: five consecutive block-processing failures land here and only an
        // adapter rebuild (fresh synchronizer) recovers. Reporting NotSynced surfaces the stall
        // to the user and lets the stall watcher trigger that rebuild.
        Synchronizer.Status.STOPPED -> AdapterState.NotSynced(lastError ?: Exception("Sync stopped"))
        // Slipstream reports DISCONNECTED both for an engine that has just started and not yet
        // reached the server (idle) and for a failed sync. Only the failure comes with a
        // processor error; without one this is a start, not something to alarm the user about.
        Synchronizer.Status.DISCONNECTED -> lastError?.let { AdapterState.NotSynced(it) } ?: AdapterState.Connecting
        Synchronizer.Status.SYNCING -> AdapterState.Syncing()
        Synchronizer.Status.SYNCED -> AdapterState.Synced
        Synchronizer.Status.INITIALIZING -> AdapterState.Syncing()
    }

    /**
     * The state once a processor error arrives, or null to keep [current]. Slipstream publishes
     * the DISCONNECTED status before it reports the error, so a Connecting set from that status
     * turns into the failure it was.
     */
    fun fromProcessorError(current: AdapterState, error: Throwable?): AdapterState? =
        if (current is AdapterState.Connecting) AdapterState.NotSynced(error ?: Exception("Disconnected")) else null

    /** Returns the refined state, or null when the progress update must be ignored. */
    fun fromProgress(current: AdapterState, progressPercent: Int, blocksRemaining: Long?): AdapterState? {
        if (blocksRemaining == null) return null
        if (current !is AdapterState.Syncing) return null
        return AdapterState.Syncing(progress = progressPercent, blocksRemained = blocksRemaining)
    }
}
