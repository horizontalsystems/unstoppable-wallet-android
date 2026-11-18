package io.horizontalsystems.walletkit.core.managers

import com.google.gson.JsonParser
import io.horizontalsystems.walletkit.entities.EvmSyncSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString.Companion.encodeUtf8
import java.util.concurrent.TimeUnit

/**
 * Probes Tron sync sources with wallet/getnowblock for the Auto-Select startup re-pick. Goes
 * through the process-wide proxy like every other client, so it follows Tor when that is on.
 */
class TronSourcePinger(private val tronGridApiKeys: List<String>) {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    suspend fun probe(sources: List<EvmSyncSource>): List<SourceProbe<EvmSyncSource>> = coroutineScope {
        sources.map { source -> async { probe(source) } }.awaitAll()
    }

    private suspend fun probe(source: EvmSyncSource): SourceProbe<EvmSyncSource> = withContext(Dispatchers.IO) {
        try {
            val start = System.nanoTime()
            client.newCall(request(source)).execute().use { response ->
                val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) return@withContext failed(source)

                val height = JsonParser.parseString(body).asJsonObject
                    .getAsJsonObject("block_header")
                    ?.getAsJsonObject("raw_data")
                    ?.get("number")?.asLong
                    ?: return@withContext failed(source)
                SourceProbe(source, elapsedMs, height)
            }
        } catch (e: Exception) {
            failed(source)
        }
    }

    // Sends the credentials the kit would send to this source, and to this source only, or a
    // source that needs them would always look dead.
    internal fun request(source: EvmSyncSource): Request =
        Request.Builder()
            .url(source.uri.toString().trimEnd('/') + "/wallet/getnowblock")
            .apply {
                if (source.isTronGrid) {
                    tronGridApiKeys.firstOrNull()?.let { header("TRON-PRO-API-KEY", it) }
                } else {
                    // tron-kit encodes the auth string as it is, unlike ethereum-kit
                    source.auth?.let { header("Authorization", "Basic " + it.encodeUtf8().base64()) }
                }
            }
            .post("{}".toRequestBody(jsonMediaType))
            .build()

    private fun failed(source: EvmSyncSource) = SourceProbe(source, null, null)

    companion object {
        private val jsonMediaType = "application/json".toMediaType()

        /**
         * How far behind the best reported height a source may be and still count as current:
         * about a minute of 3-second blocks, since the probes answer at slightly different moments.
         */
        const val LAG_THRESHOLD = 20L
    }
}
