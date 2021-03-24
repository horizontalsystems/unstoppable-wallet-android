package io.horizontalsystems.bankwallet.modules.enablecoins

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.reactivex.Single

class EnableCoinsBep20Provider(
        private val networkManager: INetworkManager,
        private val bscscanApiKey: String,
) {

    fun getTokenAddressesAsync(address: String): Single<List<String>> {
        val latestBlock = 99_999_999L // hardcoded big number, should be enough for 3 years time
        val url = "https://api.bscscan.com/"
        val resource = "api?module=account&action=tokentx&address=$address&startblock=1&endblock=$latestBlock&sort=asc&apikey=$bscscanApiKey"

        return networkManager.getEvmInfo(url, resource)
                .map { jsonObject ->
                    if (jsonObject.get("status").asString == "1" && jsonObject.has("result")) {
                        jsonObject.get("result").asJsonArray.mapNotNull {
                            try {
                                val tokenInfo = it.asJsonObject
                                tokenInfo.get("contractAddress").asString
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
