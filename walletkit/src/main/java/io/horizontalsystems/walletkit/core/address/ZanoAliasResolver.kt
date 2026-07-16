package io.horizontalsystems.walletkit.core.address

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.horizontalsystems.walletkit.core.managers.ZanoNodeManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ZanoAliasResolver(private val zanoNodeManager: ZanoNodeManager) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun resolve(alias: String): String? {
        val baseUrl = zanoNodeManager.currentNode.host.trimEnd('/')

        val requestBody = JsonObject().apply {
            addProperty("id", 0)
            addProperty("jsonrpc", "2.0")
            addProperty("method", "get_alias_details")
            add("params", JsonObject().apply {
                addProperty("alias", alias)
            })
        }

        val request = Request.Builder()
            .url("$baseUrl/json_rpc")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                throw IOException("Unexpected response: ${response.code}")
            }

            return parseAliasAddress(responseBody)
        }
    }

    companion object {
        fun parseAliasAddress(body: String): String? = try {
            val json = JsonParser.parseString(body).asJsonObject
            val result = if (json.has("error")) null else json.getAsJsonObject("result")

            if (result?.get("status")?.asString == "OK") {
                result.getAsJsonObject("alias_details")?.get("address")?.asString?.takeIf { it.isNotEmpty() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
