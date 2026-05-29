package io.horizontalsystems.bankwallet.modules.multiswap.providers

import android.util.Log
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SwapProviderInfoManager(
    appConfigProvider: AppConfigProvider,
) {
    private val unstoppableAPI = APIClient.build(
        appConfigProvider.uswapApiBaseUrl,
        mapOf("x-api-key" to appConfigProvider.uswapApiKey),
    ).create(UnstoppableAPI::class.java)

    private val mutex = Mutex()
    private var cache: Map<String, UnstoppableAPI.Response.Provider> = emptyMap()
    private var loaded = false
    private var lastFailureAt: Long = 0L

    suspend fun getInfo(providerName: String): UnstoppableAPI.Response.Provider? {
        ensureLoaded()
        return cache[providerName.uppercase()]
    }

    private suspend fun ensureLoaded() = mutex.withLock {
        if (loaded) return@withLock
        if (System.currentTimeMillis() - lastFailureAt < RETRY_BACKOFF_MS) return@withLock

        runCatching { unstoppableAPI.providers().associateBy { it.provider.uppercase() } }
            .onSuccess {
                cache = it
                loaded = true
            }
            .onFailure { e ->
                lastFailureAt = System.currentTimeMillis()
                Log.e("SwapProviderInfoManager", "Failed to load providers", e)
            }
    }

    companion object {
        private const val RETRY_BACKOFF_MS = 60_000L
    }
}