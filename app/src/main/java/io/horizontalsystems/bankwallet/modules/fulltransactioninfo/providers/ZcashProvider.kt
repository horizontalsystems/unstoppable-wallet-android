package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule

class ZcashProvider: FullTransactionInfoModule.BitcoinForksProvider {
    private val baseApiUrl = "https://zcha.in/v2/mainnet"

    override val name = "Explorer.zcha.in"
    override val pingUrl = "$baseApiUrl/network"
    override val isTrusted: Boolean = true

    override fun url(hash: String): String {
        return "https://explorer.zcha.in/transactions/$hash"
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        return FullTransactionInfoModule.Request.GetRequest("$baseApiUrl/transactions/$hash")
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, ZcashResponse::class.java)
    }
}
