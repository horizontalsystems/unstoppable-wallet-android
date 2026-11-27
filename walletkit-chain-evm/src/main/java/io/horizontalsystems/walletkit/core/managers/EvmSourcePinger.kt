package io.horizontalsystems.walletkit.core.managers

import com.google.gson.JsonParser
import io.horizontalsystems.walletkit.entities.EvmSyncSource
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Probes EVM sync sources with eth_blockNumber for the Auto-Select startup re-pick. Goes through
 * the process-wide proxy like every other client, so it follows Tor when that is on.
 */
object EvmSourcePinger {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    private val jsonMediaType = "application/json".toMediaType()
    private const val BLOCK_NUMBER_REQUEST = """{"jsonrpc":"2.0","id":1,"method":"eth_blockNumber","params":[]}"""

    suspend fun probe(sources: List<EvmSyncSource>): List<SourceProbe<EvmSyncSource>> = coroutineScope {
        sources.map { source -> async { probe(source) } }.awaitAll()
    }

    private suspend fun probe(source: EvmSyncSource): SourceProbe<EvmSyncSource> = withContext(Dispatchers.IO) {
        try {
            val request = request(source)

            val start = System.nanoTime()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                // Taken once the answer is read: the headers can arrive long before it
                val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
                if (!response.isSuccessful || body == null) return@withContext failed(source)

                val result = JsonParser.parseString(body).asJsonObject.get("result")?.asString
                    ?: return@withContext failed(source)
                SourceProbe(source, elapsedMs, result.removePrefix("0x").toLong(16))
            }
        } catch (_: Exception) {
            // Whatever the probe fails with, the source did not answer it
            failed(source)
        }
    }

    // Sends the source's auth the way ethereum-kit does, or a source that needs it would
    // always look dead.
    internal fun request(source: EvmSyncSource): Request =
        Request.Builder()
            .url(source.uri.toString())
            .apply { source.auth?.let { header("Authorization", Credentials.basic("", it)) } }
            .post(BLOCK_NUMBER_REQUEST.toRequestBody(jsonMediaType))
            .build()

    private fun failed(source: EvmSyncSource) = SourceProbe(source, null, null)

    /**
     * How far behind the best reported height a source may be and still count as current:
     * about a minute of blocks, since the probes answer at slightly different moments.
     */
    fun lagThreshold(blockchainType: BlockchainType): Long = when (blockchainType) {
        BlockchainType.Ethereum -> 5
        BlockchainType.Gnosis -> 12
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.Base -> 30
        BlockchainType.Fantom,
        BlockchainType.ZkSync -> 60
        BlockchainType.BinanceSmartChain -> 80
        BlockchainType.Arc -> 120
        BlockchainType.ArbitrumOne,
        BlockchainType.RobinhoodChain -> 240
        else -> 60
    }
}
