package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.Request.GetRequest

class BlockdozerBitcoinCashProvider(testMode: Boolean) : FullTransactionInfoModule.BitcoinForksProvider {
    override val name = "Blockdozer.com"
    override val pingUrl = "https://bch.blockdozer.com" //TODO blockdozer is down, maybe we should remove blockdozer provider

    private val baseUrl = "https://${if (testMode) "tbch." else ""}blockdozer.com"

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
