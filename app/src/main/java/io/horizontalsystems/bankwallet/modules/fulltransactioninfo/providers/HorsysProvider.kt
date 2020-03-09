package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Request.GetRequest
import java.util.*

class HorsysBitcoinProvider(val testMode: Boolean) : FullTransactionInfoModule.BitcoinForksProvider {
    private val baseUrl = "${if (testMode) "http://btc-testnet" else "https://btc"}.horizontalsystems.xyz"

    override val name = "HorizontalSystems.xyz"
    override val pingUrl = "$baseUrl/apg/block/0"
    override val isTrusted: Boolean = true

    override fun url(hash: String): String? {
        return null
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        val url = "$baseUrl/apg/tx/$hash"
        return GetRequest(url)
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, HorsysBTCResponse::class.java)
    }
}

class HorsysDashProvider(val testMode: Boolean) : FullTransactionInfoModule.BitcoinForksProvider {
    private val baseUrl = "${if (testMode) "http://dash-testnet" else "https://dash"}.horizontalsystems.xyz"

    override val name: String = "HorizontalSystems.xyz"
    override val pingUrl = "$baseUrl/apg/block/0"
    override val isTrusted: Boolean = true

    override fun url(hash: String): String {
        return "$baseUrl/insight/tx/$hash"
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        return GetRequest("$baseUrl/apg/tx/$hash")
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, InsightResponse::class.java)
    }
}

class HorsysBTCResponse(
        @SerializedName("fee") val fees: Int,
        @SerializedName("time") val time: Long,
        @SerializedName("rate") val rate: Int,
        @SerializedName("inputs") val vin: ArrayList<Vin>,
        @SerializedName("outputs") val vout: ArrayList<Vout>,
        @SerializedName("hash") override val hash: String,
        @SerializedName("height") override val height: Int,
        @SerializedName("confirmations") override val confirmations: String) : BitcoinResponse() {

    override val date get() = Date(time * 1000)
    override val inputs get() = vin as ArrayList<Input>
    override val outputs get() = vout as ArrayList<Output>
    override val feePerByte get() = rate.toDouble() / 1000
    override val fee: Double get() = fees / btcRate
    override val size: Int? get() = ((fee / feePerByte) * btcRate).toInt()

    class Vin(@SerializedName("coin") val coin: BCoin) : Input() {
        override val value get() = coin.amount.toDouble() / btcRate
        override val address get() = coin.addr
    }

    class Vout(@SerializedName("value") val amount: Int, @SerializedName("address") val addr: String) : Output() {
        override val value get() = amount.toDouble() / btcRate
        override val address get() = addr
    }

    class BCoin(@SerializedName("value") val amount: Int, @SerializedName("address") val addr: String)
}
