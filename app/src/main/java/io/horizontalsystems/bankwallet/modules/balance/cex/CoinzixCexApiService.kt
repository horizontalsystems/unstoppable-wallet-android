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

class CoinzixCexApiService {
    private val service = APIClient.retrofit("https://api.coinzix.com/", 60, true).create(CoinzixAPI::class.java)
    private val gson = Gson()

    suspend fun login(username: String, password: String, captchaToken: String): Response.Login {
        val params = mapOf(
            "username" to username,
            "password" to password,
            "g-recaptcha-response" to captchaToken,
        )

        return service.login(createJsonRequestBody(params))
    }

    suspend fun getBalances(
        authToken: String,
        secret: String,
    ): List<Response.Balance> {
        val balances = post<Response.Balances>(
            path = "v1/private/balances",
            authToken = authToken,
            secret = secret
        )
        return balances.data.list
    }

    suspend fun getAddress(
        authToken: String,
        secret: String,
        iso: String,
        new: Int,
        network: String?
    ): Response.AddressData {
        val params = buildMap {
            put("iso", iso)
            put("new", new.toString())
            network?.let {
                put("network", it)
            }
        }
        val address = post<Response.Address>(
            path = "v1/private/get-address",
            authToken = authToken,
            secret = secret,
            params = params
        )
        return address.data
    }

    suspend fun withdraw(
        authToken: String,
        secret: String,
        iso: String,
        network: String?,
        address: String,
        amount: BigDecimal
    ): String {
        val params = buildMap {
            put("iso", iso)
            put("to_address", address)
            put("amount", amount.toPlainString())
            network?.let {
                put("network", it)
            }
        }
        val response = post<Response.Withdraw>(
            path = "/v1/withdraw",
            authToken = authToken,
            secret = secret,
            params = params
        )

        check(response.status) { response.message }

        return response.data.id.toString()
    }

    suspend fun confirmWithdraw(
        authToken: String,
        secret: String,
        withdrawId: String,
        emailCode: String,
        twoFactorCode: String?
    ) {
        val params = buildMap {
            put("id", withdrawId)
            put("email_pin", emailCode)
            twoFactorCode?.let {
                put("google_pin", it)
            }
        }
        val response = post<Response.Withdraw>(
            path = "/v1/withdraw/confirm-code",
            authToken = authToken,
            secret = secret,
            params = params
        )

        check(response.status) { response.message }
    }

    suspend fun sendWithdrawPin(
        authToken: String,
        secret: String,
        withdrawId: String
    ) {
        val params = mapOf("id" to withdrawId)

        val response = post<Response.Withdraw>(
            path = "/v1/withdraw/send-pin",
            authToken = authToken,
            secret = secret,
            params = params
        )

        check(response.status) { response.message }
    }

    private suspend inline fun <reified T> post(
        path: String,
        authToken: String,
        secret: String,
        params: Map<String, String> = mapOf()
    ): T {
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
        @POST("api/user/login")
        suspend fun login(@Body params: RequestBody): Response.Login

        @POST
        suspend fun post(@Url path: String, @HeaderMap headers: Map<String, String>, @Body params: RequestBody): String
    }
}

object Response {
    data class Address(
        val data: AddressData,
        val token: String,
    )

    data class AddressData(
        val address: String?,
        val account: String?,
        val memo: String?,
    )

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

    data class Withdraw(
        val status: Boolean,
        val message: String,
        val data: Data
    ) {
        data class Data(val id: Int)
    }
}
