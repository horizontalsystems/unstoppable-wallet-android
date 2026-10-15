package io.horizontalsystems.walletkit.modules.multiswap.history

import io.horizontalsystems.walletkit.core.managers.APIClient
import io.horizontalsystems.marketkit.models.BlockchainType
import retrofit2.http.GET
import retrofit2.http.Query
import java.math.BigDecimal

/**
 * Status tracking for swaps made through AllBridge before it was retired. The provider is no
 * longer quoted; only its transfer-status endpoint is still read, for the records that carry
 * its id.
 */
object AllBridgeTracker {
    const val PROVIDER_ID = "allbridge"

    private val chainSymbolByBlockchainTypeUid: Map<String, String> = mapOf(
        BlockchainType.ArbitrumOne to "ARB",
        BlockchainType.Avalanche to "AVA",
        BlockchainType.Base to "BAS",
        BlockchainType.BinanceSmartChain to "BSC",
        BlockchainType.Ethereum to "ETH",
        BlockchainType.Optimism to "OPT",
        BlockchainType.Polygon to "POL",
        BlockchainType.Solana to "SOL",
        BlockchainType.Stellar to "SRB",
        BlockchainType.Tron to "TRX",
    ).entries.associate { (type, symbol) -> type.uid to symbol }

    private val api = APIClient
        .retrofit("https://allbridge.blocksdecoded.com", 60)
        .create(Api::class.java)

    fun chainSymbol(blockchainTypeUid: String): String? = chainSymbolByBlockchainTypeUid[blockchainTypeUid]

    suspend fun fetchTransferStatus(chain: String, txId: String): TransferStatus = api.transferStatus(chain, txId)

    // Retrofit proxies the interface, which must be reachable from outside this object.
    interface Api {
        @GET("/transfer/status")
        suspend fun transferStatus(
            @Query("chain") chain: String,
            @Query("txId") txId: String,
        ): TransferStatus
    }

    data class TransferStatus(
        val send: BridgeTransaction,
        val receive: BridgeTransaction?,
        val isSuspended: Boolean,
    )

    data class BridgeTransaction(
        val txId: String,
        val amount: String,
        val amountFormatted: BigDecimal,
        val confirmations: Int,
        val confirmationsNeeded: Int,
        val blockTime: Long?,
    )
}
