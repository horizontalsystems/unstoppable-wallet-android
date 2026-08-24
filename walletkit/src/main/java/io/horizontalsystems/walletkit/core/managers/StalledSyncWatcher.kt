package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/**
 * Notices wallets stuck in NotSynced and gives their chain a chance to recover — for Monero and
 * Zcash, re-picking a node when the selected one has died mid-session.
 *
 * This runs regardless of foreground state, which is the point: both chains keep syncing while
 * backgrounded (neither adapter stops on EnterBackground), so a node dying there would otherwise
 * waste the whole background window and only be noticed when the user next opens the app. Android
 * will freeze the process under Doze eventually, which bounds how much this can do — but it covers
 * the stretch after backgrounding, when most useful background syncing happens.
 *
 * Deliberately a poll rather than a subscription to each adapter's state. Acting on a stall
 * requires waiting to see it persist — switching nodes on a three-second blip would cause more
 * churn than it cures — so an observer would need the same delay before doing anything, plus
 * per-adapter subscription bookkeeping across every wallet reload. Re-reading state on a timer
 * gets the same behaviour and has nothing to leak.
 */
class StalledSyncWatcher(private val scope: CoroutineScope) {

    // Consecutive failing checks seen per chain, and how many it must reach to act.
    private val strikes = mutableMapOf<BlockchainType, Int>()
    private val required = mutableMapOf<BlockchainType, Int>()

    fun start() {
        scope.launch {
            while (isActive) {
                delay(CHECK_INTERVAL)
                check()
            }
        }
    }

    private suspend fun check() {
        ChainRegistry.all.forEach { plugin ->
            val type = plugin.blockchainType

            val stalled = try {
                plugin.hasUnsyncedWallet()
            } catch (e: Throwable) {
                Timber.e(e, "Stalled sync check failed for %s", type.uid)
                false
            }

            if (!shouldAct(type, stalled)) return@forEach

            try {
                plugin.onSyncStalled()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Timber.e(e, "Chain plugin %s onSyncStalled failed", type.uid)
            }
        }
    }

    /**
     * Records one check for [type] and decides whether recovery should run now.
     *
     * Separate from the loop so the strike counting and backoff can be tested directly: the
     * intervals it produces stretch to a quarter of an hour, which is not observable by hand.
     */
    internal fun shouldAct(type: BlockchainType, stalled: Boolean): Boolean {
        if (!stalled) {
            // Healthy again: forget the strikes and the backoff.
            strikes.remove(type)
            required.remove(type)
            return false
        }

        val threshold = required[type] ?: MIN_STRIKES
        val seen = (strikes[type] ?: 0) + 1
        if (seen < threshold) {
            strikes[type] = seen
            return false
        }

        strikes[type] = 0
        // Back off: if recovery did not take, retrying every minute achieves nothing but
        // re-pinging every node forever. Reset happens above once the chain syncs.
        required[type] = (threshold * 2).coerceAtMost(MAX_STRIKES)
        return true
    }

    companion object {
        private val CHECK_INTERVAL = 30.seconds

        // Two consecutive failing checks before acting, so a brief stall is ignored.
        private const val MIN_STRIKES = 2

        // Caps the backoff at roughly a quarter hour between attempts.
        private const val MAX_STRIKES = 32
    }
}
