package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import com.google.gson.JsonParser
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule

object WC2Parser {

    fun getChainId(chain: String): Int? {
        val splitted = chain.split(":")
        if (splitted.size >= 2) {
            return splitted[1].toIntOrNull()
        }
        return null
    }

    fun getChainName(chain: String): String? {
        val chainId = getChainId(chain) ?: return null
        return WalletConnectListModule.Chain.values().firstOrNull { it.value == chainId }?.title
    }

    fun getSessionRequestMethod(body: String?): String? {
        body?.let { string ->
            val parsed = JsonParser.parseString(string)
            if (parsed.isJsonObject) {
                val params = parsed.asJsonObject.get("params").asJsonObject
                val request = params.get("request").asJsonObject
                return request.get("method").asString
            }
        }
        return null
    }

    fun getSessionRequestDomainName(body: String?): String? {
        body?.let { string ->
            val parsed = JsonParser.parseString(string)
            if (parsed.isJsonObject) {
                return parsed.asJsonObject.get("domain").asJsonObject.get("name").asString
            }
        }
        return null
    }

    fun getMessageData(body: String?, address: String): String? {
        body?.let { string ->
            val parsed = JsonParser.parseString(string)
            if (parsed.isJsonObject) {
                val params = parsed.asJsonObject.get("params").asJsonObject
                val innerParams = params.get("request").asJsonObject.get("params").asJsonArray
                return innerParams.firstOrNull { it.asString != address }?.asString
            }
        }
        return null
    }

    fun getChainIdFromBody(body: String?): Int? {
        body?.let { string ->
            val parsed = JsonParser.parseString(string)
            if (parsed.isJsonObject) {
                val chainIdData = parsed.asJsonObject.get("params").asJsonObject.get("chainId").asString
                return getChainId(chainIdData)
            }
        }
        return null
    }

}
