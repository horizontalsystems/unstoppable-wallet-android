package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.BitcoinResponse
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionResponse
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import java.util.*

class BtcComProvider(override val name: String, coinCode: CoinCode, private val networkManager: INetworkManager, private val adapter: FullTransactionInfoModule.Adapter)
    : FullTransactionInfoModule.Provider {

    private val apiURL = when (coinCode) {
        "BTC" -> "https://chain.api.btc.com"
        "BCH" -> "https://bch-chain.api.btc.com"
        else -> throw Exception("Invalid coin type")
    }

    override val url = when (coinCode) {
        "BTC" -> "https://btc.com/"
        "BCH" -> "https://bch.btc.com/"
        else -> throw Exception("Invalid coin type")
    }

    override fun retrieveTransactionInfo(transactionHash: String): Flowable<FullTransactionRecord> {
        return networkManager
                .getTransaction(apiURL, "$apiURL/v3/tx/$transactionHash")
                .map { mapResponse(it) }
                .map {
                    adapter.convert(it)
                }
    }

    private fun mapResponse(json: JsonObject): FullTransactionResponse? {
        return Gson().fromJson(json["data"], Response::class.java)
    }

    class Response(
            @SerializedName("confirmations") override val confirmations: String,
            @SerializedName("block_height") override val height: Int,
            @SerializedName("block_time") val time: Long,
            @SerializedName("fee") val fees: Int,
            @SerializedName("size") override val size: Int,
            @SerializedName("hash") override val hash: String,
            @SerializedName("inputs") val vin: ArrayList<Vin>,
            @SerializedName("outputs") val vout: ArrayList<Vout>) : BitcoinResponse() {

        override val fee get() = fees.toDouble() / btcRate
        override val feePerByte: Double? get() = null
        override val date: Date get() = Date(time * 1000)
        override val inputs get() = vin as ArrayList<Input>
        override val outputs get() = vout as ArrayList<Output>

        class Vin(@SerializedName("prev_value") val amount: Long, @SerializedName("prev_addresses") val addresses: ArrayList<String>) : BitcoinResponse.Input() {
            override val value: Double
                get() = amount / BitcoinResponse.btcRate

            override val address: String
                get() = addresses.firstOrNull() ?: ""
        }

        class Vout(@SerializedName("value") val amount: Double, @SerializedName("addresses") val addresses: ArrayList<String>) : BitcoinResponse.Output() {
            override val value: Double
                get() = amount / BitcoinResponse.btcRate

            override val address: String
                get() = addresses.firstOrNull() ?: ""
        }
    }
}

