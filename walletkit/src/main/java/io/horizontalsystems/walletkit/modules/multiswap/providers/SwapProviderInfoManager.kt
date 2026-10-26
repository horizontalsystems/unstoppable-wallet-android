package io.horizontalsystems.walletkit.modules.multiswap.providers

import android.util.Log
import com.google.gson.Gson
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.managers.APIClient
import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.modules.multiswap.SwapQuote
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    @Volatile
    private var cache: Map<String, UnstoppableAPI.Response.Provider>? = null

    private val _suspensionsFlow = MutableStateFlow(restoredSuspensions())

    // Restored from storage so a cold start keeps the decision.
    @Volatile
    private var executionTypes: SwapExecutionTypes = restoredExecutionTypes()

    private val _routeScoresFlow = MutableStateFlow(restoredRouteScores())

    private val _splitProvidersFlow = MutableStateFlow(restoredSplitProviders())

    /**
     * Server ids of the providers that serve the split endpoints (`splitSwaps: true` on
     * `/v3/providers`). The Private swap tab offers exactly these; a registered candidate the
     * server no longer flags disappears from it on the next sync.
     */
    val splitProvidersFlow = _splitProvidersFlow.asStateFlow()

    fun supportsSplitSwaps(providerId: String): Boolean =
        SwapSuspensionIndex.serverId(providerId) in _splitProvidersFlow.value

    /**
     * Route Scores by provider, as the server last published them. A flow rather than a lookup:
     * a sync that changes only a score leaves the suspension index, and so the quotes, unchanged,
     * and an open Routes list would otherwise keep the old score until it was reopened.
     */
    val routeScoresFlow = _routeScoresFlow.asStateFlow()

    /**
     * Scoped asset/chain/pair suspensions, indexed by provider id. Applied by [SwapQuoteService]
     * when it decides which providers can serve the current pair.
     */
    val suspensionsFlow = _suspensionsFlow.asStateFlow()

    suspend fun getInfo(providerName: String): UnstoppableAPI.Response.Provider? {
        // Contact details are needed for whatever provider the record names, so this ignores the
        // sync interval and only asks whether this process has the response in hand yet.
        fetchIf { cache == null }

        return cache?.get(providerName.uppercase())
    }

    /**
     * Refreshes suspensions if the cached ones are stale. Cheap to call on every swap screen open.
     */
    suspend fun sync() {
        fetchIf { !isFresh() }
    }

    private fun isFresh(): Boolean {
        // A missing cache counts as stale even when the timestamp is recent. Upgrading from a
        // build that predates suspensions can leave a fresh timestamp beside an empty cache, and
        // the first hour after the update is precisely the window where a live suspension matters.
        // An empty rule set still round-trips as a stored `{}`, so "no suspensions anywhere" is a
        // hit and does not force a fetch on every launch.
        localStorage.uSwapSuspensions ?: return false
        // Same for execution types: a build that predates them has a fresh timestamp beside no
        // cache, and shielded ZEC delivery would stay off until the next hourly sync.
        localStorage.uSwapExecutionTypes ?: return false
        // And for route scores: without this a build that predates them would show no score for
        // up to an hour after the update.
        localStorage.uSwapRouteScores ?: return false
        // And for the split-capable set: a build that predates it would offer no split provider
        // until the next hourly sync.
        localStorage.uSwapSplitProviders ?: return false

        // A negative age means the stored timestamp is in the future — a clock correction, which
        // would otherwise read as fresh until the clock caught up.
        val age = System.currentTimeMillis() - localStorage.uSwapSuspensionsSyncTime
        return age in 0 until SYNC_INTERVAL
    }

    /**
     * Fetches under a lock, re-testing [needed] once it is held: a swap screen open and a refund
     * sheet can ask at the same time, and two responses landing concurrently would publish one
     * index while persisting the other.
     */
    private suspend fun fetchIf(needed: () -> Boolean) {
        if (!needed()) return

        fetchMutex.withLock {
            if (!needed()) return

            try {
                val responses = unstoppableAPI.providers()

                // Suspended providers stay in the metadata cache: a swap made before the
                // suspension still needs its refund contacts.
                cache = responses.associateBy { it.provider.uppercase() }

                val index = SwapSuspensionIndex.from(responses)
                _suspensionsFlow.value = index

                val types = SwapExecutionTypes.from(responses)
                executionTypes = types
                localStorage.uSwapExecutionTypes = gson.toJson(types)

                val scores = SwapRouteScores.from(responses)
                _routeScoresFlow.value = scores
                localStorage.uSwapRouteScores = gson.toJson(scores)

                val splitProviders = responses
                    .filter { it.splitSwaps && !it.suspended }
                    .map { SwapSuspensionIndex.serverId(it.provider) }
                    .toSet()
                _splitProvidersFlow.value = splitProviders
                localStorage.uSwapSplitProviders = gson.toJson(splitProviders.toList())

                localStorage.uSwapSuspensions = gson.toJson(index)
                localStorage.uSwapSuspensionsSyncTime = System.currentTimeMillis()
            } catch (e: CancellationException) {
                // The caller's screen went away mid-request. Not a fetch failure, and swallowing
                // it would leave its coroutine looking like it completed.
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load providers", e)
            }
        }
    }

    /**
     * Suspensions must survive a cold launch: they are refreshed at most hourly, so without this a
     * restart would quote a suspended provider until the next sync completes.
     */
    private fun restoredSuspensions(): SwapSuspensionIndex {
        val json = localStorage.uSwapSuspensions ?: return SwapSuspensionIndex()

        return try {
            gson.fromJson(json, SwapSuspensionIndex::class.java) ?: SwapSuspensionIndex()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to restore swap suspensions", e)
            SwapSuspensionIndex()
        }
    }

    /** Whether any execution types were ever stored; false only before the first successful sync. */
    val hasExecutionTypes: Boolean
        get() = localStorage.uSwapExecutionTypes != null

    /**
     * Whether a swap selling [tokenIn] through this provider can pay its ZEC output to a
     * shielded / unified receiver: the provider must settle through a vault (see
     * [SwapExecutionTypes.deliversShieldedZcash]), and the sell chain must carry the memo that
     * names a unified address, which an OP_RETURN output cannot — the UTXO chains fall back to
     * the transparent address.
     */
    fun deliversShieldedZcash(providerId: String, tokenIn: Token): Boolean =
        executionTypes.deliversShieldedZcash(providerId) && !tokenIn.blockchainType.memoRidesOpReturn

    /**
     * Whether a ZEC recipient for this swap must be a transparent address: neither the provider
     * (by execution type) nor the selected route itself delivers to a shielded receiver.
     */
    fun zcashTransparentRecipientOnly(providerId: String, swapQuote: SwapQuote): Boolean =
        swapQuote.tokenOut.blockchainType == BlockchainType.Zcash &&
            !USwapProvider.routeDeliversShieldedZcash(swapQuote) &&
            !deliversShieldedZcash(providerId, swapQuote.tokenIn)

    private fun restoredSplitProviders(): Set<String> {
        val json = localStorage.uSwapSplitProviders ?: return emptySet()

        return try {
            gson.fromJson(json, Array<String>::class.java)?.toSet() ?: emptySet()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to restore split providers", e)
            emptySet()
        }
    }

    private fun restoredRouteScores(): SwapRouteScores {
        val json = localStorage.uSwapRouteScores ?: return SwapRouteScores()

        return try {
            gson.fromJson(json, SwapRouteScores::class.java) ?: SwapRouteScores()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to restore swap route scores", e)
            SwapRouteScores()
        }
    }

    private fun restoredExecutionTypes(): SwapExecutionTypes {
        val json = localStorage.uSwapExecutionTypes ?: return SwapExecutionTypes()

        return try {
            gson.fromJson(json, SwapExecutionTypes::class.java) ?: SwapExecutionTypes()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to restore swap execution types", e)
            SwapExecutionTypes()
        }
    }

    companion object {
        private const val TAG = "SwapProviderInfoManager"
        private const val SYNC_INTERVAL = 60 * 60 * 1000L // 1 hour
    }
}
