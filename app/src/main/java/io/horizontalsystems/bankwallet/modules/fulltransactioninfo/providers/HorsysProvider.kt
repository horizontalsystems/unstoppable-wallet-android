package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.BitcoinResponse
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.EthereumResponse
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import java.math.BigInteger
import java.util.*

class HorsysBitcoinProvider(val testMode: Boolean) : FullTransactionInfoModule.BitcoinForksProvider {
    override val name = "HorizontalSystems.xyz"

    override fun url(hash: String): String {
        return "${if (testMode) "http://btc-testnet" else "https://btc"}.horizontalsystems.xyz/apg/tx/$hash"
    }

    override fun apiUrl(hash: String): String {
        return "${if (testMode) "http://btc-testnet" else "https://btc"}.horizontalsystems.xyz/apg/tx/$hash"
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, HorsysBTCResponse::class.java)
    }
}

class HorsysBitcoinCashProvider(val testMode: Boolean) : FullTransactionInfoModule.BitcoinForksProvider {
    override val name: String = "HorizontalSystems.xyz"

    override fun url(hash: String): String {
        return "${if (testMode) "http://bch-testnet" else "https://bch"}.horizontalsystems.xyz/apg/tx/$hash"
    }

    override fun apiUrl(hash: String): String {
        return "${if (testMode) "http://bch-testnet" else "https://bch"}.horizontalsystems.xyz/apg/tx/$hash"
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, HorsysBTCResponse::class.java)
    }
}

class HorsysEthereumProvider(val testMode: Boolean) : FullTransactionInfoModule.EthereumForksProvider {

    override val name: String = "HorizontalSystems.xyz"

    override fun url(hash: String): String {
        return "${if (testMode) "http://eth-testnet" else "https://eth"}.horizontalsystems.xyz/tx/$hash"
    }

    override fun apiUrl(hash: String): String {
        return "${if (testMode) "http://eth-testnet" else "https://eth"}.horizontalsystems.xyz/tx/$hash"
    }

    override fun convert(json: JsonObject): EthereumResponse {
        return Gson().fromJson(json["tx"], HorsysETHResponse::class.java)
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
        @SerializedName("nonce") val gNonce: String,
        @SerializedName("value") val amount: String,
        @SerializedName("gasPrice") val price: String,
        @SerializedName("blockNumber") val blockNumber: String,
        @SerializedName("hash") override val hash: String,
        @SerializedName("from") override val from: String,
        @SerializedName("to") override val to: String,
        @SerializedName("fee") override val fee: String,
        @SerializedName("gas") override val gasLimit: String,
        @SerializedName("gasUsed") override val gasUsed: String) : EthereumResponse() {

    override val contractAddress: String? get() = null
    override val size: Int? get() = null
    override val date: Date? get() = null
    override val confirmations: Int? get() = null
    override val height: String
        get() = Integer.parseInt(blockNumber, 16).toString()

    override val value: BigInteger
        get() = BigInteger(amount)

    override val nonce: String
        get() = Integer.parseInt(gNonce, 16).toString()

    override val gasPrice: String
        get() = (BigInteger(price, 16).toDouble() / gweiRate).toInt().toString()
}
