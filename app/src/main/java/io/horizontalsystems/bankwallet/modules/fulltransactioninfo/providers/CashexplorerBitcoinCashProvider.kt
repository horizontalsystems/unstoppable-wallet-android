package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Request.GetRequest

class CashexplorerBitcoinCashProvider : FullTransactionInfoModule.BitcoinForksProvider {
    override val name = "Cashexplorer.bitcoin.com"
    override val pingUrl = "https://cashexplorer.bitcoin.com/api/sync"

    private val baseUrl = "https://cashexplorer.bitcoin.com"

    override fun url(hash: String): String {
        return "$baseUrl/tx/$hash"
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        return GetRequest("$baseUrl/api/tx/$hash", false)
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, InsightResponse::class.java)
    }
}
