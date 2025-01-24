package io.horizontalsystems.bankwallet.core.address

import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.entities.Address
import retrofit2.http.GET
import retrofit2.http.Path

class ChainalysisAddressValidator(
    baseUrl: String,
    apiKey: String
) : AddressSecurityCheckerChain.IAddressSecurityCheckerItem {

    private val apiService by lazy {
        APIClient.build(
            baseUrl,
            mapOf("Accept" to "application/json", "X-API-KEY" to apiKey)
        ).create(ChainalysisApi::class.java)
    }

    override suspend fun handle(address: Address): AddressSecurityCheckerChain.SecurityIssue? {
        val response = apiService.address(address.hex)
        return if (response.identifications.isNotEmpty())
            AddressSecurityCheckerChain.SecurityIssue.Sanctioned("Sanctioned address. ${response.identifications.size} identifications found.")
        else
            null
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
        suspend fun address(@Path("address")  address: String): ChainalysisApiResponse
    }

}
