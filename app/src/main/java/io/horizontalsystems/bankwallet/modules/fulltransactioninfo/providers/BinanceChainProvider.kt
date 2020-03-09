package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.adapters.BinanceAdapter
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Request.GetRequest
import java.math.BigDecimal

class BinanceChainProvider(val testMode: Boolean) : FullTransactionInfoModule.BinanceProvider {
    private val baseApiUrl = "https://${if (testMode) "testnet-dex" else "dex"}.binance.org/api/v1"

    override val name = "Binance.org"
    override val pingUrl = "$baseApiUrl/node-info"
    override val isTrusted: Boolean = true

    override fun url(hash: String): String {
        return "https://${if (testMode) "testnet-explorer" else "explorer"}.binance.org/tx/$hash"
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        return GetRequest("$baseApiUrl/tx/$hash?format=json")
    }

    override fun convert(json: JsonObject): BinanceResponse {
        return BinanceChainResponse(json)
    }
}

class BinanceChainResponse(json: JsonObject) : BinanceResponse() {

    override val hash: String = json["hash"].asString
    override val blockHeight: String = json["height"].asString

    override var fee: BigDecimal = BinanceAdapter.transferFee
    override var value: BigDecimal = BigDecimal.ZERO
    override var from: String = ""
    override var to: String = ""
    override var memo: String = ""

    init {
        val tx = json["tx"].asJsonObject["value"].asJsonObject
        val msgs = tx["msg"].asJsonArray
        val msg = msgs.first().asJsonObject["value"].asJsonObject

        val output = msg["outputs"].asJsonArray.first().asJsonObject
        val input = msg["inputs"].asJsonArray.first().asJsonObject
        val coins = input["coins"].asJsonArray.first().asJsonObject

        from = input["address"].asString
        to = output["address"].asString
        value = coins["amount"].asBigDecimal
        memo = tx["memo"].asString
    }
}
