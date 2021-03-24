package io.horizontalsystems.bankwallet.modules.enablecoins

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.reactivex.Single

class EnableCoinsErc20Provider(private val networkManager: INetworkManager) {

    fun getTokenAddressesAsync(address: String): Single<List<String>> {
        val url = "https://api.ethplorer.io/getAddressInfo/$address/"
        val resource = "?apiKey=freekey"

        return networkManager.getEvmInfo(url, resource)
                .map {
                    if (it.has("tokens")) {
                        it.get("tokens").asJsonArray.mapNotNull { token ->
                            try {
                                val tokenInfo = token.asJsonObject.get("tokenInfo").asJsonObject
                                tokenInfo.get("address").asString
                            } catch (error: Throwable) {
                                null
                            }
                        }.distinct()
                    } else {
                        listOf()
                    }
                }
    }

}
