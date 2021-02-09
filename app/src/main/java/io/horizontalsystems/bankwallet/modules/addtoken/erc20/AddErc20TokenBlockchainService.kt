package io.horizontalsystems.bankwallet.modules.addtoken.erc20

import io.horizontalsystems.bankwallet.core.IAddTokenBlockchainService
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.ApiError
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.core.IBuildConfigProvider
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.reactivex.Single
import java.util.*

class AddErc20TokenBlockchainService(
        private val appConfigTestModer: IBuildConfigProvider,
        private val appConfigProvider: IAppConfigProvider,
        private val networkManager: INetworkManager
) : IAddTokenBlockchainService {

    override fun validate(reference: String) {
        AddressValidator.validate(reference)
    }

    override fun existingCoin(reference: String, coins: List<Coin>): Coin? {
        return coins.firstOrNull { it.type is CoinType.Erc20 && it.type.address.equals(reference, true) }
    }

    override fun coinSingle(reference: String): Single<Coin> {
        val host = if (appConfigTestModer.testMode) "https://api-ropsten.etherscan.io/" else "https://api.etherscan.io/"
        val request = "api?module=account&action=tokentx&contractaddress=$reference&page=1&offset=1&sort=asc&apikey=${appConfigProvider.etherscanApiKey}"

        return networkManager.getErc20CoinInfo(host, request)
                .map { response ->
                    if (response.get("status").asString == "0") {
                        try {
                            if (response.get("result").asString.toLowerCase(Locale.ENGLISH).contains("limit reached")) {
                                throw ApiError.ApiLimitExceeded
                            }
                        } catch (e: Exception) {
                            //parsing error
                        }

                        if (response.get("status").asString == "0") {
                            throw ApiError.ContractNotFound
                        }
                    }

                    val result = response.getAsJsonArray("result")?.get(0)?.asJsonObject ?: throw ApiError.InvalidResponse
                    val tokenName = result.get("tokenName")?.asString ?: throw ApiError.InvalidResponse
                    val tokenSymbol = result.get("tokenSymbol")?.asString ?: throw ApiError.InvalidResponse
                    val tokenDecimal = result.get("tokenDecimal")?.asString?.toInt() ?: throw ApiError.InvalidResponse

                    return@map Coin(tokenSymbol, tokenName, tokenSymbol, tokenDecimal, CoinType.Erc20(reference))
                }
                .firstOrError()
    }

}
