package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.core.utils.EthInputParser
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Request.GetRequest
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

class BlockChairBitcoinProvider : FullTransactionInfoModule.BitcoinForksProvider {
    private val baseApiUrl = "https://api.blockchair.com/bitcoin"

    override val name = "BlockChair.com"
    override val pingUrl = "$baseApiUrl/stats"

    override fun url(hash: String): String {
        return "https://blockchair.com/bitcoin/transaction/$hash"
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        return GetRequest("$baseApiUrl/dashboards/transaction/$hash")
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        val response = Gson().fromJson(json, BlockchairBTCResponse::class.java)
        val transaction = response.data.entries.firstOrNull()
                ?: throw Exception("Failed to parse transaction response")

        return transaction.value
    }
}

class BlockChairBitcoinCashProvider : FullTransactionInfoModule.BitcoinForksProvider {
    private val baseApiUrl = "https://api.blockchair.com/bitcoin-cash"

    override val name = "BlockChair.com"
    override val pingUrl = "$baseApiUrl/stats"

    override fun url(hash: String): String {
        return "https://blockchair.com/bitcoin-cash/transaction/$hash"
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        return GetRequest("$baseApiUrl/dashboards/transaction/$hash")
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        val response = Gson().fromJson(json, BlockchairBTCResponse::class.java)
        val transaction = response.data.entries.firstOrNull()
                ?: throw Exception("Failed to parse transaction response")

        return transaction.value
    }
}

class BlockChairDashProvider : FullTransactionInfoModule.BitcoinForksProvider {
    private val baseApiUrl = "https://api.blockchair.com/dash"

    override val name = "BlockChair.com"
    override val pingUrl = "$baseApiUrl/stats"

    override fun url(hash: String): String {
        return "https://blockchair.com/dash/transaction/$hash"
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        return GetRequest("$baseApiUrl/dashboards/transaction/$hash")
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        val response = Gson().fromJson(json, BlockchairBTCResponse::class.java)
        val transaction = response.data.entries.firstOrNull()
                ?: throw Exception("Failed to parse transaction response")

        return transaction.value
    }
}

class BlockChairEthereumProvider : FullTransactionInfoModule.EthereumForksProvider {
    private val baseApiUrl = "https://api.blockchair.com/ethereum"

    override val name = "BlockChair.com"
    override val pingUrl = "$baseApiUrl/stats"

    override fun url(hash: String): String {
        return "https://blockchair.com/ethereum/transaction/$hash"
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        return GetRequest("$baseApiUrl/dashboards/transaction/$hash")
    }

    override fun convert(json: JsonObject): EthereumResponse {
        val response = Gson().fromJson(json, BlockchairETHResponse::class.java)
        val transaction = response.data.entries.firstOrNull()
                ?: throw Exception("Failed to parse transaction response")

        return transaction.value
    }
}

class BlockchairBTCResponse(@SerializedName("data") val data: Map<String, Data>) {

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

    class Transaction(
            @SerializedName("hash") val hash: String,
            @SerializedName("time") val time: String,
            @SerializedName("size") val size: Int,
            @SerializedName("block_id") val height: Int,
            @SerializedName("fee") val fee: Int
    )

    class Vin(@SerializedName("value") val amount: Double, @SerializedName("recipient") override val address: String) : BitcoinResponse.Input() {
        override val value: Double
            get() = amount / BitcoinResponse.btcRate
    }

    class Vout(@SerializedName("value") val amount: Double, @SerializedName("recipient") override val address: String) : BitcoinResponse.Output() {
        override val value: Double
            get() = amount / BitcoinResponse.btcRate
    }
}

class BlockchairETHResponse(@SerializedName("data") val data: Map<String, Data>) : FullTransactionResponse {

    class Data(@SerializedName("transaction") val transaction: Transaction) : EthereumResponse() {
        override val size: Int? get() = null
        override val hash get() = transaction.hash
        override val height get() = transaction.height.toString()
        override val fee get() = (transaction.fee.toDouble() / ethRate).toString()
        override val from get() = transaction.from
        override val to get() = transaction.to
        override val value get() = transaction.amount
        override val nonce get() = transaction.nonce.toInt().toString()
        override val gasLimit get() = transaction.gasLimit.toString()
        override val gasPrice get() = (transaction.gasPrice / gweiRate).toString()
        override val gasUsed get() = transaction.gasUsed.toString()
        override val confirmations: Int? get() = null
        override val contractAddress: String?
            get() {
                return if (transaction.input != "") {
                    transaction.recipient
                } else {
                    null
                }
            }
        override val date: Date?
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
            @SerializedName("sender") val from: String,
            @SerializedName("recipient") var recipient: String,
            @SerializedName("input_hex") val input: String
    ) {
        val amount: BigInteger
            get() {
                var data = BigInteger(value)
                if (input != "") {
                    EthInputParser.parse(input)?.let {
                        data = BigInteger(it.value, 16)
                    }
                }

                return data
            }

        val to: String
            get() {
                var address = recipient
                if (input != "") {
                    EthInputParser.parse(input)?.let {
                        address = "0x${it.to}"
                    }
                }

                return address
            }
    }
}
