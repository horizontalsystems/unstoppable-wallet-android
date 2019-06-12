package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.BitcoinResponse
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule

class BlockdozerBitcoinCashProvider(private val testMode: Boolean): FullTransactionInfoModule.BitcoinForksProvider {
    override val name = "Blockdozer.com"

    private val baseUrl = "https://${if (testMode) "tbch." else ""}blockdozer.com"

    override fun url(hash: String): String {
        return "$baseUrl/tx/$hash"
    }

    override fun apiUrl(hash: String): String {
        return "$baseUrl/api/tx/$hash"
    }

    override fun convert(json: JsonObject): BitcoinResponse {
        return Gson().fromJson(json, InsightResponse::class.java)
    }
}
