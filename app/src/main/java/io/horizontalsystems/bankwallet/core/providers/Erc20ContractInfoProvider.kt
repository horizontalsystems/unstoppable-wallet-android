package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IErc20ContractInfoProvider
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.core.IAppConfigTestMode
import io.reactivex.Single
import java.lang.Exception
import java.util.*

class Erc20ContractInfoProvider(private val appConfigTestModer: IAppConfigTestMode,
                                private val appConfigProvider: IAppConfigProvider,
                                private val networkManager: INetworkManager): IErc20ContractInfoProvider {

    override fun getCoin(address: String): Single<Coin> {
        val host = if (appConfigTestModer.testMode) "https://api-ropsten.etherscan.io/" else "https://api.etherscan.io/"
        val request = "api?module=account&action=tokentx&contractaddress=$address&page=1&offset=1&sort=asc&apikey=${appConfigProvider.etherscanApiKey}"

        return networkManager.getCoinInfo(host, request)
                .map { response->
                    if (response.get("status").asString == "0" && response.get("result").asString.toLowerCase(Locale.ENGLISH).contains("limit reached")){
                        throw ApiError.ApiLimitExceeded
                    }
                    if (response.get("status").asString == "0" && response.get("result").asString.toLowerCase(Locale.ENGLISH).contains("invalid contract address")){
                        throw ApiError.ContractDoesNotExist
                    }

                    val result = response.getAsJsonArray("result")?.get(0)?.asJsonObject ?: throw ApiError.InvalidResponse
                    val tokenName = result.get("tokenName")?.asString ?: throw ApiError.InvalidResponse
                    val tokenSymbol = result.get("tokenSymbol")?.asString ?: throw ApiError.InvalidResponse
                    val tokenDecimal = result.get("tokenDecimal")?.asString?.toInt() ?: throw ApiError.InvalidResponse

                    return@map Coin(tokenSymbol, tokenName, tokenSymbol, tokenDecimal, CoinType.Erc20(address))
                }
                .firstOrError()
    }


    sealed class ApiError : Exception() {
        object ApiLimitExceeded : ApiError()
        object ContractDoesNotExist : ApiError()
        object InvalidResponse : ApiError()
    }

}
