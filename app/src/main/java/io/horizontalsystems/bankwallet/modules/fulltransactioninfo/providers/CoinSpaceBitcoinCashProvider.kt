package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Request.GetRequest

class CoinSpaceBitcoinCashProvider : FullTransactionInfoModule.BitcoinForksProvider {
    override val name = "Coin.space"
    override val pingUrl = "https://bch.coin.space/api/sync"

    private val baseUrl = "https://bch.coin.space"

    override fun url(hash: String): String {
        return "$baseUrl/tx/$hash"
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        return GetRequest("$baseUrl/api/tx/$hash")
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, InsightResponse::class.java)
    }
}
