package io.horizontalsystems.bankwallet.modules.enablecoins

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.ApiError
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.Single
import java.util.*

class EnableCoinsErc20Provider(private val networkManager: INetworkManager) {
    fun tokens(address: String): Single<List<Coin>> {
        val url = "https://api.ethplorer.io/getAddressInfo/$address/"
        val resource = "?apiKey=freekey"

        return networkManager.getEvmInfo(url, resource)
                .map {
                    if (it.has("tokens")) {
                        it.get("tokens").asJsonArray.mapNotNull { tokens ->
                            val token = tokens.asJsonObject
                            val tokenInfo = token.get("tokenInfo").asJsonObject

                            val contractAddress = tokenInfo.get("address").asString
                                    ?: throw ApiError.InvalidResponse
                            val tokenName = tokenInfo.get("name")?.asString
                                    ?: throw ApiError.InvalidResponse
                            val tokenSymbol = tokenInfo.get("symbol")?.asString
                                    ?: throw ApiError.InvalidResponse
                            val tokenDecimal = tokenInfo.get("decimals")?.asString?.toInt()
                                    ?: throw ApiError.InvalidResponse

                            if (tokenName.isEmpty() || tokenSymbol.isEmpty()){
                                return@mapNotNull null
                            }

                            Coin(title = tokenName, code = tokenSymbol, decimal = tokenDecimal, type = CoinType.Erc20(contractAddress.toLowerCase(Locale.ENGLISH)))
                        }
                    } else {
                        listOf()
                    }
                }
    }
}
