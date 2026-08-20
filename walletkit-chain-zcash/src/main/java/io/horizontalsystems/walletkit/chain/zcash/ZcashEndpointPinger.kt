package io.horizontalsystems.walletkit.chain.zcash

import android.content.Context
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.Response
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.EndpointPingResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Measures reachability and latency of lightwalletd endpoints.
 *
 * Runs against a standalone [LightWalletClient], so it works with no synchronizer and no wallet —
 * which is what lets the startup Auto-Select probe run before the Zcash adapter is created. The
 * SDK's own FastestServerFetcher cannot be used here: it is an instance member of a live
 * Synchronizer and needs the Rust backend for the consensus-branch check.
 */
object ZcashEndpointPinger {

    private val NETWORK = ZcashNetwork.Mainnet

    private val REQUEST_TIMEOUT = 5.seconds

    // A server reporting an estimated chain tip this far above the blocks it actually serves is
    // still catching up; same threshold the SDK's fastest-server validation uses.
    private const val SYNCED_THRESHOLD_BLOCKS = 10

    // Consensus branch id cannot be verified without the Rust backend, so a server that lags the
    // best height reported by the rest of the list is ruled out instead.
    private const val HEIGHT_LAG_THRESHOLD = 5

    suspend fun ping(
        context: Context,
        urls: List<String>,
        toEndpoint: (String) -> LightWalletEndpoint,
    ): List<EndpointPingResult> = withContext(Dispatchers.IO) {
        val probes = coroutineScope {
            urls
                .map { url ->
                    async {
                        // A malformed custom url must not take down the whole probe.
                        val endpoint = runCatching { toEndpoint(url) }.getOrNull()
                        url to endpoint?.let { probe(context, it) }
                    }
                }
                .awaitAll()
        }

        val bestHeight = probes.mapNotNull { it.second?.height }.maxOrNull()

        probes.map { (url, probe) ->
            // A stale server is reported unreachable rather than merely slow: selecting it would
            // stall the sync at its tip.
            val isValid = probe != null &&
                (bestHeight == null || probe.height >= bestHeight - HEIGHT_LAG_THRESHOLD)

            EndpointPingResult(
                url = url,
                isValid = isValid,
                responseTime = if (isValid && probe != null) probe.responseTimeMs else Double.MAX_VALUE,
            )
        }
    }

    @Suppress("ReturnCount")
    private suspend fun probe(context: Context, endpoint: LightWalletEndpoint): Probe? {
        val client = runCatching {
            LightWalletClient.new(context, endpoint, singleRequestTimeout = REQUEST_TIMEOUT)
        }.getOrNull() ?: return null

        try {
            val serverInfo: LightWalletEndpointInfoUnsafe?
            val serverInfoDuration = measureTime {
                serverInfo = withTimeoutOrNull(REQUEST_TIMEOUT) {
                    (client.getServerInfo() as? Response.Success)?.result
                }
            }
            val info = serverInfo ?: return null

            if (!info.matchingNetwork(NETWORK.networkName)) return null
            if (info.saplingActivationHeightUnsafe.value != NETWORK.saplingActivationHeight.value) return null
            if (info.estimatedHeight >= info.blockHeightUnsafe.value + SYNCED_THRESHOLD_BLOCKS) return null

            val latestHeight: Long?
            val blockHeightDuration = measureTime {
                latestHeight = withTimeoutOrNull(REQUEST_TIMEOUT) {
                    (client.getLatestBlockHeight() as? Response.Success)?.result?.value
                }
            }
            val height = latestHeight ?: return null

            // Same score the SDK ranks servers by: the mean of the two round trips.
            val meanMs = (serverInfoDuration + blockHeightDuration).inWholeMilliseconds / 2.0
            return Probe(responseTimeMs = meanMs, height = height)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        } finally {
            // The overall probe is under a timeout, so this runs on a cancelled coroutine most of
            // the time it matters; without NonCancellable the gRPC channel would be left open.
            withContext(NonCancellable) { runCatching { client.dispose() } }
        }
    }

    private data class Probe(
        val responseTimeMs: Double,
        val height: Long,
    )
}
