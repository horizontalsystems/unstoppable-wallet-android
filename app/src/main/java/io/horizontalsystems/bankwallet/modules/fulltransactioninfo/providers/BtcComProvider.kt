package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Request.GetRequest
import java.util.*

class BtcComBitcoinProvider : FullTransactionInfoModule.BitcoinForksProvider {
    private val baseApiUrl = "https://chain.api.btc.com/v3"

    override val name = "Btc.com"
    override val pingUrl = "$baseApiUrl/block/0"
    override val isTrusted: Boolean = true

    override fun url(hash: String): String {
        return "https://btc.com/$hash"
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        return GetRequest("$baseApiUrl/tx/$hash")
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json["data"], BtcComResponse::class.java)
    }
}

class BtcComBitcoinCashProvider : FullTransactionInfoModule.BitcoinForksProvider {
    private val baseApiUrl = "https://bch-chain.api.btc.com/v3"

    override val name = "Btc.com"
    override val pingUrl = "$baseApiUrl/block/0"
    override val isTrusted: Boolean = true

    override fun url(hash: String): String {
        return "https://bch.btc.com/$hash"
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        return GetRequest("$baseApiUrl/tx/$hash")
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json["data"], BtcComResponse::class.java)
    }
}

class BtcComResponse(
        @SerializedName("fee") val fees: Int,
        @SerializedName("inputs") val vin: ArrayList<Vin>,
        @SerializedName("outputs") val vout: ArrayList<Vout>,
        @SerializedName("confirmations") override val confirmations: String,
        @SerializedName("hash") override val hash: String,
        @SerializedName("block_height") override val height: Int,
        @SerializedName("block_time") val time: Long,
        @SerializedName("size") override val size: Int) : BitcoinResponse() {

    override val fee get() = fees.toDouble() / btcRate
    override val feePerByte: Double? get() = null
    override val date get() = Date(time * 1000)
    override val inputs get() = vin
    override val outputs get() = vout

    class Vin(@SerializedName("prev_value") val amount: Long, @SerializedName("prev_addresses") val addresses: ArrayList<String>) : Input() {
        override val value: Double get() = amount / btcRate
        override val address: String get() = addresses.firstOrNull() ?: ""
    }

    class Vout(@SerializedName("value") val amount: Double, @SerializedName("addresses") val addresses: ArrayList<String>) : Output() {
        override val value: Double get() = amount / btcRate
        override val address: String get() = addresses.firstOrNull() ?: ""
    }
}
