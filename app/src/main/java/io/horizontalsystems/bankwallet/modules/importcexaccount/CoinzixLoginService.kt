package io.horizontalsystems.bankwallet.modules.importcexaccount

import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.modules.balance.cex.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.http.Body
import retrofit2.http.POST

class CoinzixLoginService {

    private val loginApi = APIClient
        .retrofit("https://api.coinzix.com/", 60, true)
        .create(CoinzixPublicAPI::class.java)

    suspend fun login(username: String, password: String, captchaToken: String): Response.Login {
        val params = mapOf(
            "username" to username,
            "password" to password,
            "g-recaptcha-response" to captchaToken,
        )

        return loginApi.login(createJsonRequestBody(params))
    }

    private fun createJsonRequestBody(parameters: Map<String, String>): RequestBody {
        return JSONObject(parameters).toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
    }

}

interface CoinzixPublicAPI {
    @POST("api/user/login")
    suspend fun login(
        @Body params: RequestBody
    ): Response.Login
}
