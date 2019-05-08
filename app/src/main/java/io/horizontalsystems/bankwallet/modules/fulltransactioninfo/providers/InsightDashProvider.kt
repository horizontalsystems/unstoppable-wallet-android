package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.BitcoinResponse
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import java.util.*

class InsightDashProvider : FullTransactionInfoModule.BitcoinForksProvider {
    override val name = "Insight.dash.org"

    override fun url(hash: String): String {
        return "https://insight.dash.org/insight/tx/$hash"
    }

    override fun apiUrl(hash: String): String {
        return "https://insight.dash.org/insight-api/tx/$hash"
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, InsightDashResponse::class.java)
    }
}

class InsightDashResponse(
        @SerializedName("txid") override val hash: String,
        @SerializedName("blockheight") override val height: Int,
        @SerializedName("size") override val size: Int?,
        @SerializedName("fees") override val fee: Double,
        @SerializedName("confirmations") override val confirmations: String?,
        @SerializedName("time") val time: Long,
        @SerializedName("vin") val vin: ArrayList<Vin>,
        @SerializedName("vout") val vout: ArrayList<Vout>
) : BitcoinResponse() {

    override val date: Date get() = Date(time * 1000)
    override val inputs: ArrayList<Input> get() = vin as ArrayList<Input>
    override val outputs: ArrayList<Output> get() = vout as ArrayList<Output>
    override val feePerByte: Double? get() = size?.let { fee * btcRate / size.toDouble() }

    class Vin(@SerializedName("value") override val value: Double, @SerializedName("addr") override val address: String) : Input()

    class Vout(@SerializedName("value") override val value: Double, @SerializedName("scriptPubKey") val scriptPubKey: Map<String, Any>) : Output() {
        override val address get() = (scriptPubKey["addresses"] as? List<*>)?.firstOrNull() as? String ?: ""
    }
}
