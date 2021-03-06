package io.horizontalsystems.bankwallet.modules.enablecoins

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.ApiError
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.Single
import java.util.*

class EnableCoinsBep20Provider(
        private val networkManager: INetworkManager,
        private val bscscanApiKey: String,
) {

    fun tokens(address: String): Single<List<Coin>> {
        val latestBlock = 99_999_999L // hardcoded big number, should be enough for 3 years time
        val url = "https://api.bscscan.com/"
        val resource = "api?module=account&action=tokentx&address=$address&startblock=1&endblock=$latestBlock&sort=asc&apikey=$bscscanApiKey"

        return networkManager.getEvmInfo(url, resource)
                .map { jsonObject ->
                    if (jsonObject.get("status").asString == "1" && jsonObject.has("result")) {
                        jsonObject.get("result").asJsonArray.mapNotNull {
                            val tokenInfo = it.asJsonObject

                            val contractAddress = tokenInfo.get("contractAddress").asString
                                    ?: throw ApiError.InvalidResponse
                            val tokenName = tokenInfo.get("tokenName")?.asString
                                    ?: throw ApiError.InvalidResponse
                            val tokenSymbol = tokenInfo.get("tokenSymbol")?.asString
                                    ?: throw ApiError.InvalidResponse
                            val tokenDecimal = tokenInfo.get("tokenDecimal")?.asString?.toInt()
                                    ?: throw ApiError.InvalidResponse

                            if (tokenName.isEmpty() || tokenSymbol.isEmpty()) {
                                return@mapNotNull null
                            }

                            Coin(title = tokenName, code = tokenSymbol, decimal = tokenDecimal, type = CoinType.Bep20(contractAddress.toLowerCase(Locale.ENGLISH)))
                        }
                                .distinct()
                    } else {
                        listOf()
                    }
                }
    }
}
