package io.horizontalsystems.walletkit.modules.multiswap.providers

import android.util.Log
import com.google.gson.Gson
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.managers.APIClient
import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The only provider directory: `GET /v3/providers`, synced hourly and kept in local storage so a
 * cold launch names venues straight away. Venue names, Route Scores, privacy levels, execution
 * types, split capability and refund contacts are read from here and nowhere else; the app
 * carries no list of providers of its own. Suspensions are enforced server-side: a suspended
 * provider is simply absent from an unnamed `/v3/rate`, so the rows only decide which venues
 * the split tab and private send may name.
 */
class SwapProviderInfoManager(
    appConfigProvider: IAppConfigProvider,
    private val localStorage: ILocalStorage,
) {
    private val unstoppableAPI = APIClient.build(
        appConfigProvider.uswapApiBaseUrl,
        mapOf("x-api-key" to appConfigProvider.uswapApiKey),
    ).create(UnstoppableAPI::class.java)

    private val gson = Gson()
    private val fetchMutex = Mutex()

    // Every synced row, suspended ones included: a swap made before a suspension still needs
    // its venue name and refund contacts.
    @Volatile
    private var rows: Map<String, UnstoppableAPI.Response.Provider> = restoredRows()

    // Ids that already forced a sync and when, so an id the server keeps quoting but never
    // lists forces at most one fetch an hour.
    private val unknownSyncTimes = mutableMapOf<String, Long>()

    private val _venuesFlow = MutableStateFlow(venues(rows))
    private val _splitProvidersFlow = MutableStateFlow(splitProviderIds(rows))

    /**
     * The venues by server id, as the last sync published them. A flow rather than a lookup: a
     * sync that changes a name or a Route Score reaches an open screen without a reopen.
     */
    val venuesFlow = _venuesFlow.asStateFlow()

    /**
     * Server ids of the providers that serve the split endpoints (`splitSwaps: true` and not
     * suspended). The Private swap tab offers exactly these.
     */
    val splitProvidersFlow = _splitProvidersFlow.asStateFlow()

    /** The venue a route is served by. An id the last sync did not list gets a humanized name and no policy. */
    fun venue(id: String): SwapVenue = _venuesFlow.value[id.uppercase()] ?: SwapVenue.unknown(id)

    /** `executionType` as published by `/v3/providers`; null for an unknown id or an entry without one. */
    fun executionType(id: String): String? = rows[id.uppercase()]?.executionType?.takeIf { it.isNotEmpty() }

    fun supportsSplitSwaps(id: String): Boolean = id.uppercase() in _splitProvidersFlow.value

    /**
     * Providers a private send may route through: confidential (`privacy != none`), settling as
     * a plain `transfer`, and not suspended.
     */
    val privateProviderIds: Set<String>
        get() = rows.values
            .filter { it.isConfidential && !it.suspended && it.executionType == SwapProviderIds.TRANSFER_EXECUTION_TYPE }
            .map { it.provider.uppercase() }
            .toSet()

    /**
     * The distinct synced privacy levels other than `none`, what a private send asks `/v3/rate`
     * for. `["basic"]` until a sync has listed a confidential provider.
     */
    val privacyLevelsExcludingNone: List<String>
        get() = rows.values
            .mapNotNull { it.privacyLevel }
            .filter { it != SwapVenue.NO_PRIVACY }
            .distinct()
            .sorted()
            .ifEmpty { FALLBACK_PRIVACY_LEVELS }

    /** The synced row of a provider, for its refund contacts. Fetches once when nothing is stored yet. */
    suspend fun getInfo(id: String): UnstoppableAPI.Response.Provider? {
        fetchIf { rows.isEmpty() }
        return rows[id.uppercase()]
    }

    /** Refreshes the list if the stored one is stale. Cheap to call on every swap screen open. */
    suspend fun sync() {
        fetchIf { !isFresh() }
    }

    /** A route named a venue the list does not know: fetch the list now, ignoring the hour, once per id per hour. */
    suspend fun requestSync(unknownProviderId: String) {
        val now = System.currentTimeMillis()
        val due = synchronized(unknownSyncTimes) {
            val last = unknownSyncTimes[unknownProviderId]
            if (last != null && now - last in 0 until SYNC_INTERVAL) {
                false
            } else {
                unknownSyncTimes[unknownProviderId] = now
                true
            }
        }
        if (due) fetchIf { true }
    }

    private fun isFresh(): Boolean {
        localStorage.uSwapProviders ?: return false
        // A negative age means the stored timestamp is in the future (a clock correction), which
        // would otherwise read as fresh until the clock caught up.
        val age = System.currentTimeMillis() - localStorage.uSwapProvidersSyncTime
        return age in 0 until SYNC_INTERVAL
    }

    /**
     * Fetches under a lock, re-testing [needed] once it is held: a swap screen open and a refund
     * sheet can ask at the same time, and two responses landing concurrently would publish one
     * list while persisting the other.
     */
    private suspend fun fetchIf(needed: () -> Boolean) {
        if (!needed()) return

        fetchMutex.withLock {
            if (!needed()) return

            try {
                val responses = unstoppableAPI.providers()
                // Stored before it is published: a failed write, like a failed fetch, leaves the
                // previous list and timestamp in place, so the next sync retries instead of
                // waiting out the hour.
                localStorage.uSwapProviders = gson.toJson(responses)
                localStorage.uSwapProvidersSyncTime = System.currentTimeMillis()
                apply(responses)
            } catch (e: CancellationException) {
                // The caller's screen went away mid-request. Not a fetch failure, and swallowing
                // it would leave its coroutine looking like it completed.
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load providers", e)
            }
        }
    }

    private fun apply(responses: List<UnstoppableAPI.Response.Provider>) {
        val byId = index(responses)
        rows = byId
        _venuesFlow.value = venues(byId)
        _splitProvidersFlow.value = splitProviderIds(byId)
    }


    private fun restoredRows(): Map<String, UnstoppableAPI.Response.Provider> {
        val json = localStorage.uSwapProviders ?: return emptyMap()

        return try {
            gson.fromJson(json, Array<UnstoppableAPI.Response.Provider>::class.java)
                ?.let { index(it.toList()) }
                ?: emptyMap()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to restore swap providers", e)
            emptyMap()
        }
    }

    companion object {
        private const val TAG = "SwapProviderInfoManager"

        /** The rows by upper-cased id. A row without an id (Gson leaves an absent field null) is dropped, not thrown on. */
        fun index(responses: List<UnstoppableAPI.Response.Provider>): Map<String, UnstoppableAPI.Response.Provider> =
            responses.filter { !it.provider.isNullOrEmpty() }.associateBy { it.provider.uppercase() }

        private const val SYNC_INTERVAL = 60 * 60 * 1000L // 1 hour
        private val FALLBACK_PRIVACY_LEVELS = listOf("basic")

        fun venues(rows: Map<String, UnstoppableAPI.Response.Provider>): Map<String, SwapVenue> =
            rows.mapValues { (id, row) ->
                SwapVenue(
                    id = id,
                    name = row.name?.takeIf { it.isNotBlank() } ?: SwapVenue.humanized(id),
                    riskLevel = row.amlPolicy?.let { RiskLevel.fromAmlPolicy(it) },
                    privacy = row.privacyLevel ?: SwapVenue.NO_PRIVACY,
                    executionType = row.executionType?.takeIf { it.isNotEmpty() },
                    isKnown = true,
                )
            }

        /** Ids quoted on the Private (split) tab: every provider that serves the split endpoints and is not suspended. */
        fun splitProviderIds(rows: Map<String, UnstoppableAPI.Response.Provider>): Set<String> =
            rows.values.filter { it.splitSwaps && !it.suspended }.map { it.provider.uppercase() }.toSet()
    }
}
