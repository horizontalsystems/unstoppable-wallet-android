package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.annotations.SerializedName
import java.util.*
import kotlin.collections.ArrayList

class BlockbookResponse(
        @SerializedName("hash") override val hash: String,
        @SerializedName("version") val version: Int,
        @SerializedName("blockHeight") override val height: Int,
        @SerializedName("fees") val feeString: String,
        @SerializedName("confirmations") val confirmationsInt: Int,
        @SerializedName("blockTime") val time: Long,
        @SerializedName("vin") val vin: ArrayList<Vin>,
        @SerializedName("vout") val vout: ArrayList<Vout>,
        @SerializedName("blockHash") val blockHash: String,
        @SerializedName("value") val value: String,
        @SerializedName("valueIn") val valueIn: String,
        @SerializedName("hex") val hex: String)
    : BitcoinResponse() {

    override val date: Date get() = Date(time * 1000)
    override val inputs: ArrayList<Input> get() = vin as ArrayList<Input>
    override val outputs: ArrayList<Output> get() = vout as ArrayList<Output>
    override val feePerByte: Double? get() = hex?.let { feeString.toDouble() / hex.length / 2 }
    override val confirmations: String get() = confirmationsInt.toString()
    override val fee: Double get() = feeString.toDouble() / btcRate
    override val size: Int get() = hex?.let { hex.length / 2 }

    class Vin(@SerializedName("value") val valueString: String,
              @SerializedName("addresses") val addresses: ArrayList<String> = ArrayList(),
              @SerializedName("n") val n: Int,
              @SerializedName("hex") val scriptSig: String) : Input() {
        override val value: Double get() = (valueString.toDouble() / btcRate)
        override val address: String get() = (addresses[0])
    }

    class Vout(@SerializedName("value") val valueString: String,
               @SerializedName("hex")  val scriptPubKey: String,
               @SerializedName("n")  val n: Int,
               @SerializedName("addresses") val addresses: ArrayList<String> = ArrayList()) : Output() {
        override val value: Double get() = (valueString.toDouble() / btcRate)
        override val address: String get() = (addresses[0])
    }
}