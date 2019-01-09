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

class BlockexplorerBitcoinProvider(override val name: String, coinCode: CoinCode, private val networkManager: INetworkManager, private val adapter: FullTransactionInfoModule.Adapter)
    : FullTransactionInfoModule.Provider {

    override val url = when (coinCode) {
        "BTC" -> "https://blockexplorer.com/api/tx/"
        "BCH" -> "https://bitcoincash.blockexplorer.com/api/tx/"
        "BTCt" -> "https://testnet.blockexplorer.com/api/tx/"
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
        return Gson().fromJson(json, Response::class.java)
    }

    class Response(
            @SerializedName("txid") override val hash: String,
            @SerializedName("blockheight") override val height: Int,
            @SerializedName("fees") val fees: Double,
            @SerializedName("time") val time: Long,
            @SerializedName("size") override val size: Int,
            @SerializedName("confirmations") val confirms: Int,
            @SerializedName("vin") val vin: ArrayList<Vin>,
            @SerializedName("vout") val vout: ArrayList<Vout>) : BitcoinResponse() {

        override val date get() = Date(time * 1000)
        override val fee get() = fees
        override val feePerByte: Double? = null
        override val confirmations get() = confirms.toString()
        override val inputs get() = vin as ArrayList<Input>
        override val outputs get() = vout as ArrayList<Output>

        class Vin(@SerializedName("valueSat") val amount: Int, @SerializedName("addr") override val address: String) : Input() {
            override val value: Double
                get() = amount / btcRate
        }

        class Vout(@SerializedName("value") val amount: String, @SerializedName("scriptPubKey") val pubKey: PubKey) : Output() {
            override val value: Double
                get() = amount.toDouble()

            override val address: String
                get() = pubKey.addresses.firstOrNull() ?: ""
        }

        class PubKey(@SerializedName("addresses") val addresses: ArrayList<String>)
    }
}
