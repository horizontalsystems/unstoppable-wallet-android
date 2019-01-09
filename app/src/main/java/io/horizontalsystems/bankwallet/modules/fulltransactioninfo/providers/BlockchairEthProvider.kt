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
import java.text.SimpleDateFormat
import java.util.*

class BlockchairEthProvider(override val name: String, coinCode: CoinCode, private val networkManager: INetworkManager, private val adapter: FullTransactionInfoModule.Adapter)
    : FullTransactionInfoModule.Provider {

    private val apiUrl = when (coinCode) {
        "ETH" -> "https://api.blockchair.com"
        else -> throw Exception("Invalid coin type")
    }

    override val url = when (coinCode) {
        "ETH" -> "https://blockchair.com/ethereum/transaction/"
        else -> throw Exception("Invalid coin type")
    }

    override fun retrieveTransactionInfo(transactionHash: String): Flowable<FullTransactionRecord> {
        return networkManager
                .getTransaction(apiUrl, "$apiUrl/ethereum/dashboards/transaction/$transactionHash")
                .map { mapResponse(it) }
                .map {
                    adapter.convert(it)
                }
    }

    private fun mapResponse(json: JsonObject): FullTransactionResponse {
        val response = Gson().fromJson(json, Response::class.java)
        val transaction = response.data.entries.firstOrNull()
                ?: throw Exception("Failed to parse transaction response")

        return transaction.value
    }

    class Response(@SerializedName("data") val data: Map<String, Data>) : FullTransactionResponse {

        class Data(@SerializedName("transaction") val transaction: Transaction) : EthereumResponse() {
            override val size: Int? get() = transaction.size
            override val hash get() = transaction.hash
            override val height get() = transaction.height.toString()
            override val fee get() = (transaction.fee.toDouble() / ethRate).toString()
            override val from get() = transaction.sender
            override val to get() = transaction.recipient
            override val value get() = ValueFormatter.format(transaction.value.toBigInteger().toDouble() / ethRate)
            override val nonce get() = transaction.nonce.toInt().toString()
            override val gasLimit get() = transaction.gasLimit.toString()
            override val gasPrice get() = (transaction.gasPrice / gweiRate).toString()
            override val gasUsed get () = transaction.gasUsed.toString()
            override val confirmations: Int? get() = null
            override val date: Date
                get() {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    return dateFormat.parse(transaction.time)
                }
        }

        class Transaction(
                @SerializedName("hash") val hash: String,
                @SerializedName("time") val time: String,
                @SerializedName("size") val size: Int,
                @SerializedName("block_id") val height: Int,
                @SerializedName("fee") val fee: String,
                @SerializedName("gas_limit") val gasLimit: Long,
                @SerializedName("gas_price") val gasPrice: Long,
                @SerializedName("gas_used") val gasUsed: Long,
                @SerializedName("nonce") val nonce: String,
                @SerializedName("value") val value: String,
                @SerializedName("sender") val sender: String,
                @SerializedName("recipient") val recipient: String
        )
    }
}

