package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.IAddTokenBlockchainService
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.ApiError
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.Single
import java.util.*

class AddEvmTokenBlockchainService(
        private val appConfigProvider: IAppConfigProvider,
        private val networkManager: INetworkManager,
        private val networkType: EthereumKit.NetworkType
): IAddTokenBlockchainService {

    private fun getApiUrl(networkType: EthereumKit.NetworkType): String {
        return when (networkType) {
            EthereumKit.NetworkType.EthMainNet -> "https://api.etherscan.io"
            EthereumKit.NetworkType.EthRopsten -> "https://api-ropsten.etherscan.io"
            EthereumKit.NetworkType.EthKovan -> "https://api-kovan.etherscan.io"
            EthereumKit.NetworkType.EthRinkeby -> "https://api-rinkeby.etherscan.io"
            EthereumKit.NetworkType.EthGoerli -> "https://api-goerli.etherscan.io"
            EthereumKit.NetworkType.BscMainNet -> "https://api.bscscan.com"
        }
    }

    private fun getExplorerKey(networkType: EthereumKit.NetworkType): String {
        return when (networkType) {
            EthereumKit.NetworkType.EthMainNet,
            EthereumKit.NetworkType.EthRopsten,
            EthereumKit.NetworkType.EthKovan,
            EthereumKit.NetworkType.EthRinkeby,
            EthereumKit.NetworkType.EthGoerli -> appConfigProvider.etherscanApiKey
            EthereumKit.NetworkType.BscMainNet -> appConfigProvider.bscscanApiKey
        }
    }

    override fun isValid(reference: String): Boolean {
        return try{
            AddressValidator.validate(reference)
            true
        } catch (e: Exception){
            false
        }
    }

    override fun coinType(reference: String): CoinType {
        val address = reference.lowercase()

        return when (networkType) {
            EthereumKit.NetworkType.EthMainNet,
            EthereumKit.NetworkType.EthRopsten,
            EthereumKit.NetworkType.EthKovan,
            EthereumKit.NetworkType.EthRinkeby,
            EthereumKit.NetworkType.EthGoerli -> CoinType.Erc20(address)
            EthereumKit.NetworkType.BscMainNet -> CoinType.Bep20(address)
        }
    }

    override fun customTokenAsync(reference: String): Single<CustomToken> {
        val request = "api?module=account&action=tokentx&contractaddress=$reference&page=1&offset=1&sort=asc&apikey=${getExplorerKey(networkType)}"

        return networkManager.getEvmInfo(getApiUrl(networkType), request)
                .map { response ->
                    if (response.get("status").asString == "0") {
                        try {
                            if (response.get("result").asString.lowercase().contains("limit reached")) {
                                throw ApiError.ApiLimitExceeded
                            }
                        } catch (e: Exception) {
                            //parsing error
                        }

                        if (response.get("status").asString == "0") {
                            throw ApiError.ContractNotFound
                        }
                    }

                    val result = response.getAsJsonArray("result")?.get(0)?.asJsonObject
                            ?: throw ApiError.InvalidResponse
                    val tokenName = result.get("tokenName")?.asString
                            ?: throw ApiError.InvalidResponse
                    val tokenSymbol = result.get("tokenSymbol")?.asString
                            ?: throw ApiError.InvalidResponse
                    val tokenDecimal = result.get("tokenDecimal")?.asString?.toInt()
                            ?: throw ApiError.InvalidResponse

                    return@map CustomToken(tokenName, tokenSymbol, coinType(reference), tokenDecimal)
                }
    }

}
