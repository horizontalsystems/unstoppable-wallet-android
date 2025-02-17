import io.horizontalsystems.bankwallet.core.address.AddressCheckResult
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import retrofit2.http.Body
import retrofit2.http.POST

class HashDitAddressValidator(
    baseUrl: String,
    apiKey: String,
    private val evmBlockchainManager: EvmBlockchainManager
) {
    private val supportedBlockchainTypes = listOf(BlockchainType.Ethereum, BlockchainType.BinanceSmartChain, BlockchainType.Polygon)

    private val apiService by lazy {
        APIClient.build(
            baseUrl,
            mapOf("Accept" to "application/json", "X-API-KEY" to apiKey)
        ).create(HashDitApi::class.java)
    }

    suspend fun check(address: Address, token: Token): AddressCheckResult {
        if (!supports(token)) return AddressCheckResult.NotSupported

        val chain = evmBlockchainManager.getChain(token.blockchainType)
        val response = apiService.transactionSecurity(TransactionSecurityData(chain.id, address.hex))
        return if (response.data.risk_level == 0)
            AddressCheckResult.Clear
        else
            AddressCheckResult.Detected
    }

    fun supports(token: Token): Boolean {
        return supportedBlockchainTypes.contains(token.blockchainType)
    }

    private interface HashDitApi {
        @POST("transaction-security")
        suspend fun transactionSecurity(@Body data: TransactionSecurityData): TransactionSecurityResponse
    }

    data class TransactionSecurityData(
        val chainId: Int,
        val to: String
    )

    data class TransactionSecurityResponse(
        val code: String,
        val status: String,
        val data: Data
    ) {
        data class Data(
            val request_id: String,
            val has_result: Boolean,
            val polling_interval: Int,
            val risk_level: Int,
            val risk_detail: List<RiskDetail>
        )

        data class RiskDetail(
            val name: String,
            val value: String
        )
    }

}
