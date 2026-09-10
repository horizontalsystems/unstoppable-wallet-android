package io.horizontalsystems.walletkit.modules.walletconnect.request

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import org.json.JSONArray

/** Pure extraction of the payload from a WalletConnect EVM request's `params` array. */
object WCEvmRequestParams {

    /**
     * The JSON object of a typed-data (`eth_signTypedData*`) or transaction request, as a string
     * for display and signing. EIP-712 dApps send the typed data either as a JSON object or, per
     * the original MetaMask convention, as a JSON-encoded STRING next to the signer address —
     * both forms are accepted so the request renders instead of failing on the second one.
     */
    fun jsonObjectParam(paramsJson: String): String {
        val params = JsonParser.parseString(paramsJson).asJsonArray

        params.firstOrNull { it.isJsonObject }?.let { return it.toString() }

        params.asSequence()
            .filter { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            .mapNotNull { it.asString.parseJsonOrNull() }
            .firstOrNull { it.isJsonObject }
            ?.let { return it.toString() }

        throw Exception("Invalid Data")
    }

    /** The `personal_sign` message: hex-encoded bytes decoded to text, otherwise the raw string. */
    fun personalSignMessage(paramsJson: String): String {
        val jsonArray = JSONArray(paramsJson)
        require(jsonArray.length() > 0)
        val message = jsonArray.getString(0)
        return try {
            String(message.hexStringToByteArray())
        } catch (_: Throwable) {
            message
        }
    }

    private fun String.parseJsonOrNull(): JsonElement? = try {
        JsonParser.parseString(this)
    } catch (e: Exception) {
        null
    }
}
