package io.horizontalsystems.walletkit.modules.multiswap.providers

import android.util.Log
import com.google.gson.Gson
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.managers.APIClient
import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SwapProviderInfoManager(
    appConfigProvider: IAppConfigProvider,
    private val localStorage: ILocalStorage,
) {
    private val unstoppableAPI = APIClient.build(
        appConfigProvider.uswapApiBaseUrl,
        mapOf("x-api-key" to appConfigProvider.uswapApiKey),
    ).create(UnstoppableAPI::class.java)

    private val gson = Gson()

    private var cache: Map<String, UnstoppableAPI.Response.Provider>? = null

    private val _suspensionsFlow = MutableStateFlow(restoredSuspensions())

    /**
     * Scoped asset/chain/pair suspensions, indexed by provider id. Applied by [SwapQuoteService]
     * when it decides which providers can serve the current pair.
     */
    val suspensionsFlow = _suspensionsFlow.asStateFlow()

    suspend fun getInfo(providerName: String): UnstoppableAPI.Response.Provider? {
        // Contact details are needed for whatever provider the record names, so this ignores the
        // sync interval and only asks whether this process has the response in hand yet.
        if (cache == null) {
            fetch()
        }
        return cache?.get(providerName.uppercase())
    }

    /**
     * Refreshes suspensions if the cached ones are stale. Cheap to call on every swap screen open.
     */
    suspend fun sync() {
        if (isFresh()) return
        fetch()
    }

    private fun isFresh(): Boolean {
        // A missing cache counts as stale even when the timestamp is recent. Upgrading from a
        // build that predates suspensions can leave a fresh timestamp beside an empty cache, and
        // the first hour after the update is precisely the window where a live suspension matters.
        // An empty rule set still round-trips as a stored `{}`, so "no suspensions anywhere" is a
        // hit and does not force a fetch on every launch.
        localStorage.uSwapSuspensions ?: return false

        return System.currentTimeMillis() - localStorage.uSwapSuspensionsSyncTime < SYNC_INTERVAL
    }

    private suspend fun fetch() {
        try {
            val responses = unstoppableAPI.providers()

            // Suspended providers stay in the metadata cache: a swap made before the suspension
            // still needs its refund contacts.
            cache = responses.associateBy { it.provider.uppercase() }

            val index = SwapSuspensionIndex.from(responses)
            _suspensionsFlow.value = index

            localStorage.uSwapSuspensions = gson.toJson(index)
            localStorage.uSwapSuspensionsSyncTime = System.currentTimeMillis()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load providers", e)
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

    companion object {
        private const val TAG = "SwapProviderInfoManager"
        private const val SYNC_INTERVAL = 60 * 60 * 1000L // 1 hour
    }
}
