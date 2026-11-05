package io.horizontalsystems.walletkit.modules.multiswap.providers

import android.util.Log
import com.google.gson.Gson
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.managers.APIClient
import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider
import io.horizontalsystems.walletkit.core.storage.SwapProviderAssetDao
import io.horizontalsystems.walletkit.entities.SwapProviderAssetRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The one token catalog: `GET /v3/tokens/all`, synced hourly with `If-None-Match` and kept in
 * the swap asset table so a cold launch answers from the last list. A token maps to its catalog
 * identifier, else to its chain-prefixed address ([USwapAssetMapping.derivedIdentifier]), else
 * it cannot be quoted. Read synchronously from the swap screen, the private-send gate and
 * CrossPay; the lookup never blocks on the network.
 */
class USwapAssetCatalog(
    appConfigProvider: IAppConfigProvider,
    private val localStorage: ILocalStorage,
    private val dao: SwapProviderAssetDao,
) {
    /** One catalog asset: how a request names it and which providers with a token list quote it. */
    data class Entry(
        val identifier: String,
        val providerIds: List<String> = emptyList(),
    )

    private val unstoppableAPI = APIClient.build(
        appConfigProvider.uswapApiBaseUrl,
        mapOf("x-api-key" to appConfigProvider.uswapApiKey),
    ).create(UnstoppableAPI::class.java)

    private val gson = Gson()
    private val syncMutex = Mutex()

    // Keyed by [USwapAssetMapping.catalogKey], so a lookup needs no coin database read.
    @Volatile
    private var entries: Map<String, Entry> = emptyMap()

    @Volatile
    private var storedRowsLoaded = false

    @Volatile
    private var lastSyncTime: Long? = null

    private val _syncFlow = MutableStateFlow(0)

    /** Bumped when the stored rows are loaded and after every sync, 200 and 304 alike: the signal to re-evaluate on. */
    val syncFlow = _syncFlow.asStateFlow()

    /**
     * Whether a catalog has landed (the stored rows or a fetch). Until then every catalogued
     * token would read as uncatalogued, so callers that show "not supported" wait for this.
     */
    val isLoaded: Boolean
        get() = entries.isNotEmpty()

    fun entry(token: Token): Entry? = entries[USwapAssetMapping.catalogKey(token.tokenQuery)]

    /** The identifier a request names the token by, or null when the token cannot be quoted. */
    fun asset(token: Token): String? = entry(token)?.identifier ?: USwapAssetMapping.derivedIdentifier(token)

    /**
     * Whether the pair can be asked for at all: both sides have an identifier, and either a
     * provider with a token list quotes both, or the pair is one the DEXes and LI.FI (which
     * keep no list) may route: a same-chain pair, or chain-addressed assets on both sides. A
     * same-asset pair is a private send, not a swap.
     */
    fun canQuote(tokenIn: Token, tokenOut: Token): Boolean {
        if (tokenIn == tokenOut) return false
        val entryIn = entry(tokenIn)
        val entryOut = entry(tokenOut)
        if ((entryIn?.identifier ?: USwapAssetMapping.derivedIdentifier(tokenIn)) == null) return false
        if ((entryOut?.identifier ?: USwapAssetMapping.derivedIdentifier(tokenOut)) == null) return false

        if (entryIn != null && entryOut != null && entryIn.providerIds.any { it in entryOut.providerIds }) return true

        return tokenIn.blockchainType == tokenOut.blockchainType ||
            (USwapAssetMapping.derivedIdentifier(tokenIn) != null && USwapAssetMapping.derivedIdentifier(tokenOut) != null)
    }

    /**
     * The confidential venues that list the token for a private send, in catalog order. Chains
     * that are already privacy-native are never offered: routing them through a confidential
     * intermediary adds a counterparty without adding unlinkability.
     */
    fun privateVenueIds(token: Token, privateProviderIds: Set<String>): List<String> {
        if (token.blockchainType in privacyNativeChains) return emptyList()
        return entry(token)?.providerIds.orEmpty().filter { it.uppercase() in privateProviderIds }
    }

    /**
     * Loads the stored rows on the first call, then fetches `GET /v3/tokens/all` when the hour
     * is up. One sync at a time; a failed fetch keeps the previous rows and the next call retries.
     */
    suspend fun sync() = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            if (!storedRowsLoaded) {
                loadStoredRows()
            }

            val lastSyncTime = lastSyncTime
            if (lastSyncTime != null && System.currentTimeMillis() - lastSyncTime in 0 until SYNC_INTERVAL) {
                return@withLock
            }

            try {
                fetch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to sync the token catalog", e)
            }
        }
    }

    // Rows written before the key was case-normalized are re-keyed on the way in.
    private fun storedKey(tokenQueryId: String): String =
        TokenQuery.fromId(tokenQueryId)?.let { USwapAssetMapping.catalogKey(it) } ?: tokenQueryId

    private fun loadStoredRows() {
        storedRowsLoaded = true
        val records = try {
            dao.getByProvider(STORAGE_KEY)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to read the stored token catalog", e)
            return
        }
        if (records.isEmpty()) return

        val restored = records.mapNotNull { record ->
            try {
                gson.fromJson(record.data, Entry::class.java)?.let { storedKey(record.tokenQueryId) to it }
            } catch (e: Throwable) {
                null
            }
        }.toMap()
        if (restored.isEmpty()) return

        entries = restored
        lastSyncTime = records.minOf { it.timestamp }
        _syncFlow.value++
    }

    private suspend fun fetch() {
        // An ETag is only sent beside the rows it describes: without rows a 304 would leave
        // nothing to answer from.
        val etag = localStorage.uSwapCatalogEtag.takeIf { entries.isNotEmpty() }
        val response = unstoppableAPI.allTokens(etag)
        val now = System.currentTimeMillis()

        if (response.code() == 304) {
            lastSyncTime = now
            _syncFlow.value++
            return
        }

        val tokens = response.body()
        if (!response.isSuccessful || tokens == null) {
            throw IllegalStateException("tokens/all answered ${response.code()}")
        }

        val fetched = mutableMapOf<String, Entry>()
        for (token in tokens) {
            val entry = Entry(token.identifier, token.providers.orEmpty().map { it.uppercase() })
            for (query in USwapAssetMapping.tokenQueries(token)) {
                fetched[USwapAssetMapping.catalogKey(query)] = entry
            }
        }

        // An empty list is never stored as current: the rows stand and the ETag is dropped, so
        // the next sync fetches the body again instead of being told "not modified".
        if (fetched.isEmpty()) {
            localStorage.uSwapCatalogEtag = null
            return
        }

        val records = fetched.map { (tokenQueryId, entry) ->
            SwapProviderAssetRecord(STORAGE_KEY, tokenQueryId, gson.toJson(entry), now)
        }
        // The ETag is persisted only once the rows it describes are: a failed write must not
        // leave an ETag that would answer 304 for rows that were never saved.
        try {
            dao.deleteByProvider(STORAGE_KEY)
            dao.insertAll(records)
            localStorage.uSwapCatalogEtag = response.headers()["ETag"]
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to store the token catalog", e)
            localStorage.uSwapCatalogEtag = null
        }

        entries = fetched
        lastSyncTime = now
        _syncFlow.value++
    }

    companion object {
        private const val TAG = "USwapAssetCatalog"
        private const val SYNC_INTERVAL = 60 * 60 * 1000L // 1 hour

        /** The `providerId` the catalog rows are stored under in the swap asset table. Not a provider. */
        const val STORAGE_KEY = "__catalog__"

        /**
         * Chains whose own transactions already hide the sender: Monero and Zano by protocol,
         * Zcash when spending from the shielded pool. Routing them through a swap rail adds a
         * counterparty who learns the refund address and the recipient, strictly less private
         * than a plain send, so the catalog is never allowed to light the toggle for them.
         */
        val privacyNativeChains = setOf(
            BlockchainType.Zcash,
            BlockchainType.Monero,
            BlockchainType.Zano,
        )
    }
}
