package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.annotations.SerializedName
import java.util.*

class ZcashResponse(
        @SerializedName("hash") override val hash: String,
        @SerializedName("blockHeight") override val height: Int,
        @SerializedName("fee") override val fee: Double,
        @SerializedName("timestamp") val timestamp: Long,
        @SerializedName("vin") override val inputs: ArrayList<Vin>,
        @SerializedName("vout") override val outputs: ArrayList<Vout>
) : BitcoinResponse() {

    override val size: Int? = null
    override val confirmations: String? = null
    override val feePerByte: Double? = null
    override val date: Date
        get() = Date(timestamp * 1000)

    class Vin(
            @SerializedName("retrievedVout") val retrievedVout: Map<String, Any>
    ) : Input() {

        override val value: Double
            get() = retrievedVout["value"] as? Double ?: 0.0

        override val address: String
            get() {
                val scriptPubKey = retrievedVout["scriptPubKey"] as? Map<String, Any> ?: mapOf()
                return (scriptPubKey["addresses"] as? List<*>)?.firstOrNull() as? String ?: ""
            }
    }

    class Vout(
            @SerializedName("value") override val value: Double,
            @SerializedName("scriptPubKey") val scriptPubKey: Map<String, Any>
    ) : Output() {

        override val address: String
            get() = (scriptPubKey["addresses"] as? List<*>)?.firstOrNull() as? String ?: ""
    }

}
