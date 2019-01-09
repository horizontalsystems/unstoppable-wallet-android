package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.EthereumResponse
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionResponse
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.bankwallet.viewHelpers.ValueFormatter
import io.reactivex.Flowable
import java.math.BigInteger
import java.util.*

class EtherscanProvider(override val name: String, coinCode: CoinCode, private val networkManager: INetworkManager, private val adapter: FullTransactionInfoModule.Adapter)
    : FullTransactionInfoModule.Provider {

    private val apiURL = when (coinCode) {
        "ETH" -> "https://api.etherscan.io"
        "ETHt" -> "https://api-ropsten.etherscan.io"
        else -> throw Exception("Invalid coin type")
    }

    override val url = when (coinCode) {
        "ETH" -> "https://etherscan.io/tx/"
        "ETHt" -> "https://ropsten.etherscan.io/tx/"
        else -> throw Exception("Invalid coin type")
    }

    override fun retrieveTransactionInfo(transactionHash: String): Flowable<FullTransactionRecord> {
        return networkManager
                .getTransaction(apiURL, "$apiURL/api?module=proxy&action=eth_getTransactionByHash&txhash=$transactionHash")
                .map { mapResponse(it) }
                .map {
                    adapter.convert(it)
                }
    }

    private fun mapResponse(json: JsonObject): FullTransactionResponse? {
        val result = json.asJsonObject.get("result")
                ?: return null

        return Gson().fromJson(result, Response::class.java)
    }

    class Response(
            @SerializedName("hash") override val hash: String,
            @SerializedName("from") override val from: String,
            @SerializedName("to") override val to: String,
            @SerializedName("nonce") val gNonce: String,
            @SerializedName("value") val amount: String,
            @SerializedName("gas") val gLimit: String,
            @SerializedName("gasPrice") val price: String,
            @SerializedName("blockNumber") val blockNumber: String
    ) : EthereumResponse() {
        override val date: Date? get() = null
        override val confirmations: Int? get() = null
        override val height: String
            get() = Integer.parseInt(blockNumber.substring(2), 16).toString()

        override val value: String
            get() = ValueFormatter.format(BigInteger(amount.substring(2), 16).toDouble() / ethRate)

        override val nonce: String
            get() = Integer.parseInt(gNonce.substring(2), 16).toString()

        override val gasLimit: String
            get() = BigInteger(gLimit.substring(2), 16).toString()

        override val gasPrice: String
            get() = (BigInteger(price.substring(2), 16).toDouble() / gweiRate).toInt().toString()

        override val gasUsed: String? get() = null
        override val fee: String? = null
        override val size: Int? get() = null
    }
}

