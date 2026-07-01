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
    private const val EXPECTED_HRP = "lnurl"
    private val GEN = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)

    private fun polymod(values: IntArray): Int {
        var c = 1
        for (v in values) {
            val c0 = c ushr 25
            c = ((c and 0x1ffffff) shl 5) xor v
            for (i in 0..4) {
                if (c0 and (1 shl i) != 0) c = c xor GEN[i]
            }
        }
        return c
    }

    private fun verifyChecksum(hrp: String, data: IntArray): Boolean {
        val expand = IntArray(hrp.length * 2 + 1)
        for (i in hrp.indices) {
            expand[i] = hrp[i].code shr 5
            expand[i + hrp.length + 1] = hrp[i].code and 31
        }
        expand[hrp.length] = 0
        return polymod(expand + data) == 1
    }

    fun decode(lnurl: String): String {
        val lower = lnurl.lowercase().removePrefix("lightning:")
        val separatorIdx = lower.lastIndexOf('1')
        if (separatorIdx < 1) error("Invalid bech32: no separator")

        val hrp = lower.substring(0, separatorIdx)
        if (hrp != EXPECTED_HRP) error("Invalid LNURL: expected HRP '$EXPECTED_HRP', got '$hrp'")

        val data = lower.substring(separatorIdx + 1)
        if (data.length < 6) error("Invalid bech32: data too short")

        val fiveBitValues = IntArray(data.length) { i ->
            val idx = CHARSET.indexOf(data[i])
            if (idx < 0) error("Invalid bech32 character: ${data[i]}")
            idx
        }

        if (!verifyChecksum(hrp, fiveBitValues)) error("Invalid bech32 checksum")

        // Convert 5-bit groups to 8-bit bytes, dropping the 6-char checksum
        val bytes = mutableListOf<Byte>()
        var buffer = 0
        var bitsInBuffer = 0
        for (i in 0 until fiveBitValues.size - 6) {
            buffer = (buffer shl 5) or fiveBitValues[i]
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
