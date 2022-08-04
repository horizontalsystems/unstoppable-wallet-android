package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.marketkit.models.BlockchainType
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal
import java.math.BigInteger

class TokenBalanceProvider {

    private val apiURL = App.appConfigProvider.marketApiBaseUrl + "/v1/"
    private val apiService: HsTokenBalanceApi = APIClient.retrofit(apiURL, 60).create(HsTokenBalanceApi::class.java)

    private val balanceThreshold = BigDecimal(BigInteger.ONE, 8)
    private val usdBalanceThreshold = BigDecimal(BigInteger.ONE)

    suspend fun addresses(address: String, blockchainType: BlockchainType): AddressInfo {
        val response = apiService.addresses(address, chain(blockchainType))
        return addressInfo(response)
    }

    suspend fun blockNumber(blockchainType: BlockchainType): Long {
        return apiService.chain(chain(blockchainType)).block_number
    }

    private fun addressInfo(response: AddressesResponse): AddressInfo {
        val addresses = response.balances.mapNotNull { balance ->
            when {
                balance.value <= balanceThreshold || balance.price == null || balance.price * balance.value < usdBalanceThreshold -> {
                    null
                }
                else -> {
                    balance.address
                }
            }
        }
        return AddressInfo(response.block_number, addresses)
    }

    private fun chain(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.ArbitrumOne -> "arbitrum-one"
        BlockchainType.BinanceSmartChain -> "binance-smart-chain"
        BlockchainType.Ethereum -> "ethereum"
        BlockchainType.Optimism -> "optimism"
        BlockchainType.Polygon -> "polygon-pos"
        BlockchainType.Avalanche -> "avalanche"
        else -> throw IllegalArgumentException("Unsupported blockchain type $blockchainType")
    }

    data class AddressInfo(
        val blockNumber: Long,
        val addresses: List<String>
    )

    private data class AddressesResponse(val block_number: Long, val balances: List<BalanceResponse>)
    private data class BalanceResponse(val address: String, val value: BigDecimal, val price: BigDecimal?)
    private data class ChainResponse(val block_number: Long)

    private interface HsTokenBalanceApi {

        @GET("addresses/{address}/coins")
        suspend fun addresses(
            @Path("address") address: String,
            @Query("blockchain") blockchain: String
        ): AddressesResponse

        @GET("chain/{chain}")
        suspend fun chain(
            @Path("chain") chain: String,
        ): ChainResponse

    }

}
