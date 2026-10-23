package io.horizontalsystems.walletkit.chain.zcash

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Resolves Zcash Names (ZNS) to addresses through the hosted Resolver.
 *
 * The Resolver speaks JSON-RPC 2.0. Under the Name Notes protocol a registration is a
 * note on chain, readable with the registry's published viewing key, and the Resolver is
 * a cache of that registry. Every lookup also asks the Resolver for its viewing key and
 * requires the pinned mainnet key. This catches a wrong endpoint, a testnet Resolver or a
 * rotated registry; it does not protect against a dishonest Resolver, since the key is
 * public. Records are not verified on chain, so the address is trusted on TLS and the
 * Resolver operator.
 *
 * See https://www.zcashnames.com/docs/resolver/verifying-state
 */
open class ZnsResolver(
    private val url: String = MAINNET_URL,
    private val viewingKey: String = MAINNET_VIEWING_KEY,
) {

    class RpcError(message: String) : Exception(message)
    class UnknownRegistry : Exception("ZNS Resolver serves a different registry")

    data class Registration(
        val name: String,
        val address: String,
    )

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Returns the address bound to [name], or null when the name is not registered.
     *
     * @param name a normalized name (see [ZnsName.normalize]); the Resolver does not normalize
     * @throws IOException when the Resolver is unreachable or answers with a non-2xx status
     * @throws RpcError when the Resolver returns a JSON-RPC error, a malformed body, or a
     * registration for another name
     * @throws UnknownRegistry when the Resolver's viewing key is not the pinned one
     */
    open fun resolve(name: String): String? = runBlocking(Dispatchers.IO) {
        val status = async { call("status", null) }
        val registration = async {
            call("resolve", JsonObject().apply { addProperty("query", name) })
        }

        verifyRegistry(status.await(), viewingKey)
        parseResolveResponse(registration.await())?.let { validate(name, it) }
    }

    private fun call(method: String, params: JsonObject?): String {
        val requestBody = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("id", 1)
            addProperty("method", method)
            params?.let { add("params", it) }
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                throw IOException("Unexpected response: ${response.code}")
            }

            return responseBody
        }
    }

    companion object {
        const val MAINNET_URL = "https://main.zcashnames.com"

        /** Unified full viewing key of the mainnet registry, as published in the ZNS docs. */
        const val MAINNET_VIEWING_KEY =
            "uview1209ld9y4fr2sel9q87kfjg7xm2sy53e3qf7dvcw3w8jwhhjpldl8z0vk0a6hveg4jk7xcn0rc9mr8yg77djfpc0uxrm5pqzt8q7ul4w0nvs3x5q9vmk9nm2crywtqaealek4mp47ur7ywu7g4g4xcdlwv6gnqzvu5panv5je2ps6rxmx4856unlt5wde8fpz8uhrqchmwhn2j252h3cyx5phad877xezerm6ex8l5kcfjw02ll7m0nspfufamh3ae9ea48395elptspnv6vvc70j2gcjrdze9syt8kpr2kc67flvg8687thdkpha7lpt9r4p9wjty90q2v2yjanqsr2zrecrqs7lfx9cpkmshf7jyh2vvwfqnyzwtpuy7p72hm2mjhperdafvm9u5rwa6s6urcy0uw30f687e3jnmas45qfqtwyz39kf6kxklfrdn7t4hkz56jegmlr8s8hwmt9wa8vz8zlmm76lrmzyj82nzpeqfu572njq"

        /**
         * Checks the body of a `status` call against the pinned [viewingKey].
         *
         * @throws RpcError on a JSON-RPC error object or a malformed body
         * @throws UnknownRegistry when the reported key is not exactly [viewingKey]
         */
        fun verifyRegistry(body: String, viewingKey: String) {
            val result = parseResult(body)
            if (result == null || !result.isJsonObject) throw RpcError("Unexpected status shape")

            val reported = result.asJsonObject.get("viewing_key")
                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                ?.asString
                ?: throw RpcError("Status is missing the viewing key")

            if (reported != viewingKey) throw UnknownRegistry()
        }

        /**
         * Parses the body of a `resolve` call made with a name query.
         *
         * Only `name` and `address` are read. `expires_at` is ignored on purpose: a name past
         * its deadline still resolves until the Mint's release note lands on chain, after
         * which the Resolver returns null, and the device clock is not chain time.
         *
         * @return the registration, or null when `result` is null (name not registered)
         * @throws RpcError on a JSON-RPC error object, a malformed body, or a result that is not
         * a registration object (an address query returns an array; we never send one)
         */
        fun parseResolveResponse(body: String): Registration? {
            val result = parseResult(body) ?: return null
            if (!result.isJsonObject) throw RpcError("Unexpected result shape")

            val obj = result.asJsonObject
            return Registration(
                name = obj.string("name") ?: throw RpcError("Registration is missing the name"),
                address = obj.string("address") ?: throw RpcError("Registration is missing the address"),
            )
        }

        /**
         * Returns the address of [registration] when it answers the query for [name].
         *
         * @throws RpcError when the registration is for a different name
         */
        fun validate(name: String, registration: Registration): String {
            if (registration.name != name) {
                throw RpcError("Registration name \"${registration.name}\" does not match query \"$name\"")
            }
            return registration.address
        }

        /** Returns `result` of a JSON-RPC body, or null when it is absent or null. */
        private fun parseResult(body: String): JsonElement? {
            val json = try {
                JsonParser.parseString(body).asJsonObject
            } catch (e: Exception) {
                throw RpcError("Malformed response")
            }

            json.get("error")?.takeIf { it.isJsonObject }?.asJsonObject?.let { error ->
                val code = error.get("code")?.asString
                val message = error.get("message")?.asString
                throw RpcError("JSON-RPC error $code: $message")
            }

            return json.get("result")?.takeIf { !it.isJsonNull }
        }

        private fun JsonObject.string(key: String): String? =
            get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
    }
}
