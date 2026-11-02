package io.horizontalsystems.walletkit.core.adapters.zcash

import android.content.Context
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.Response
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

/**
 * Block times for transactions the SDK knows the mined height of but not the block time.
 *
 * Transparent history is fetched with an address lookup as soon as the wallet starts, long
 * before the block scan reaches those heights. The SDK records the mined height of each such
 * transaction but takes the block time from the table of scanned blocks, so until the scan
 * catches up it reports none, and the history would show the transaction on January 1, 1970.
 *
 * [fetch] reads the exact time from the block header on the light wallet server; its result is
 * cached by height. When that fails, [estimate] interpolates from the bundled checkpoints; an
 * estimate is not cached so a later call can still replace it with the exact time.
 */
class ZcashBlockTimeResolver(
    private val fetch: suspend (height: Long) -> Long?,
    private val estimate: suspend (height: Long) -> Long?,
) {
    private val mutex = Mutex()
    private val exactByHeight = mutableMapOf<Long, Long>()

    /** Block time of [height] in epoch seconds, or null when neither source can give one. */
    suspend fun blockTime(height: Long): Long? {
        mutex.withLock { exactByHeight[height] }?.let { return it }

        val exact = attempt { fetch(height) }
        if (exact != null && exact > 0) {
            mutex.withLock { exactByHeight[height] = exact }
            return exact
        }

        return attempt { estimate(height) }?.takeIf { it > 0 }
    }

    private suspend fun attempt(block: suspend () -> Long?): Long? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    companion object {
        private val REQUEST_TIMEOUT = 10.seconds

        fun forEndpoint(
            context: Context,
            endpoint: LightWalletEndpoint,
            network: ZcashNetwork,
        ): ZcashBlockTimeResolver {
            val appContext = context.applicationContext
            return ZcashBlockTimeResolver(
                fetch = { height -> fetchBlockTime(appContext, endpoint, height) },
                estimate = { height ->
                    SdkSynchronizer.estimateBirthdayDate(
                        context = appContext,
                        height = BlockHeight.new(height),
                        network = network,
                    ).epochSeconds
                },
            )
        }

        /**
         * The header time of the compact block at [height], read on a connection of its own.
         * Such lookups are rare, one per transaction found ahead of the scan, so the channel is
         * opened per call and closed once the block has been read.
         */
        private suspend fun fetchBlockTime(context: Context, endpoint: LightWalletEndpoint, height: Long): Long? {
            val client = LightWalletClient.new(context, endpoint, singleRequestTimeout = REQUEST_TIMEOUT)
            try {
                val range = BlockHeightUnsafe(height)..BlockHeightUnsafe(height)
                val response = withTimeoutOrNull(REQUEST_TIMEOUT) {
                    client.getBlockRange(range).firstOrNull()
                }
                return (response as? Response.Success)?.result?.time?.toLong()
            } finally {
                withContext(NonCancellable) {
                    client.dispose()
                }
            }
        }
    }
}
