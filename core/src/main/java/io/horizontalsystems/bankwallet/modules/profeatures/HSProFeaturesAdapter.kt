package io.horizontalsystems.bankwallet.modules.profeatures

import io.horizontalsystems.marketkit.providers.RetrofitUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

class HSProFeaturesAdapter(baseUrl: String, apiKey: String) {

    private val service by lazy {
        RetrofitUtils.build("${baseUrl}/v1/", mapOf("apikey" to apiKey)).create(HsService::class.java)
    }

    suspend fun getMessage(address: String): String = withContext(Dispatchers.IO) {
        service.getKey(address).key
    }

    suspend fun authenticate(address: String, signature: String): String = withContext(Dispatchers.IO) {
        service.authenticate(SignatureData(address, signature)).token
    }

    private interface HsService {
        @GET("auth/get-key")
        suspend fun getKey(
            @Query("address") address: String
        ): AuthKeyResponse

        @POST("auth/authenticate")
        suspend fun authenticate(
            @Body signatureData: SignatureData
        ): AuthenticationResponse
    }

    data class SignatureData(val address: String, val signature: String)

    data class AuthKeyResponse(val key: String)
    data class AuthenticationResponse(val token: String)

}
