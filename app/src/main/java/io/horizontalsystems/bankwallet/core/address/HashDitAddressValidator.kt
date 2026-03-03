
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import retrofit2.http.Body
import retrofit2.http.POST

class HashDitAddressValidator(
    baseUrl: String,
    apiKey: String,
    private val evmBlockchainManager: EvmBlockchainManager
) {
    val supportedBlockchainTypes = listOf(BlockchainType.Ethereum, BlockchainType.BinanceSmartChain, BlockchainType.Polygon)

    private val apiService by lazy {
        APIClient.build(
            baseUrl,
            mapOf("Accept" to "application/json", "X-API-Key" to apiKey)
        ).create(HashDitApi::class.java)
    }

    suspend fun isClear(address: Address, token: Token): Boolean {
        return isClear(address, token.blockchainType)
    }

    //score <= 15: Significant Risk
    //score <= 40: High Risk
    //score <= 59: Medium Risk
    //score >= 60: Safe
    suspend fun isClear(address: Address, blockchainType: BlockchainType): Boolean {
        if (!supportedBlockchainTypes.contains(blockchainType)) throw UnsupportedBlockchainType()

        val chain = evmBlockchainManager.getChain(blockchainType)
        val request = AddressSecurityRequest(chain.id.toString(), address.hex)

        var response = apiService.addressSecurity(request)
        while (response.status == "in progress") {
            delay((response.pollAfter ?: 10) * 1000L)
            response = apiService.addressSecurity(request)
        }
        return (response.data?.overall_score?.toIntOrNull() ?: 0) >= 60
    }

    fun supports(token: Token): Boolean {
        return supportedBlockchainTypes.contains(token.blockchainType)
    }

    private interface HashDitApi {
        @POST("address-security-v2")
        suspend fun addressSecurity(@Body data: AddressSecurityRequest): AddressSecurityResponse
    }

    data class AddressSecurityRequest(
        val chainId: String,
        val address: String
    )

    data class AddressSecurityResponse(
        val code: String,
        val status: String,
        val pollAfter: Int?,
        val data: Data?
    ) {
        data class Data(
            val overall_score: String
        )
    }

}

class UnsupportedBlockchainType : Exception()
