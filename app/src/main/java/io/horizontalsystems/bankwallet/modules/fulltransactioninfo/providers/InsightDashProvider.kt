package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule

class InsightDashProvider : FullTransactionInfoModule.BitcoinForksProvider {
    override val name = "Insight.dash.org"

    override fun url(hash: String): String {
        return "https://insight.dash.org/insight/tx/$hash"
    }

    override fun apiUrl(hash: String): String {
        return "https://insight.dash.org/insight-api/tx/$hash"
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, InsightResponse::class.java)
    }
}
