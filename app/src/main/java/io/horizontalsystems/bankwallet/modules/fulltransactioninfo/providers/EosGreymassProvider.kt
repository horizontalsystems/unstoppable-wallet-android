package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule

class EosGreymassProvider : FullTransactionInfoModule.EosProvider {

    override val name = "Greymass.com"
    override val pingUrl = "https://eos.greymass.com/"

    override fun url(hash: String): String? {
        return null
    }

    override fun apiRequest(hash: String): FullTransactionInfoModule.Request {
        return FullTransactionInfoModule.Request.PostRequest("https://eos.greymass.com/v1/history/get_transaction", hashMapOf("id" to hash))
    }

    override fun convert(json: JsonObject, eosAccount: String): EosResponse {
        return EosProviderResponse(json, eosAccount)
    }

}
