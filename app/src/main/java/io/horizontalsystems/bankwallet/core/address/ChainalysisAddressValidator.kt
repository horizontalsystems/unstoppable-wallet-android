package io.horizontalsystems.bankwallet.core.address

import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.entities.Address
import retrofit2.http.GET
import retrofit2.http.Path

class ChainalysisAddressValidator(
    baseUrl: String,
    apiKey: String
) {

    private val apiService by lazy {
        APIClient.build(
            baseUrl,
            mapOf("Accept" to "application/json", "X-API-KEY" to apiKey)
        ).create(ChainalysisApi::class.java)
    }

    suspend fun isClear(address: Address): Boolean {
        val response = apiService.address(address.hex)
        return response.identifications.isEmpty()
    }

    data class ChainalysisApiResponse(
        val identifications: List<Identification>
    )

    data class Identification(
        val category: String,
        val name: String?,
        val description: String?,
        val url: String?
    )

    private interface ChainalysisApi {
        @GET("address/{address}")
        suspend fun address(@Path("address") address: String): ChainalysisApiResponse
    }

}
