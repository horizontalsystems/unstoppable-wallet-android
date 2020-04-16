package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Request.GetRequest
import java.util.*

class HorsysBitcoinProvider(val testMode: Boolean) : FullTransactionInfoModule.BitcoinForksProvider {
    private val baseApiUrl = if (testMode) "https://btc-testnet.horizontalsystems.xyz/api" else "https://btc.horizontalsystems.xyz/apg"

    override val name = "HorizontalSystems.xyz"
    override val pingUrl = "$baseApiUrl/block/0"
    override val isTrusted: Boolean = true

    override fun url(hash: String): String? {
        return null
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        val url = "$baseApiUrl/tx/$hash"
        return GetRequest(url)
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, HorsysBTCResponse::class.java)
    }
}

class HorsysLitecoinProvider(val testMode: Boolean) : FullTransactionInfoModule.BitcoinForksProvider {
    private val baseUrl = "${if (testMode) "https://ltc-testnet" else "https://ltc"}.horizontalsystems.xyz"

    override val name = "HorizontalSystems.xyz"
    override val pingUrl = "$baseUrl/api/block/0"
    override val isTrusted: Boolean = true

    override fun url(hash: String): String? {
        return null
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        val url = "$baseUrl/api/tx/$hash"
        return GetRequest(url)
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, HorsysLitecoinResponse::class.java)
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
    override val inputs get() = vin
    override val outputs get() = vout
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

class HorsysLitecoinResponse(
        @SerializedName("fee") override val fee: Double,
        @SerializedName("ts") val time: Long,
        @SerializedName("rate") val rateString: String,
        @SerializedName("inputs") val vin: ArrayList<Vin>,
        @SerializedName("outputs") val vout: ArrayList<Vout>,
        @SerializedName("hash") override val hash: String,
        @SerializedName("height") override val height: Int,
        @SerializedName("confirmations") override val confirmations: String) : BitcoinResponse() {

    override val date get() = Date(time * 1000)
    override val inputs get() = vin.filter { it.coin != null }
    override val outputs get() = vout
    override val feePerByte: Double?
        get() = rateString.toDoubleOrNull()?.let {
            it * btcRate / 1000
        }

    override val size: Int? get() = feePerByte?.let { ((fee / it) * btcRate).toInt() }

    class Vin(@SerializedName("coin") val coin: BCoin?) : Input() {
        override val value get() = coin?.let { it.amount / btcRate } ?: 0.0
        override val address get() = coin?.address ?: ""
    }

    class Vout(@SerializedName("value") override val value: Double,
               @SerializedName("address") override val address: String) : Output()

    class BCoin(@SerializedName("value") val amount: Double,
                @SerializedName("address") val address: String)
}
