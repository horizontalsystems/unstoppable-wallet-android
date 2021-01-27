package io.horizontalsystems.bankwallet.modules.enablecoins

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.reactivex.Single

class EnableCoinsErc20Provider(private val networkManager: INetworkManager) {
    fun contractAddressesSingle(address: String): Single<List<String>> {
        val url = "https://api.ethplorer.io/getAddressInfo/$address/"
        val resource = "?apiKey=freekey"

        return networkManager.getErc20CoinInfo(url, resource)
                .map {
                    it.get("tokens").asJsonArray.map { tokens ->
                        val token = tokens.asJsonObject
                        val tokenInfo = token.get("tokenInfo").asJsonObject

                        tokenInfo.get("address").asString
                    }
                }
                .firstOrError()
    }
}
