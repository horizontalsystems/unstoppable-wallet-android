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
        Synchronizer.Status.DISCONNECTED -> AdapterState.NotSynced(lastError ?: Exception("Disconnected"))
        Synchronizer.Status.SYNCING -> AdapterState.Syncing()
        Synchronizer.Status.SYNCED -> AdapterState.Synced
        Synchronizer.Status.INITIALIZING -> AdapterState.Syncing()
    }

    /** Returns the refined state, or null when the progress update must be ignored. */
    fun fromProgress(current: AdapterState, progressPercent: Int, blocksRemaining: Long?): AdapterState? {
        if (blocksRemaining == null) return null
        if (current !is AdapterState.Syncing) return null
        return AdapterState.Syncing(progress = progressPercent, blocksRemained = blocksRemaining)
    }
}
