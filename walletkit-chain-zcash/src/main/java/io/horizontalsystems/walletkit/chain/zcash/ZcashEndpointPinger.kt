package io.horizontalsystems.walletkit.chain.zcash

import android.content.Context
import android.util.Log
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.Response
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.EndpointPingResult
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.EndpointPingResult.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URI
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

    private const val TAG = "ZcashEndpointPinger"

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
                        val endpoint = runCatching { toEndpoint(url) }
                            // Without the exception: its message repeats the url.
                            .onFailure { Log.w(TAG, "${label(url)}: not a valid endpoint url") }
                            .getOrNull()
                        url to (endpoint?.let { probe(context, it) } ?: Probe.Unreachable)
                    }
                }
                .awaitAll()
        }

        results(probes) { Log.w(TAG, it) }
    }

    /**
     * What an endpoint is called in the log: host and port, never the stored url, which may carry
     * credentials in its user info part.
     */
    internal fun label(url: String): String {
        val uri = runCatching { URI(url) }.getOrNull()
        val host = uri?.host ?: return "invalid url"
        return "$host:${uri.port.takeIf { it > 0 } ?: 443}"
    }

    /**
     * What a server's own info already settles, or null when the probe goes on to ask for its
     * latest block. A server of another network cannot serve the wallet at all, so it is reported
     * the same way as one that does not answer; a server whose estimated chain tip is well above
     * the blocks it serves is still catching up.
     */
    internal fun serverInfoVerdict(
        chainMatches: Boolean,
        saplingActivationHeight: Long,
        blockHeight: Long,
        estimatedHeight: Long,
    ): Verdict? = when {
        !chainMatches ->
            Verdict(Probe.Unreachable, "serves another chain")

        saplingActivationHeight != NETWORK.saplingActivationHeight.value ->
            Verdict(Probe.Unreachable, "sapling activation height is not mainnet's")

        estimatedHeight >= blockHeight + SYNCED_THRESHOLD_BLOCKS ->
            Verdict(Probe.NotSynced, "still catching up with the chain")

        else -> null
    }

    /** The outcome a server's info settles, with the check that settled it for the log. */
    internal data class Verdict(val probe: Probe, val reason: String)

    internal fun results(
        probes: List<Pair<String, Probe>>,
        warn: (String) -> Unit = {},
    ): List<EndpointPingResult> {
        val bestHeight = probes.mapNotNull { (it.second as? Probe.Synced)?.height }.maxOrNull()

        return probes.map { (url, probe) ->
            when (probe) {
                Probe.Unreachable -> EndpointPingResult(url, Status.Unreachable, Double.MAX_VALUE)
                Probe.NotSynced -> EndpointPingResult(url, Status.NotSynced, Double.MAX_VALUE)
                is Probe.Synced -> when {
                    // A stale server is ruled out rather than reported as merely slow: selecting
                    // it would stall the sync at its tip.
                    bestHeight != null && probe.height < bestHeight - HEIGHT_LAG_THRESHOLD -> {
                        warn("${label(url)}: at height ${probe.height}, behind the best height $bestHeight")
                        EndpointPingResult(url, Status.NotSynced, Double.MAX_VALUE)
                    }

                    else -> EndpointPingResult(url, Status.Ok, probe.responseTimeMs)
                }
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun probe(context: Context, endpoint: LightWalletEndpoint): Probe {
        val name = "${endpoint.host}:${endpoint.port}"

        val client = runCatching {
            LightWalletClient.new(context, endpoint, singleRequestTimeout = REQUEST_TIMEOUT)
        }.getOrElse {
            Log.w(TAG, "$name: client was not created", it)
            return Probe.Unreachable
        }

        try {
            val serverInfo: LightWalletEndpointInfoUnsafe?
            val serverInfoDuration = measureTime {
                serverInfo = request(name, "getServerInfo") { client.getServerInfo() }
            }
            val info = serverInfo ?: return Probe.Unreachable

            val verdict = serverInfoVerdict(
                chainMatches = info.matchingNetwork(NETWORK.networkName),
                saplingActivationHeight = info.saplingActivationHeightUnsafe.value,
                blockHeight = info.blockHeightUnsafe.value,
                estimatedHeight = info.estimatedHeight,
            )
            if (verdict != null) {
                Log.w(
                    TAG,
                    "$name: ${verdict.reason}: chain ${info.chainName}, sapling activation " +
                        "${info.saplingActivationHeightUnsafe.value}, height ${info.blockHeightUnsafe.value}, " +
                        "estimated tip ${info.estimatedHeight}"
                )
                return verdict.probe
            }

            val latestHeight: Long?
            val blockHeightDuration = measureTime {
                latestHeight = request(name, "getLatestBlockHeight") { client.getLatestBlockHeight() }?.value
            }
            val height = latestHeight ?: return Probe.Unreachable

            // Same score the SDK ranks servers by: the mean of the two round trips.
            val meanMs = (serverInfoDuration + blockHeightDuration).inWholeMilliseconds / 2.0
            return Probe.Synced(responseTimeMs = meanMs, height = height)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$name: probe failed", e)
            return Probe.Unreachable
        } finally {
            // The overall probe is under a timeout, so this runs on a cancelled coroutine most of
            // the time it matters; without NonCancellable the gRPC channel would be left open.
            withContext(NonCancellable) { runCatching { client.dispose() } }
        }
    }

    /** Runs one request, logging why it produced no result: a failure response or a timeout. */
    private suspend fun <T> request(name: String, method: String, call: suspend () -> Response<T>): T? {
        val response = withTimeoutOrNull(REQUEST_TIMEOUT) { call() }
        if (response is Response.Success) return response.result

        if (response is Response.Failure) {
            Log.w(TAG, "$name: $method failed", response.toThrowable())
        } else {
            Log.w(TAG, "$name: $method timed out after $REQUEST_TIMEOUT")
        }
        return null
    }

    internal sealed interface Probe {
        data class Synced(val responseTimeMs: Double, val height: Long) : Probe

        /** The server answered but is still catching up with the chain. */
        object NotSynced : Probe

        /**
         * No usable answer. A refused connection, a timeout and a reply that cannot be decoded all
         * end here on purpose: none of them leaves a server the wallet could sync through, so the
         * badge and Auto-Select treat them alike. What happened is in the log.
         */
        object Unreachable : Probe
    }
}
