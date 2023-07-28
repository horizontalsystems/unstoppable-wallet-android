package io.horizontalsystems.bankwallet.modules.balance.cex

import com.google.gson.Gson
import com.google.gson.JsonElement
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bitcoincore.utils.HashUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url
import java.math.BigDecimal

class CoinzixCexApiService {
    private val service = APIClient.retrofit("https://api.coinzix.com/", 60, true).create(CoinzixAPI::class.java)
    private val gson = Gson()

    suspend fun login(username: String, password: String): Response.Login {
        val params = mapOf(
            "username" to username,
            "password" to password
        )

        try {
            return service.login(createJsonRequestBody(params))
        } catch (exception: HttpException) {
            val message = parseHttpExceptionError(exception) ?: exception.message ?: exception.javaClass.simpleName
            throw IllegalStateException(message)
        }
    }

    suspend fun resendLoginPin(token: String): Response.Login {
        val params = mapOf(
            "login_token" to token
        )

        try {
            return service.resendLoginPin(createJsonRequestBody(params))
        } catch (exception: HttpException) {
            val message = parseHttpExceptionError(exception) ?: exception.message ?: exception.javaClass.simpleName
            throw IllegalStateException(message)
        }
    }

    suspend fun validateCode(token: String, code: String): Response.Login {
        val params = mapOf(
            "login_token" to token,
            "code" to code
        )

        try {
            return service.validateCode(createJsonRequestBody(params))
        } catch (exception: HttpException) {
            val message = parseHttpExceptionError(exception) ?: exception.message ?: exception.javaClass.simpleName
            throw IllegalStateException(message)
        }
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

    suspend fun getConfig(): Response.Config {
        return service.config()
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
        amount: BigDecimal,
        feeFromAmount: Boolean
    ): Response.Withdraw {
        val params = buildMap {
            put("iso", iso)
            put("to_address", address)
            put("amount", amount.toPlainString())
            put("fee_from_amount", if (feeFromAmount) "1" else "0")
            network?.let {
                put("network", it)
            }
        }
        return post(
            path = "/v1/withdraw",
            authToken = authToken,
            secret = secret,
            params = params
        )
    }

    suspend fun confirmWithdraw(
        authToken: String,
        secret: String,
        withdrawId: String,
        emailCode: String?,
        twoFactorCode: String?
    ): Response.Withdraw {
        val params = buildMap {
            put("id", withdrawId)
            emailCode?.let {
                put("email_pin", emailCode)
            }
            twoFactorCode?.let {
                put("google_pin", twoFactorCode)
            }
        }
        return post(
            path = "/v1/withdraw/confirm-code",
            authToken = authToken,
            secret = secret,
            params = params
        )
    }

    suspend fun sendWithdrawPin(
        authToken: String,
        secret: String,
        withdrawId: String
    ): Response.Withdraw {
        val params = mapOf("id" to withdrawId)

        return post(
            path = "/v1/withdraw/send-pin",
            authToken = authToken,
            secret = secret,
            params = params
        )
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

        try {
            val responseString = service.post(path, headers, createJsonRequestBody(parameters))
            return gson.fromJson(responseString, T::class.java)
        } catch (exception: HttpException) {
            val message = parseHttpExceptionError(exception) ?: exception.message ?: exception.javaClass.simpleName
            throw IllegalStateException(message)
        }
    }

    private fun parseHttpExceptionError(exception: HttpException) =
        exception.response()?.errorBody()?.string()?.let {
            try {
                val jsonObject = gson.fromJson(it, Map::class.java)
                jsonObject["error"]?.toString() ?: jsonObject["message"]?.toString()
            } catch (exception: Exception) {
                null
            }
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
        @POST("api/user/init-app")
        suspend fun login(@Body params: RequestBody): Response.Login

        @POST("api/user/send-two-fa")
        suspend fun resendLoginPin(@Body params: RequestBody): Response.Login

        @POST("api/user/validate-code")
        suspend fun validateCode(@Body params: RequestBody): Response.Login

        @POST
        suspend fun post(@Url path: String, @HeaderMap headers: Map<String, String>, @Body params: RequestBody): String

        @GET("api/default/config")
        suspend fun config(): Response.Config
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
        val status: Boolean,
        val data: JsonElement?,
        val token: String?,
        val is_login: Boolean,
        val required_code: Boolean,
        val errors: List<String>?
    ) {
        fun loginData(): LoginData? = try {
            Gson().fromJson(data, LoginData::class.java)
        } catch (e: Throwable) {
            null
        }
    }

    data class LoginData(
        val required: Int?,
        val email: String?,
        val secret: String?,
        val left_attempt: Int?,
        val time_expire: Int?
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
        val message: String?,
        val data: Data?,
        val errors: List<String>?
    ) {
        data class Data(val id: String, val step: List<Int>)
    }

    data class Config(
        val status: Boolean,
        val data: Data
    ) {
        fun withdrawNetworks(id: String): List<WithdrawNetwork> =
            data.commission[id]?.let { network ->
                listOf(network) + network.networks
            } ?: listOf()

        fun depositNetworks(id: String): List<DepositNetwork> =
            data.commission_refill[id]?.let { network ->
                listOf(network) + network.networks
            } ?: listOf()

        data class Data(
            val currency_withdraw: List<String>,
            val currency_deposit: List<String>,
            val commission: Map<String, WithdrawNetwork>,
            val commission_refill: Map<String, DepositNetwork>,
            val demo_currency: Map<String, String>,
            val fiat_currencies: List<String>
        )

        data class WithdrawNetwork(
            val fixed: BigDecimal,
            val percent: BigDecimal,
            val min_commission: BigDecimal,
            val max_withdraw: BigDecimal,
            val min_withdraw: BigDecimal,
            val network_type: Int,
            val networks: List<WithdrawNetwork>
        )

        data class DepositNetwork(
            val fixed: BigDecimal,
            val min_commission: BigDecimal,
            val min_refill: BigDecimal,
            val network_type: Int,
            val networks: List<DepositNetwork>
        )
    }

}
