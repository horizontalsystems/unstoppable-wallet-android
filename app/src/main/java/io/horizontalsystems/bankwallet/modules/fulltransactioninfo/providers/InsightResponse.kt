package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.annotations.SerializedName
import java.util.*

class InsightResponse(
        @SerializedName("txid") override val hash: String,
        @SerializedName("blockheight") override val height: Int,
        @SerializedName("size") override val size: Int?,
        @SerializedName("fees") override val fee: Double,
        @SerializedName("confirmations") override val confirmations: String?,
        @SerializedName("time") val time: Long,
        @SerializedName("vin") val vin: ArrayList<Vin>,
        @SerializedName("vout") val vout: ArrayList<Vout>)
    : BitcoinResponse() {

    override val date get() = Date(time * 1000)
    override val inputs get() = vin
    override val outputs get() = vout
    override val feePerByte: Double? get() = size?.let { fee * btcRate / size.toDouble() }

    class Vin(@SerializedName("value") override val value: Double, @SerializedName("addr") override val address: String) : Input()

    class Vout(@SerializedName("value") override val value: Double, @SerializedName("scriptPubKey") val scriptPubKey: Map<String, Any>) : Output() {
        override val address get() = (scriptPubKey["addresses"] as? List<*>)?.firstOrNull() as? String ?: ""
    }
}
