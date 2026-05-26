package io.horizontalsystems.bankwallet.modules.opencryptopay

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.marketkit.models.BlockchainType
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

// ── Data classes ─────────────────────────────────────────────────────────────

data class OcpPaymentResponse(
    val callback: String,
    @SerializedName("displayName") val merchant: String?,
    val quote: OcpQuote?,
    val requestedAmount: OcpRequestedAmount?,
    val transferAmounts: List<OcpTransferAmount> = emptyList(),
    val statusCode: Int? = null,
    val message: String? = null,
)

data class OcpQuote(
    val id: String,
    val expiration: String,
    val payment: String,
)

data class OcpRequestedAmount(
    val asset: String?,
    val amount: Double?,
)

data class OcpTransferAmount(
    val method: String,
    val assets: List<OcpAsset> = emptyList(),
    val available: Boolean = true,
    val minFee: Double? = null,
)

data class OcpAsset(
    val asset: String,
    val amount: String,
)

data class OcpTransactionResponse(
    val uri: String?,
    val hint: String?,
    val pr: String?,               // Lightning invoice
    val expiryDate: String?,
    val blockchain: String?,
    val id: String?,               // EVM/Bitcoin-specific payment ID (used for hex proof URL)
    val paymentId: String?,
    val txId: String?,
    val method: String?,
)

// ── Chain mapping ─────────────────────────────────────────────────────────────

fun OcpTransferAmount.supportedBlockchainTypes(): List<BlockchainType> = when (method) {
    "Bitcoin" -> listOf(BlockchainType.Bitcoin)
    "Monero" -> listOf(BlockchainType.Monero)
    "Zano" -> listOf(BlockchainType.Zano)
    "Solana" -> listOf(BlockchainType.Solana)
    "Tron" -> listOf(BlockchainType.Tron)
    "Ethereum" -> listOf(BlockchainType.Ethereum)
    "BinanceSmartChain" -> listOf(BlockchainType.BinanceSmartChain)
    "Polygon" -> listOf(BlockchainType.Polygon)
    "Arbitrum" -> listOf(BlockchainType.ArbitrumOne)
    "Optimism" -> listOf(BlockchainType.Optimism)
    "Base" -> listOf(BlockchainType.Base)
    else -> emptyList()
}

// ── LNURL decoder ─────────────────────────────────────────────────────────────

object LnurlDecoder {
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    fun decode(lnurl: String): String {
        val lower = lnurl.lowercase().removePrefix("lightning:")
        val separatorIdx = lower.lastIndexOf('1')
        if (separatorIdx < 1) error("Invalid bech32: no separator")

        val data = lower.substring(separatorIdx + 1)
        // Drop last 6 chars (checksum)
        val payload = data.dropLast(6)

        val fiveBitValues = payload.map { ch ->
            val idx = CHARSET.indexOf(ch)
            if (idx < 0) error("Invalid bech32 character: $ch")
            idx
        }

        // Convert 5-bit groups to 8-bit bytes
        val bytes = mutableListOf<Byte>()
        var buffer = 0
        var bitsInBuffer = 0
        for (value in fiveBitValues) {
            buffer = (buffer shl 5) or value
            bitsInBuffer += 5
            if (bitsInBuffer >= 8) {
                bitsInBuffer -= 8
                bytes.add(((buffer shr bitsInBuffer) and 0xFF).toByte())
            }
        }

        return String(bytes.toByteArray(), Charsets.UTF_8)
    }
}

// ── Retrofit service ──────────────────────────────────────────────────────────

object OcpApiService {
    fun service(baseUrl: String): OcpApi {
        val normalizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return APIClient.retrofit(normalizedBase, timeout = 30)
            .create(OcpApi::class.java)
    }

    interface OcpApi {
        @GET
        suspend fun getPaymentDetails(
            @Url url: String,
            @Query("timeout") timeout: Int = 0,
        ): OcpPaymentResponse

        @GET
        suspend fun getTransactionDetails(
            @Url url: String,
            @Query("quote") quote: String,
            @Query("method") method: String,
            @Query("asset") asset: String,
        ): OcpTransactionResponse
    }
}

// ── Proof submission service ──────────────────────────────────────────────────

object OcpProofService {
    fun service(baseUrl: String): OcpProofApi {
        return APIClient.retrofit(baseUrl, timeout = 30)
            .create(OcpProofApi::class.java)
    }

    interface OcpProofApi {
        @GET
        suspend fun submitProofTx(
            @Url url: String,
            @Query("quote") quote: String,
            @Query("method") method: String,
            @Query("tx") tx: String,
        )

        @GET
        suspend fun submitProofHex(
            @Url url: String,
            @Query("quote") quote: String,
            @Query("method") method: String,
            @Query("hex") hex: String,
        )
    }
}
