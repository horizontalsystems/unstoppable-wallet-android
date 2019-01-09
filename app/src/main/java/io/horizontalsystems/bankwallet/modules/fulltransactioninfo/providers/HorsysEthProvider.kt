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
import io.reactivex.Flowable
import java.math.BigInteger
import java.util.*

class HorsysEthProvider(override val name: String, coinCode: CoinCode, private val networkManager: INetworkManager, private val adapter: FullTransactionInfoModule.Adapter)
    : FullTransactionInfoModule.Provider {

    override val url = when (coinCode) {
        "ETH" -> "https://eth.horizontalsystems.xyz/tx/"
        "ETHt" -> "https://eth-testnet.horizontalsystems.xyz/tx/"
        else -> throw Exception("Invalid coin type")
    }

    override fun retrieveTransactionInfo(transactionHash: String): Flowable<FullTransactionRecord> {
        return networkManager
                .getTransaction(url, "$url$transactionHash")
                .map { mapResponse(it) }
                .map {
                    adapter.convert(it)
                }
    }

    private fun mapResponse(json: JsonObject): FullTransactionResponse {
        return Gson().fromJson(json["tx"], Response::class.java)
    }

    class Response(
            @SerializedName("hash") override val hash: String,
            @SerializedName("from") override val from: String,
            @SerializedName("to") override val to: String,
            @SerializedName("fee") override val fee: String,
            @SerializedName("nonce") val gNonce: String,
            @SerializedName("value") val amount: String,
            @SerializedName("gas") override val gasLimit: String,
            @SerializedName("gasPrice") val price: String,
            @SerializedName("gasUsed") override val gasUsed: String,
            @SerializedName("blockNumber") val blockNumber: String
    ) : EthereumResponse() {
        override val size: Int? get() = null
        override val date: Date? get() = null
        override val confirmations: Int? get() = null
        override val height: String
            get() = Integer.parseInt(blockNumber, 16).toString()

        override val value: String
            get() = (amount.toDouble() / ethRate).toString()

        override val nonce: String
            get() = Integer.parseInt(gNonce, 16).toString()

        override val gasPrice: String
            get() = (BigInteger(price, 16).toDouble() / gweiRate).toInt().toString()
    }
}

