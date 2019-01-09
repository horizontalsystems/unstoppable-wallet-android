package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.BitcoinResponse
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionResponse
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import java.util.*

class HorsysBitcoinProvider(override val name: String, coinCode: CoinCode, private val networkManager: INetworkManager, private val adapter: FullTransactionInfoModule.Adapter)
    : FullTransactionInfoModule.Provider {

    override val url = when (coinCode) {
        "BTC" -> "https://btc.horizontalsystems.xyz/tx/"
        "BCH" -> "https://bch.horizontalsystems.xyz/tx/"
        "BCHt" -> "https://btc-testnet.horizontalsystems.xyz/tx/"
        "BTCt" -> "https://bch-testnet.horizontalsystems.xyz/tx/"
        else -> throw Exception("Invalid coin type")
    }

    override fun retrieveTransactionInfo(transactionHash: String): Flowable<FullTransactionRecord> {
        return networkManager
                .getTransaction(url, "$url$transactionHash")
                .map { mapResponse(it) }
                .map {
                    adapter.convert(it)
                }
    }

    private fun mapResponse(json: JsonObject): FullTransactionResponse? {
        return Gson().fromJson(json, Response::class.java)
    }

    class Response(
            @SerializedName("hash") override val hash: String,
            @SerializedName("height") override val height: Int,
            @SerializedName("fee") val fees: Int,
            @SerializedName("time") val time: Long,
            @SerializedName("rate") val rate: Int,
            @SerializedName("confirmations") override val confirmations: String,
            @SerializedName("inputs") val vin: ArrayList<Vin>,
            @SerializedName("outputs") val vout: ArrayList<Vout>) : BitcoinResponse() {

        override val date get() = Date(time * 1000)
        override val inputs get() = vin as ArrayList<Input>
        override val outputs get() = vout as ArrayList<Output>
        override val feePerByte get() = rate.toDouble() / 1000
        override val fee: Double
            get() = fees / btcRate
        override val size: Int?
            get() = ((fee / feePerByte) * btcRate).toInt()

        class Vin(@SerializedName("coin") val coin: Coin) : Input() {
            override val value get() = coin.amount.toDouble() / btcRate
            override val address get() = coin.addr
        }

        class Vout(@SerializedName("value") val amount: Int, @SerializedName("address") val addr: String) : Output() {
            override val value get() = amount.toDouble() / btcRate
            override val address get() = addr
        }

        class Coin(@SerializedName("value") val amount: Int, @SerializedName("address") val addr: String)
    }
}

