package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.walletconnect.walletconnectv2.client.WalletConnect
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.SignMessage
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WCAccountData
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WCChain
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray

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
        return WCChain.values().firstOrNull { it.id == chainId }?.title
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

    fun getChainIdFromBody(body: String?): Int? {
        body?.let { string ->
            val parsed = JsonParser.parseString(string)
            if (parsed.isJsonObject) {
                val chainIdData =
                    parsed.asJsonObject.get("params").asJsonObject.get("chainId").asString
                return getChainId(chainIdData)
            }
        }
        return null
    }

    fun parseTransactionRequest(
        request: WalletConnect.Model.JsonRpcHistory.HistoryEntry,
        address: String,
        dAppName: String
    ): WC2Request? {
        request.body?.let { string ->
            val parsed = JsonParser.parseString(string)
            if (parsed.isJsonObject) {
                val requestNode =
                    parsed.asJsonObject.get("params").asJsonObject.get("request").asJsonObject
                when (requestNode.get("method").asString) {
                    "eth_sendTransaction" -> {
                        val params = requestNode.get("params").asJsonArray.first()
                        val transaction =
                            Gson().fromJson(params, WC2EthereumTransaction::class.java)
                        return WC2SendEthereumTransactionRequest(
                            request.requestId,
                            request.topic,
                            transaction
                        )
                    }
                    "personal_sign" -> {
                        val dataString = requestNode.get("params").asJsonArray
                            .firstOrNull { it.asString != address }?.asString ?: ""
                        val data = hexStringToUtf8String(dataString)
                        return WC2SignMessageRequest(
                            request.requestId,
                            request.topic,
                            dataString,
                            SignMessage.PersonalMessage(data)
                        )
                    }
                    "eth_signTypedData" -> {
                        val dataString = requestNode.get("params").asJsonArray
                            .firstOrNull { it.asString != address }?.asString ?: ""
                        val data = hexStringToUtf8String(dataString)
                        val domain = getSessionRequestDomainName(data) ?: ""
                        val message = SignMessage.TypedMessage(data, domain, dAppName)
                        return WC2SignMessageRequest(
                            request.requestId,
                            request.topic,
                            dataString,
                            message
                        )
                    }
                }

            }
        }
        return null
    }

    private fun hexStringToUtf8String(hexString: String) = try {
        String(hexString.hexStringToByteArray())
    } catch (_: Throwable) {
        hexString
    }

    fun getAccountData(string: String): WCAccountData? {
        val chunks = string.split(":")
        if (chunks.size < 2) {
            return null
        }

        val chainId = chunks[1].toIntOrNull() ?: return null
        val chain = WCChain.values().firstOrNull { it.id == chainId }
        val address: String? = when {
            chunks.size >= 3 -> chunks[2]
            else -> null
        }

        return chain?.let {
            WCAccountData(eip = chunks[0], chain = chain, address = address)
        }
    }

}
