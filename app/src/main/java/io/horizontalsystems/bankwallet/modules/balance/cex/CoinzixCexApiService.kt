package io.horizontalsystems.bankwallet.modules.balance.cex

import com.google.gson.Gson
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bitcoincore.utils.HashUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url
import java.math.BigDecimal

class CoinzixCexApiService(
    private val authToken: String,
    private val secret: String
) {
    private val service = APIClient.retrofit("https://api.coinzix.com/", 60, true).create(CoinzixAPI::class.java)
    private val gson = Gson()

    suspend fun getBalances(): List<Response.Balance> {
        val balances = post<Response.Balances>("v1/private/balances")
        return balances.data.list.filter { it.balance_available > BigDecimal.ZERO }
    }

    private suspend inline fun <reified T> post(path: String, params: Map<String, String> = mapOf()): T {
        val parameters = params + mapOf(
            "request_id" to System.currentTimeMillis().toString()
        )

        val headers = mapOf(
            "login-token" to authToken,
            "x-auth-sign" to xAuthSign(parameters, secret)
        )

        val responseString = service.post(path, headers, createJsonRequestBody(parameters))
        return gson.fromJson(responseString, T::class.java)
    }

    private fun xAuthSign(parameters: Map<String, String>, secret: String): String {
        val parametersSignature =
            parameters.keys.sorted().mapNotNull { parameters[it] }.joinToString("")
        val signature = parametersSignature + secret
        return HashUtils.sha256(signature.toByteArray()).toRawHexString()
    }

    private fun createJsonRequestBody(parameters: Map<String, String>): RequestBody {
        return JSONObject(parameters).toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
    }

    interface CoinzixAPI {
        @POST
        suspend fun post(@Url path: String, @HeaderMap headers: Map<String, String>, @Body params: RequestBody): String
    }
}

object Response {
    data class Login(
        val data: LoginData,
        val token: String,
    )
    data class LoginData(
        val email: String,
        val chat: String,
        val secret: String,
    )
    data class Balances(
        val data: BalanceList
    )

    data class BalanceList(
        val list: List<Balance>
    )

    data class Balance(
        val balance: BigDecimal,
        val balance_available: BigDecimal,
        val currency: Currency
    )

    data class Currency(
        val iso3: String,
        val name: String,
        val refill: Int,
        val withdraw: Int,
        val networks: Map<Int, String>,
    )
}
