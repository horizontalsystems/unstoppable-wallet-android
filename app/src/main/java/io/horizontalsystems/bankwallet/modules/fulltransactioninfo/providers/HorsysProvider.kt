package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.core.utils.EthInputParser
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Request.GetRequest
import java.math.BigInteger
import java.util.*

class HorsysBitcoinProvider(val testMode: Boolean) : FullTransactionInfoModule.BitcoinForksProvider {
    override val name = "HorizontalSystems.xyz"

    override fun url(hash: String): String? {
        return null
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        val url = "${if (testMode) "http://btc-testnet" else "https://btc"}.horizontalsystems.xyz/apg/tx/$hash"
        return GetRequest(url)
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, HorsysBTCResponse::class.java)
    }
}

class HorsysDashProvider(val testMode: Boolean) : FullTransactionInfoModule.BitcoinForksProvider {
    override val name: String = "HorizontalSystems.xyz"

    override fun url(hash: String): String {
        return "${if (testMode) "http://dash-testnet" else "https://dash"}.horizontalsystems.xyz/insight/tx/$hash"
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        val url = "${if (testMode) "http://dash-testnet" else "https://dash"}.horizontalsystems.xyz/apg/tx/$hash"
        return GetRequest(url)
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, InsightResponse::class.java)
    }
}

class HorsysEthereumProvider(val testMode: Boolean) : FullTransactionInfoModule.EthereumForksProvider {

    private val url = if (testMode) "http://eth-ropsten.horizontalsystems.xyz/tx/" else "https://eth.horizontalsystems.xyz/tx/"
    private val apiUrl = if (testMode) "http://eth-ropsten.horizontalsystems.xyz/api?module=transaction&action=gettxinfo&txhash=" else "https://eth.horizontalsystems.xyz/api?module=transaction&action=gettxinfo&txhash="

    override val name: String = "HorizontalSystems.xyz"

    override fun url(hash: String): String = "$url$hash"

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        return GetRequest("$apiUrl$hash")
    }

    override fun convert(json: JsonObject): EthereumResponse {
        return Gson().fromJson(json["result"], HorsysETHResponse::class.java)
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

class HorsysETHResponse(
        @SerializedName("timeStamp") val time: String?,
        @SerializedName("nonce") val gNonce: String?,
        @SerializedName("value") val valueString: String,
        @SerializedName("blockNumber") val blockNumber: String,
        @SerializedName("confirmations") val confirmationsString: String?,
        @SerializedName("hash") override val hash: String,
        @SerializedName("from") override val from: String,
        @SerializedName("to") val receiver: String,
        @SerializedName("fee") override val fee: String,
        @SerializedName("input") val input: String,
        @SerializedName("gasLimit") override val gasLimit: String,
        @SerializedName("gasUsed") override val gasUsed: String) : EthereumResponse() {

    override val contractAddress: String? get() = if (input != "0x") receiver else null
    override val size: Int? get() = null
    override val date: Date? get() = time?.let { Date(it.toLong() * 1000) }
    override val confirmations: Int? get() = confirmationsString?.toIntOrNull()
    override val height: String get() = Integer.parseInt(blockNumber, 16).toString()
    override val gasPrice: String? get() = null

    override val value: BigInteger
        get() {
            var amountData = valueString.substring(2)
            if (input != "0x") {
                EthInputParser.parse(input)?.let {
                    amountData = it.value
                }
            }

            return BigInteger(amountData, 16)
        }

    override val nonce: String?
        get() = gNonce?.let { Integer.parseInt(it, 16).toString() }

    override val to: String
        get() {
            if (input != "0x") {
                EthInputParser.parse(input)?.let {
                    return "0x${it.to}"
                }
            }

            return receiver
        }
}
