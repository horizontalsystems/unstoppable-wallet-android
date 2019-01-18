package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.BitcoinResponse
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import java.util.*

class BlockExplorerBitcoinProvider : FullTransactionInfoModule.BitcoinForksProvider {
    override val name = "BlockExplorer.com"

    override fun url(hash: String): String {
        return "https://blockexplorer.com/tx/$hash"
    }

    override fun apiUrl(hash: String): String {
        return "https://blockexplorer.com/api/tx/$hash"
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, BlockExplorerResponse::class.java)
    }
}

class BlockExplorerBitcoinCashProvider : FullTransactionInfoModule.BitcoinForksProvider {
    override val name = "BlockExplorer.com"

    override fun url(hash: String): String {
        return "https://bitcoincash.blockexplorer.com/tx/$hash"
    }

    override fun apiUrl(hash: String): String {
        return "https://bitcoincash.blockexplorer.com/api/tx/$hash"
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, BlockExplorerResponse::class.java)
    }
}

class BlockExplorerResponse(
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
    override val feePerByte get() = fees * btcRate / size
    override val confirmations get() = confirms.toString()
    override val inputs get() = vin as ArrayList<Input>
    override val outputs get() = vout as ArrayList<Output>

    class Vin(@SerializedName("valueSat") val amount: Int, @SerializedName("addr") override val address: String) : Input() {
        override val value: Double get() = amount / btcRate
    }

    class Vout(@SerializedName("value") val amount: String, @SerializedName("scriptPubKey") val pubKey: PubKey) : Output() {
        override val value: Double get() = amount.toDouble()
        override val address: String get() = pubKey.addresses.firstOrNull() ?: ""
    }

    class PubKey(@SerializedName("addresses") val addresses: ArrayList<String>)
}
