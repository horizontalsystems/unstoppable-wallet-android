package io.horizontalsystems.bankwallet.modules.multiswap.providers

import android.util.Log
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwapProviderInfoManager @Inject constructor(
    appConfigProvider: AppConfigProvider,
) {
    private val unstoppableAPI = APIClient.build(
        appConfigProvider.uswapApiBaseUrl,
        mapOf("x-api-key" to appConfigProvider.uswapApiKey),
    ).create(UnstoppableAPI::class.java)

    private var cache: Map<String, UnstoppableAPI.Response.Provider>? = null

    suspend fun getInfo(providerName: String): UnstoppableAPI.Response.Provider? {
        if (cache == null) {
            try {
                cache = unstoppableAPI.providers().associateBy { it.provider.uppercase() }
            } catch (e: Throwable) {
                Log.e("SwapProviderInfoManager", "Failed to load providers", e)
            }
        }
        return cache?.get(providerName.uppercase())
    }
}
