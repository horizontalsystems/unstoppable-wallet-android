package io.horizontalsystems.bankwallet.modules.multiswap.providers

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

    suspend fun getInfo(providerName: String): UnstoppableAPI.Response.Provider? {
        ensureLoaded()
        return cache[providerName.uppercase()]
    }

    private suspend fun ensureLoaded() = mutex.withLock {
        if (loaded) return@withLock
        runCatching {
            cache = unstoppableAPI.providers().associateBy { it.provider.uppercase() }
        }
        loaded = true
    }
}