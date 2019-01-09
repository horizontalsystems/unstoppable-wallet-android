package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.BitcoinResponse
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.BitcoinResponse.Companion.btcRate
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionResponse
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import java.text.SimpleDateFormat
import java.util.*

class BlockchairBitcoinProvider(override val name: String, coinCode: CoinCode, private val networkManager: INetworkManager, private val adapter: FullTransactionInfoModule.Adapter)
    : FullTransactionInfoModule.Provider {

    private val apiUrl = when (coinCode) {
        "BTC" -> "https://api.blockchair.com/bitcoin/dashboards/transaction/"
        "BCH" -> "https://api.blockchair.com/bitcoin-cash/dashboards/transaction/"
        else -> throw Exception("Invalid coin type")
    }

    override val url = when (coinCode) {
        "BTC" -> "https://blockchair.com/bitcoin/transaction/"
        "BCH" -> "https://blockchair.com/bitcoin-cash/transaction/"
        else -> throw Exception("Invalid coin type")
    }

    override fun retrieveTransactionInfo(transactionHash: String): Flowable<FullTransactionRecord> {
        return networkManager
                .getTransaction(apiUrl, "$apiUrl$transactionHash")
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

        class Data(@SerializedName("transaction") val transaction: Transaction, @SerializedName("inputs") val vin: ArrayList<Vin>, @SerializedName("outputs") val vout: ArrayList<Vout>) : BitcoinResponse() {
            override val hash get() = transaction.hash
            override val height get() = transaction.height
            override val fee get() = transaction.fee.toDouble() / btcRate
            override val size get() = transaction.size
            override val feePerByte: Double? get() = null
            override val confirmations: String? = null
            override val inputs get() = vin as ArrayList<Input>
            override val outputs get() = vout as ArrayList<Output>
            override val date: Date
                get() {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    return dateFormat.parse(transaction.time)
                }
        }

        class Vin(@SerializedName("value") val amount: Double, @SerializedName("recipient") override val address: String) : BitcoinResponse.Input() {
            override val value: Double
                get() = amount / btcRate
        }

        class Vout(@SerializedName("value") val amount: Double, @SerializedName("recipient") override val address: String) : BitcoinResponse.Output() {
            override val value: Double
                get() = amount / btcRate
        }

        class Transaction(
                @SerializedName("hash") val hash: String,
                @SerializedName("time") val time: String,
                @SerializedName("size") val size: Int,
                @SerializedName("block_id") val height: Int,
                @SerializedName("fee") val fee: Int)
    }
}

